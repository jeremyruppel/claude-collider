import { appendFileSync } from "fs"

const LOG_FILE = "/tmp/claude-collider.log"

export function debug(message: string): void {
  const line = `[${new Date().toISOString()}] ${message}\n`
  appendFileSync(LOG_FILE, line)
}
