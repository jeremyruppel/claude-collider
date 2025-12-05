// Effect metadata - populated dynamically from SuperCollider

import { SuperCollider } from "./supercollider.js"

export interface EffectParam {
  name: string
  default: number
}

export interface EffectMeta {
  name: string
  description: string
  params: EffectParam[]
}

export class Effects {
  private defs: Record<string, EffectMeta> = {}
  private sc: SuperCollider

  constructor(sc: SuperCollider) {
    this.sc = sc
  }

  async load(): Promise<void> {
    const result = await this.sc.execute("~cc.fx.describe")
    this.defs = {}
    for (const line of result.split("\n")) {
      const [name, description, paramsStr] = line.split("|")
      if (name) {
        const params: EffectParam[] = paramsStr
          ? paramsStr.split(",").map((p) => {
              const [paramName, defaultVal] = p.split(":")
              return { name: paramName, default: parseFloat(defaultVal) }
            })
          : []
        this.defs[name] = { name, description: description || "", params }
      }
    }
  }

  get(name: string): EffectMeta | undefined {
    return this.defs[name]
  }

  names(): string[] {
    return Object.keys(this.defs)
  }

  all(): EffectMeta[] {
    return Object.values(this.defs)
  }

  isEmpty(): boolean {
    return Object.keys(this.defs).length === 0
  }

  formatList(): string {
    return this.all()
      .map((e) => `  - ${e.name}: ${e.description}`)
      .join("\n")
  }
}
