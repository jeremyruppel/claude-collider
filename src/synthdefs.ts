// SynthDef metadata - populated dynamically from SuperCollider

import { SuperCollider } from "./supercollider.js"

export interface SynthDefMeta {
  name: string
  description: string
  params: string[]
}

export class SynthDefs {
  private defs: Record<string, SynthDefMeta> = {}
  private sc: SuperCollider

  constructor(sc: SuperCollider) {
    this.sc = sc
  }

  async load(): Promise<void> {
    const result = await this.sc.execute("~cc.synths.describe")
    this.defs = {}
    for (const line of result.split("\n")) {
      const [name, description, paramsStr] = line.split("|")
      if (name) {
        this.defs[name] = {
          name,
          description: description || "",
          params: paramsStr ? paramsStr.split(",") : [],
        }
      }
    }
  }

  get(name: string): SynthDefMeta | undefined {
    return this.defs[name]
  }

  names(): string[] {
    return Object.keys(this.defs)
  }

  all(): SynthDefMeta[] {
    return Object.values(this.defs)
  }

  isEmpty(): boolean {
    return Object.keys(this.defs).length === 0
  }

  formatList(): string {
    return this.all()
      .map((s) => `  ${s.name}: ${s.description}`)
      .join("\n")
  }
}
