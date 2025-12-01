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
    return `"${OutputParser.BEGIN_MARKER}".postln; (\n${code}\n).value; "${OutputParser.END_MARKER}".postln;`
  }

  static bootCommand(): string {
    return `s.waitForBoot { "${OutputParser.SERVER_READY}".postln };`
  }
}
