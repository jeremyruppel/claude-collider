import { EventEmitter } from "events"
import { debug } from "./debug.js"
import { SclangConfig } from "./config.js"
import { OutputParser } from "./parser.js"
import { SclangProcess } from "./process.js"
import { ServerState } from "./types.js"

export class SuperCollider extends EventEmitter {
  private readonly config: SclangConfig
  private _process: SclangProcess | null = null
  private state: ServerState = ServerState.Stopped
  private bootCommandSent = false

  constructor() {
    super()
    this.config = new SclangConfig()
  }

  private get process(): SclangProcess {
    if (!this._process) {
      throw new Error("SuperCollider process is not running")
    }
    return this._process
  }

  private set process(value: SclangProcess | null) {
    this._process = value
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
    this.bootCommandSent = false
    debug("State set to Booting")

    return new Promise<string>((resolve, reject) => {
      const timeout = setTimeout(() => {
        this.state = ServerState.Stopped
        reject(new Error("SuperCollider boot timed out"))
        this.quit()
      }, this.config.bootTimeout)

      this.process = new SclangProcess(this.config)

      this.process.once("sclang-ready", () => {
        if (!this.bootCommandSent) {
          debug("sclang ready, sending boot command")
          this.bootCommandSent = true
          this.process?.send(OutputParser.bootCommand())
        }
      })

      this.process.once("server-ready", () => {
        debug("Server ready")
        clearTimeout(timeout)
        this.state = ServerState.Running
        resolve("SuperCollider server booted successfully")
      })

      this.process.once("error", (err) => {
        clearTimeout(timeout)
        this.state = ServerState.Stopped
        reject(new Error(`Failed to start sclang: ${err.message}`))
      })

      this.process.on("exit", (code) => {
        const wasRunning = this.state === ServerState.Running
        this.state = ServerState.Stopped
        this.process = null
        this.emit("exit", code)
        if (wasRunning) {
          this.emit("crash", code)
        }
      })

      this.process.on("stdout", (data) => this.emit("stdout", data))
      this.process.on("stderr", (data) => this.emit("stderr", data))

      this.process.spawn()
    })
  }

  async execute(code: string): Promise<string> {
    if (this.state !== ServerState.Running) {
      throw new Error(
        `SuperCollider is not running (state: ${this.state}). Call boot() first.`
      )
    }

    return new Promise<string>((resolve, reject) => {
      const timeout = setTimeout(() => {
        reject(new Error("Execution timed out"))
      }, this.config.execTimeout)

      const cleanup = () => {
        clearTimeout(timeout)
        this.process?.removeListener("exec-result", onResult)
        this.process?.removeListener("exec-error", onError)
      }

      const onResult = (result: string) => {
        cleanup()
        resolve(result)
      }

      const onError = (error: Error) => {
        cleanup()
        reject(error)
      }

      this.process.once("exec-result", onResult)
      this.process.once("exec-error", onError)
      this.process.sendWrapped(code)
    })
  }

  async stop(): Promise<void> {
    this._process?.send("CmdPeriod.run;")
  }

  async freeAll(): Promise<void> {
    this._process?.send("s.freeAll;")
  }

  async quit(): Promise<void> {
    if (this._process) {
      await this._process.kill()
      this.process = null
    }
    this.state = ServerState.Stopped
    this.bootCommandSent = false
  }

  async restart(): Promise<string> {
    debug("restart() called")
    if (this.state !== ServerState.Running) {
      return this.boot()
    }
    await this.execute("s.reboot;")
    return "Server rebooted"
  }

  isRunning(): boolean {
    return this.state === ServerState.Running
  }

  getState(): ServerState {
    return this.state
  }

  getSamplesPath(): string {
    return this.config.samplesPath.replace(/"/g, '\\"')
  }

  getRecordingsPath(): string {
    return this.config.recordingsPath.replace(/"/g, '\\"')
  }
}
