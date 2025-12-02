export interface ScError {
  type: string
  message: string
  line?: number
  char?: number
  context?: string
}

export class OutputParser {
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
    // Remove newlines - sclang interprets each line independently via stdin
    const singleLine = code.replace(/\n/g, " ").replace(/\s+/g, " ").trim()
    return `"${OutputParser.BEGIN_MARKER}".postln; (${singleLine}).value; "${OutputParser.END_MARKER}".postln;`
  }

  static bootCommand(): string {
    return `s.waitForBoot { "${OutputParser.SERVER_READY}".postln };`
  }

  hasError(): boolean {
    return this.buffer.includes("ERROR:")
  }

  parseError(): ScError | null {
    if (!this.hasError()) {
      return null
    }

    const errorMatch = this.buffer.match(/ERROR:\s*(.+?)(?:\n|$)/)
    if (!errorMatch) {
      return null
    }

    const errorLine = errorMatch[1].trim()
    const error: ScError = {
      type: "unknown",
      message: errorLine,
    }

    // Parse syntax errors: "syntax error, unexpected TOKEN, expecting TOKEN"
    if (errorLine.includes("syntax error")) {
      error.type = "syntax"
    }
    // Parse "not understood" errors: "Message 'foo' not understood"
    else if (errorLine.includes("not understood")) {
      error.type = "not_understood"
      const msgMatch = errorLine.match(/Message '(.+?)' not understood/)
      if (msgMatch) {
        error.message = `Unknown method or message: '${msgMatch[1]}'`
      }
    }
    // Parse "does not understand" errors
    else if (errorLine.includes("does not understand")) {
      error.type = "not_understood"
    }
    // Parse primitive failed errors
    else if (errorLine.includes("Primitive")) {
      error.type = "primitive_failed"
    }

    // Extract line and char position: "line N char M"
    const posMatch = this.buffer.match(/line\s+(\d+)\s+char\s+(\d+)/)
    if (posMatch) {
      error.line = parseInt(posMatch[1], 10)
      error.char = parseInt(posMatch[2], 10)
    }

    // Extract code context (the line with the caret showing error location)
    const contextMatch = this.buffer.match(/:\s+(.+)\n\s*\^/)
    if (contextMatch) {
      error.context = contextMatch[1].trim()
    }

    return error
  }

  formatError(): string | null {
    const error = this.parseError()
    if (!error) {
      return null
    }

    let msg = `Error: ${error.message}`

    if (error.line !== undefined) {
      msg += ` (line ${error.line}`
      if (error.char !== undefined) {
        msg += `, char ${error.char}`
      }
      msg += ")"
    }

    if (error.context) {
      msg += `\n  → ${error.context}`
    }

    return msg
  }
}
