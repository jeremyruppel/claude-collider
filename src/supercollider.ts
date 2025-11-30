import { spawn, ChildProcess } from "child_process"
import { existsSync, appendFileSync } from "fs"
import { EventEmitter } from "events"

const MACOS_SCLANG_PATH =
  "/Applications/SuperCollider.app/Contents/MacOS/sclang"

const LOG_FILE = "/tmp/claude-collider.log"

function debug(message: string): void {
  const line = `[${new Date().toISOString()}] ${message}\n`
  appendFileSync(LOG_FILE, line)
}

enum ServerState {
  Stopped = "stopped",
  Booting = "booting",
  Running = "running",
}

class SclangConfig {
  readonly path: string
  readonly bootTimeout: number
  readonly execTimeout: number

  constructor() {
    this.path = this.findSclangPath()
    this.bootTimeout = parseInt(process.env.SC_BOOT_TIMEOUT || "10000", 10)
    this.execTimeout = parseInt(process.env.SC_EXEC_TIMEOUT || "2000", 10)
  }

  private findSclangPath(): string {
    if (process.env.SCLANG_PATH) {
      return process.env.SCLANG_PATH
    }
    if (process.platform === "darwin" && existsSync(MACOS_SCLANG_PATH)) {
      return MACOS_SCLANG_PATH
    }
    return "sclang"
  }
}

class OutputParser {
  private static readonly BEGIN_MARKER = ">>>BEGIN>>>"
  private static readonly END_MARKER = "<<<END<<<"
  private static readonly SERVER_READY = "SERVER_READY"
  private static readonly SCLANG_READY = "Welcome to SuperCollider"

  private buffer = ""

  append(data: string): void {
    this.buffer += data
  }

  clear(): void {
    this.buffer = ""
  }

  hasSclangReady(): boolean {
    return this.buffer.includes(OutputParser.SCLANG_READY)
  }

  hasServerReady(): boolean {
    return this.buffer.includes(OutputParser.SERVER_READY)
  }

  extractResult(): string | null {
    const beginIndex = this.buffer.indexOf(OutputParser.BEGIN_MARKER)
    const endIndex = this.buffer.indexOf(OutputParser.END_MARKER)

    if (beginIndex !== -1 && endIndex !== -1 && endIndex > beginIndex) {
      const startPos = beginIndex + OutputParser.BEGIN_MARKER.length
      return this.buffer.slice(startPos, endIndex).trim() || "OK"
    }
    return null
  }

  static wrapCode(code: string): string {
    return `"${OutputParser.BEGIN_MARKER}".postln; (\n${code}\n).value; "${OutputParser.END_MARKER}".postln;`
  }

  static bootCommand(): string {
    return `s.waitForBoot { "${OutputParser.SERVER_READY}".postln };`
  }
}

interface PendingOperation {
  resolve: (value: string) => void
  reject: (error: Error) => void
  timeout: ReturnType<typeof setTimeout>
}

export class SuperCollider extends EventEmitter {
  private readonly config: SclangConfig
  private readonly parser: OutputParser
  private process: ChildProcess | null = null
  private state: ServerState = ServerState.Stopped
  private pendingBoot: PendingOperation | null = null
  private pendingExec: PendingOperation | null = null
  private bootCommandSent = false

  constructor() {
    super()
    this.config = new SclangConfig()
    this.parser = new OutputParser()
  }

  async boot(): Promise<string> {
    debug("boot() called")

    if (this.state === ServerState.Running) {
      return "SuperCollider is already running"
    }

    if (this.state === ServerState.Booting) {
      return "SuperCollider is already booting"
    }

    this.state = ServerState.Booting
    debug("State set to Booting")

    return new Promise<string>((resolve, reject) => {
      const timeout = setTimeout(() => {
        this.pendingBoot = null
        this.state = ServerState.Stopped
        reject(new Error("SuperCollider boot timed out"))
        this.quit()
      }, this.config.bootTimeout)

      this.pendingBoot = { resolve, reject, timeout }
      this.startProcess()
    })
  }

  async execute(code: string): Promise<string> {
    if (this.state !== ServerState.Running) {
      throw new Error(
        `SuperCollider is not running (state: ${this.state}). Call boot() first.`
      )
    }

    if (this.pendingExec) {
      throw new Error("Another execution is already in progress")
    }

    this.parser.clear()

    return new Promise<string>((resolve, reject) => {
      const timeout = setTimeout(() => {
        this.pendingExec = null
        reject(new Error("Execution timed out"))
      }, this.config.execTimeout)

      this.pendingExec = { resolve, reject, timeout }
      this.sendCode(OutputParser.wrapCode(code))
    })
  }

  async stop(): Promise<void> {
    if (this.process) {
      this.sendCode("CmdPeriod.run;")
    }
  }

  async quit(): Promise<void> {
    if (this.process) {
      this.sendCode("0.exit;")
      this.process.kill()
      this.process = null
    }
    this.state = ServerState.Stopped
    this.bootCommandSent = false
    this.clearPending()
  }

  isRunning(): boolean {
    return this.state === ServerState.Running
  }

  getState(): ServerState {
    return this.state
  }

  private startProcess(): void {
    debug(`startProcess() called`)
    debug(`sclang path: ${this.config.path}`)

    this.process = spawn(this.config.path, [], {
      stdio: ["pipe", "pipe", "pipe"],
    })

    debug(`Process spawned, pid: ${this.process.pid}`)

    this.parser.clear()
    this.bootCommandSent = false
    this.setupProcessHandlers()
    // Boot command will be sent when sclang is ready (see checkForSclangReady)
  }

  private setupProcessHandlers(): void {
    if (!this.process) {
      return
    }

    this.process.stdout?.on("data", (data: Buffer) => {
      this.handleStdout(data.toString())
    })

    this.process.stderr?.on("data", (data: Buffer) => {
      this.handleStderr(data.toString())
    })

    this.process.on("error", (err) => {
      this.handleProcessError(err)
    })

    this.process.on("exit", (code) => {
      this.handleExit(code)
    })
  }

  private handleStdout(data: string): void {
    debug(`[stdout] ${data}`)
    this.parser.append(data)
    this.emit("stdout", data)
    this.checkForSclangReady()
    this.checkForServerReady()
    this.checkForExecResult()
  }

  private checkForSclangReady(): void {
    if (
      this.state === ServerState.Booting &&
      !this.bootCommandSent &&
      this.parser.hasSclangReady()
    ) {
      debug("sclang ready, sending boot command")
      this.bootCommandSent = true
      this.sendCode(OutputParser.bootCommand())
    }
  }

  private handleStderr(data: string): void {
    debug(`[stderr] ${data}`)
    this.parser.append(data)
    this.emit("stderr", data)
    this.checkForServerReady()

    if (data.includes("ERROR")) {
      this.emit("error", new Error(data))
    }
  }

  private checkForServerReady(): void {
    if (this.state === ServerState.Booting && this.pendingBoot) {
      if (this.parser.hasServerReady()) {
        debug("SERVER_READY detected, transitioning to Running state")
        this.state = ServerState.Running
        clearTimeout(this.pendingBoot.timeout)
        const { resolve } = this.pendingBoot
        this.pendingBoot = null
        resolve("SuperCollider server booted successfully")
      }
    }
  }

  private checkForExecResult(): void {
    if (this.state === ServerState.Running && this.pendingExec) {
      const result = this.parser.extractResult()
      if (result !== null) {
        debug(`Execution result: ${result}`)
        clearTimeout(this.pendingExec.timeout)
        const { resolve } = this.pendingExec
        this.pendingExec = null
        resolve(result)
      }
    }
  }

  private handleProcessError(err: Error): void {
    debug(`Process error: ${err.message}`)
    const error = new Error(`Failed to start sclang: ${err.message}`)

    if (this.pendingBoot) {
      clearTimeout(this.pendingBoot.timeout)
      this.pendingBoot.reject(error)
      this.pendingBoot = null
    }

    this.state = ServerState.Stopped
    this.emit("error", error)
  }

  private handleExit(code: number | null): void {
    debug(`Process exited with code: ${code}`)
    const wasRunning = this.state === ServerState.Running
    this.state = ServerState.Stopped
    this.process = null

    const error = new Error(`sclang exited with code ${code}`)

    if (this.pendingBoot) {
      clearTimeout(this.pendingBoot.timeout)
      this.pendingBoot.reject(error)
      this.pendingBoot = null
    }

    if (this.pendingExec) {
      clearTimeout(this.pendingExec.timeout)
      this.pendingExec.reject(error)
      this.pendingExec = null
    }

    this.emit("exit", code)

    if (wasRunning) {
      this.emit("crash", code)
    }
  }

  private clearPending(): void {
    if (this.pendingBoot) {
      clearTimeout(this.pendingBoot.timeout)
      this.pendingBoot = null
    }
    if (this.pendingExec) {
      clearTimeout(this.pendingExec.timeout)
      this.pendingExec = null
    }
  }

  private sendCode(code: string): void {
    debug(`sendCode called with: ${code}`)
    if (!this.process?.stdin) {
      debug("ERROR: No stdin available")
      return
    }
    const result = this.process.stdin.write(code + "\n")
    debug(`stdin.write returned: ${result}`)
  }
}
