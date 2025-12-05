// SynthDef metadata - populated dynamically from SuperCollider

import { SuperCollider } from "./supercollider.js"

export class SynthDefs {
  private description = ""
  private sc: SuperCollider

  constructor(sc: SuperCollider) {
    this.sc = sc
  }

  async load(): Promise<void> {
    this.description = await this.sc.execute("~cc.synths.describe")
  }

  format(): string {
    return this.description || "No synths loaded"
  }
}
