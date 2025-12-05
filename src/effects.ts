// Effect metadata - populated dynamically from SuperCollider

import { SuperCollider } from "./supercollider.js"

export class Effects {
  private description = ""
  private sc: SuperCollider

  constructor(sc: SuperCollider) {
    this.sc = sc
  }

  async load(): Promise<void> {
    this.description = await this.sc.execute("~cc.fx.describe")
  }

  format(): string {
    return this.description || "No effects loaded"
  }
}
