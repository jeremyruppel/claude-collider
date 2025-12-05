import { SuperCollider } from "./supercollider.js"

export class Samples {
  private sc: SuperCollider

  constructor(sc: SuperCollider) {
    this.sc = sc
  }

  async load(): Promise<void> {
    await this.sc.execute(`~cc.samples.loadAll`)
  }

  async format(): Promise<string> {
    return this.sc.execute(`~cc.samples.describe`)
  }

  async play(name: string, rate: number = 1, amp: number = 0.5): Promise<void> {
    await this.sc.execute(`~cc.samples.play("${name}", ${rate}, ${amp})`)
  }

  async free(name: string): Promise<void> {
    await this.sc.execute(`~cc.samples.free("${name}")`)
  }

  async freeAll(): Promise<void> {
    await this.sc.execute(`~cc.samples.freeAll`)
  }
}
