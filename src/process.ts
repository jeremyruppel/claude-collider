import { spawn, ChildProcess } from "child_process"
import { EventEmitter } from "events"
import { debug } from "./debug.js"
import { SclangConfig } from "./config.js"
import { OutputParser } from "./parser.js"
import { compress } from "./tokenizer.js"

export class SclangProcess extends EventEmitter {
  private readonly config: SclangConfig
  private readonly parser: OutputParser
  private process: ChildProcess | null = null

  constructor(config: SclangConfig) {
    super()
    this.config = config
    this.parser = new OutputParser()
  }

  get isAlive(): boolean {
    return this.process !== null
  }

  spawn(): void {
    debug(`Spawning sclang: ${this.config.path}`)

    this.process = spawn(this.config.path, [], {
      stdio: ["pipe", "pipe", "pipe"],
    })

    debug(`Process spawned, pid: ${this.process.pid}`)
    this.parser.clear()
    this.setupHandlers()
  }

  send(code: string): void {
    if (!this.process?.stdin) {
      debug("ERROR: No stdin available")
      return
    }
    const compressed = compress(code)
    debug(`Sending: ${compressed}`)
    this.process.stdin.write(compressed + "\n")
  }

  sendWrapped(code: string): void {
    this.parser.clear()
    this.send(OutputParser.wrapCode(code))
  }

  async kill(): Promise<void> {
    if (!this.process) return

    const proc = this.process
    this.process = null

    debug("Sending quit commands to sclang")
    proc.stdin?.write("Server.quitAll; 0.exit;\n")

    await new Promise<void>((resolve) => {
      const forceKillTimeout = setTimeout(() => {
        debug("Force killing sclang after timeout")
        proc.kill("SIGKILL")
      }, 2000)

      proc.once("exit", () => {
        clearTimeout(forceKillTimeout)
        resolve()
      })
    })
  }

  private setupHandlers(): void {
    if (!this.process) return

    this.process.stdout?.on("data", (data: Buffer) => {
      const str = data.toString()
      debug(`[stdout] ${str}`)
      this.parser.append(str)
      this.emit("stdout", str)
      this.checkOutput()
    })

    this.process.stderr?.on("data", (data: Buffer) => {
      const str = data.toString()
      debug(`[stderr] ${str}`)
      this.parser.append(str)
      this.emit("stderr", str)
      this.checkOutput()
    })

    this.process.on("error", (err) => {
      debug(`Process error: ${err.message}`)
      this.emit("error", err)
    })

    this.process.on("exit", (code) => {
      debug(`Process exited with code: ${code}`)
      this.process = null
      this.emit("exit", code)
    })
  }

  private checkOutput(): void {
    if (this.parser.hasSclangReady()) {
      this.emit("sclang-ready")
    }

    if (this.parser.hasServerReady()) {
      this.emit("server-ready")
    }

    if (this.parser.hasCCReady()) {
      this.emit("cc-ready")
    }

    const result = this.parser.extractResult()
    if (result !== null) {
      const error = this.parser.formatError()
      if (error) {
        this.emit("exec-error", new Error(error))
      } else {
        this.emit("exec-result", result)
      }
    }

    if (this.parser.hasError()) {
      this.emit("error-output", this.parser.formatError())
    }
  }
}
