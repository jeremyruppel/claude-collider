import { existsSync } from "fs"

const MACOS_SCLANG_PATH =
  "/Applications/SuperCollider.app/Contents/MacOS/sclang"

export class SclangConfig {
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
