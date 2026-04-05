export interface ScError {
  type: string
  message: string
  line?: number
  char?: number
  context?: string
}

export class OutputParser {
  private static readonly END_MARKER = "<<<END<<<"
  private static readonly SERVER_READY = "SERVER_READY"
  private static readonly SCLANG_READY = "Welcome to SuperCollider"
  private static readonly CC_READY = "*** ClaudeCollider ready ***"
  private static readonly SCLANG_PROMPT = "sc3> "

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

  hasCCReady(): boolean {
    return this.buffer.includes(OutputParser.CC_READY)
  }

  extractResult(): string | null {
    const endIndex = this.buffer.indexOf(OutputParser.END_MARKER)

    if (endIndex !== -1) {
      return (
        this.buffer
          .slice(0, endIndex)
          .trim()
          .replace(OutputParser.SCLANG_PROMPT, "") || "OK"
      )
    }
    return null
  }

  static wrapCode(code: string): string {
    // Strip trailing semicolons so (code).postln doesn't become (code;).postln
    const trimmed = code.trim().replace(/;+$/, "")
    return `try { (${trimmed}).postln } { |err| ("ERROR: " ++ err.errorString).postln }; "${OutputParser.END_MARKER}".postln;`
  }

  static bootCommand(): string {
    return `
      s.waitForBoot { "${OutputParser.SERVER_READY}".postln };
    `
  }

  hasError(): boolean {
    return this.buffer.includes("ERROR:")
  }

  hasPrompt(): boolean {
    return this.buffer.includes(OutputParser.SCLANG_PROMPT)
  }

  hasSyntaxError(): boolean {
    return this.buffer.includes("syntax error,")
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

  /**
   * Parse sclang compile-time syntax errors. These bypass the try/catch wrapper
   * entirely, so no END_MARKER is emitted. Format:
   *   line N char M:
   *     <code line>
   *           ^
   *   syntax error, unexpected TOKEN[, expecting TOKEN]
   */
  parseSyntaxError(): ScError | null {
    if (!this.hasSyntaxError()) {
      return null
    }

    const syntaxMatch = this.buffer.match(
      /syntax error,\s*(.+?)(?:\n|$)/
    )
    if (!syntaxMatch) {
      return null
    }

    const error: ScError = {
      type: "syntax",
      message: `syntax error, ${syntaxMatch[1].trim()}`,
    }

    const posMatch = this.buffer.match(/line\s+(\d+)\s+char\s+(\d+)/)
    if (posMatch) {
      error.line = parseInt(posMatch[1], 10)
      error.char = parseInt(posMatch[2], 10)
    }

    const contextMatch = this.buffer.match(/:\s+(.+)\n\s*\^/)
    if (contextMatch) {
      error.context = contextMatch[1].trim()
    }

    return error
  }

  formatError(): string | null {
    const error = this.parseError() || this.parseSyntaxError()
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
