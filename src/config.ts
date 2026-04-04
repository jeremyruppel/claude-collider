import { existsSync } from "fs"
import { homedir } from "os"
import { join } from "path"

const MACOS_SCLANG_PATH =
  "/Applications/SuperCollider.app/Contents/MacOS/sclang"

const DEFAULT_CC_DIR = join(homedir(), ".claudecollider")

export class SclangConfig {
  readonly path: string
  readonly bootTimeout: number
  readonly execTimeout: number
  readonly samplesPath: string
  readonly recordingsPath: string

  constructor() {
    this.path = this.findSclangPath()
    this.bootTimeout = parseInt(process.env.SC_BOOT_TIMEOUT || "10000", 10)
    this.execTimeout = parseInt(process.env.SC_EXEC_TIMEOUT || "2000", 10)
    this.samplesPath = this.buildSamplesPath()
    this.recordingsPath = process.env.CC_RECORDINGS_PATH || join(DEFAULT_CC_DIR, "recordings")
  }

  private buildSamplesPath(): string {
    const defaultDir = join(DEFAULT_CC_DIR, "samples")
    const localDir = join(process.cwd(), "samples")
    if (process.env.CC_SAMPLES_PATH) {
      return process.env.CC_SAMPLES_PATH
    }
    return [localDir, defaultDir].join(":")
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
