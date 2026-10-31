import { existsSync } from "fs"
import { homedir } from "os"
import { join } from "path"
import { fileURLToPath } from "url"

const MACOS_SCLANG_PATH =
  "/Applications/SuperCollider.app/Contents/MacOS/sclang"

// The repo root, found from this file's location (dist/config.js) so it
// works no matter which directory the server is started from.
const REPO_DIR = fileURLToPath(new URL("..", import.meta.url))
const USER_DIR = join(REPO_DIR, "user")

// Older default location, still searched for samples.
const LEGACY_CC_DIR = join(homedir(), ".claudecollider")

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
    this.recordingsPath = process.env.CC_RECORDINGS_PATH || join(USER_DIR, "recordings")
  }

  // Search order matters: the first directory wins when two samples share
  // a name, so user samples override the bundled ones.
  private buildSamplesPath(): string {
    if (process.env.CC_SAMPLES_PATH) {
      return process.env.CC_SAMPLES_PATH
    }
    return [
      join(USER_DIR, "samples"),
      join(REPO_DIR, "samples"),
      join(LEGACY_CC_DIR, "samples"),
    ].join(":")
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
