// Run the SuperCollider test suite with the same sclang the MCP server uses.
// Extra arguments are passed through to sclang.
import { spawn } from "child_process"
import { fileURLToPath } from "url"
import { SclangConfig } from "./config.js"

const RUN_TESTS = fileURLToPath(
  new URL("../ClaudeCollider/tests/run_tests.scd", import.meta.url)
)

const { path } = new SclangConfig()

const sclang = spawn(path, [RUN_TESTS, ...process.argv.slice(2)], {
  stdio: "inherit",
})

sclang.on("error", (err: NodeJS.ErrnoException) => {
  if (err.code === "ENOENT") {
    console.error(
      `error: sclang not found at '${path}'. Install SuperCollider or set SCLANG_PATH.`
    )
  } else {
    console.error(`error: could not run sclang: ${err.message}`)
  }
  process.exit(1)
})

sclang.on("exit", (code, signal) => {
  process.exit(signal ? 1 : (code ?? 1))
})
