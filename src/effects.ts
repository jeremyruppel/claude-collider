/**
 * Effects Manager for the Vibe Music Server
 * Manages loading, chaining, and routing of audio effects
 */

import { SuperCollider } from './supercollider.js';
import { effectsLibrary, EffectDefinition, EffectParam } from './effects-library.js';

export interface LoadedEffect {
  slot: string;
  type: string;
  inputBus: string;
  params: Record<string, EffectParam>;
  chain?: string; // If part of a chain
  bypassed: boolean;
  originalCode?: string; // For bypass/restore
}

export interface EffectChain {
  name: string;
  inputBus: string;
  effects: Array<{ slot: string; name: string }>;
}

export interface FxLoadResult {
  slot: string;
  inputBus: string;
  params: Record<string, { min: number; max: number; default: number }>;
  usage: string;
}

export interface FxChainResult {
  name: string;
  inputBus: string;
  effects: Array<{ slot: string; name: string }>;
  usage: string;
}

export interface FxListResult {
  effects: Array<{
    slot: string;
    type: string;
    chain?: string;
    params: Record<string, { min: number; max: number; default: number; current?: number }>;
    playing: boolean;
    bypassed: boolean;
  }>;
  chains: Array<{
    name: string;
    inputBus: string;
    effects: string[];
  }>;
}

export class EffectsManager {
  private sc: SuperCollider;
  private effects: Map<string, LoadedEffect> = new Map();
  private chains: Map<string, EffectChain> = new Map();

  constructor(supercollider: SuperCollider) {
    this.sc = supercollider;
  }

  /**
   * Load a pre-built effect into an Ndef
   */
  async load(name: string, slot?: string): Promise<FxLoadResult> {
    const effectDef = effectsLibrary[name];
    if (!effectDef) {
      const available = Object.keys(effectsLibrary).join(', ');
      throw new Error(`Unknown effect: ${name}. Available effects: ${available}`);
    }

    const effectSlot = slot || `fx_${name}`;
    const inputBus = `~${effectSlot}_bus`;

    // Generate SuperCollider code
    const code = `
${inputBus} = Bus.audio(s, 2);
Ndef(\\${effectSlot}, ${effectDef.generateCode(inputBus)}).play;
"Effect ${effectSlot} loaded with input bus ${inputBus}"
`;

    await this.sc.execute(code);

    // Track the loaded effect
    const loadedEffect: LoadedEffect = {
      slot: effectSlot,
      type: name,
      inputBus,
      params: effectDef.params,
      bypassed: false,
      originalCode: effectDef.generateCode(inputBus),
    };
    this.effects.set(effectSlot, loadedEffect);

    // Build simplified params for return
    const params: Record<string, { min: number; max: number; default: number }> = {};
    for (const [paramName, paramDef] of Object.entries(effectDef.params)) {
      params[paramName] = {
        min: paramDef.min,
        max: paramDef.max,
        default: paramDef.default,
      };
    }

    return {
      slot: effectSlot,
      inputBus,
      params,
      usage: `Send audio with: \\out, ${inputBus}`,
    };
  }

  /**
   * Set parameters on a loaded effect
   */
  async set(slot: string, params: Record<string, number>): Promise<string> {
    const effect = this.effects.get(slot);
    if (!effect) {
      const available = Array.from(this.effects.keys()).join(', ') || '(none loaded)';
      throw new Error(`Effect not loaded: ${slot}. Loaded effects: ${available}`);
    }

    // Validate and clamp parameters
    const validParams: string[] = [];
    const warnings: string[] = [];

    for (const [paramName, value] of Object.entries(params)) {
      const paramDef = effect.params[paramName];
      if (!paramDef) {
        const validNames = Object.keys(effect.params).join(', ');
        throw new Error(`Invalid parameter: ${paramName}. Valid params for ${effect.type}: ${validNames}`);
      }

      // Clamp value to valid range
      let clampedValue = value;
      if (value < paramDef.min) {
        clampedValue = paramDef.min;
        warnings.push(`${paramName} clamped to min ${paramDef.min}`);
      } else if (value > paramDef.max) {
        clampedValue = paramDef.max;
        warnings.push(`${paramName} clamped to max ${paramDef.max}`);
      }

      validParams.push(`\\${paramName}, ${clampedValue}`);
    }

    const code = `Ndef(\\${slot}).set(${validParams.join(', ')});`;
    await this.sc.execute(code);

    let result = `Set ${Object.keys(params).join(', ')} on ${slot}`;
    if (warnings.length > 0) {
      result += ` (${warnings.join('; ')})`;
    }
    return result;
  }

  /**
   * Create a chain of effects in series
   */
  async chain(name: string, effects: Array<{ name: string; params?: Record<string, number> }>): Promise<FxChainResult> {
    if (effects.length === 0) {
      throw new Error('Chain must have at least one effect');
    }

    // Validate all effects exist
    for (const effect of effects) {
      if (!effectsLibrary[effect.name]) {
        const available = Object.keys(effectsLibrary).join(', ');
        throw new Error(`Unknown effect: ${effect.name}. Available effects: ${available}`);
      }
    }

    const chainInputBus = `~chain_${name}_in`;
    const chainEffects: Array<{ slot: string; name: string }> = [];

    // Build the SuperCollider code for the chain
    let code = `// Create chain input bus\n${chainInputBus} = Bus.audio(s, 2);\n`;

    // Create inter-effect buses
    for (let i = 0; i < effects.length - 1; i++) {
      code += `~chain_${name}_${i + 1} = Bus.audio(s, 2);\n`;
    }
    code += '\n';

    // Create each effect in the chain
    for (let i = 0; i < effects.length; i++) {
      const effect = effects[i];
      const effectDef = effectsLibrary[effect.name];
      const slot = `chain_${name}_${effect.name}`;

      // Determine input and output buses
      const inputBus = i === 0 ? chainInputBus : `~chain_${name}_${i}`;
      const isLast = i === effects.length - 1;

      // Build parameter string with custom values if provided
      const paramStrings: string[] = [];
      for (const [paramName, paramDef] of Object.entries(effectDef.params)) {
        const value = effect.params?.[paramName] ?? paramDef.default;
        paramStrings.push(`${paramName}=${value}`);
      }

      // Generate the function code
      const funcCode = effectDef.generateCode(inputBus);

      if (isLast) {
        // Last effect outputs to main out (plays directly)
        code += `Ndef(\\${slot}, ${funcCode}).play;\n`;
      } else {
        // Intermediate effects write to next bus
        const outputBus = `~chain_${name}_${i + 1}`;
        // Wrap the function to output to the next bus
        code += `Ndef(\\${slot}, { Out.ar(${outputBus}, ${funcCode.replace(/^\{/, '').replace(/\}$/, '')}) }).play;\n`;
      }

      chainEffects.push({ slot, name: effect.name });

      // Track as a loaded effect
      this.effects.set(slot, {
        slot,
        type: effect.name,
        inputBus,
        params: effectDef.params,
        chain: name,
        bypassed: false,
        originalCode: funcCode,
      });
    }

    code += `\n"Chain ${name} created with ${effects.length} effects"`;

    await this.sc.execute(code);

    // Track the chain
    const chainData: EffectChain = {
      name,
      inputBus: chainInputBus,
      effects: chainEffects,
    };
    this.chains.set(name, chainData);

    return {
      name,
      inputBus: chainInputBus,
      effects: chainEffects,
      usage: `Send audio with: \\out, ${chainInputBus}`,
    };
  }

  /**
   * Route a source (Pdef or Ndef) to an effect or chain
   */
  async route(source: string, target: string): Promise<string> {
    // Determine the target bus
    let targetBus: string;

    // Check if target is a chain
    const chain = this.chains.get(target);
    if (chain) {
      targetBus = chain.inputBus;
    } else {
      // Check if target is a standalone effect
      const effect = this.effects.get(target);
      if (effect) {
        targetBus = effect.inputBus;
      } else {
        // Check for fx_ prefix
        const fxEffect = this.effects.get(`fx_${target}`);
        if (fxEffect) {
          targetBus = fxEffect.inputBus;
        } else {
          const available = [
            ...Array.from(this.effects.keys()),
            ...Array.from(this.chains.keys()),
          ].join(', ') || '(none)';
          throw new Error(`Target not found: ${target}. Available: ${available}`);
        }
      }
    }

    // Try routing as Pdef first, then Ndef
    const code = `
(
if(Pdef(\\${source}).isPlaying) {
  Pdef(\\${source}).set(\\out, ${targetBus});
  "Routed Pdef ${source} to ${targetBus}"
} {
  if(Ndef(\\${source}).isPlaying) {
    Ndef(\\${source}_routed, { Out.ar(${targetBus}, Ndef(\\${source}).ar) }).play;
    "Routed Ndef ${source} to ${targetBus}"
  } {
    "Source ${source} not found or not playing"
  }
}
)
`;

    const result = await this.sc.execute(code);
    return `Routed ${source} to ${target} (${targetBus})`;
  }

  /**
   * Bypass an effect (pass audio through unchanged)
   */
  async bypass(slot: string, bypass: boolean = true): Promise<string> {
    const effect = this.effects.get(slot);
    if (!effect) {
      throw new Error(`Effect not loaded: ${slot}`);
    }

    if (bypass) {
      // Replace with pass-through
      const code = `Ndef(\\${slot}, { In.ar(${effect.inputBus}, 2) });`;
      await this.sc.execute(code);
      effect.bypassed = true;
      return `Bypassed ${slot}`;
    } else {
      // Restore original function
      if (effect.originalCode) {
        const code = `Ndef(\\${slot}, ${effect.originalCode});`;
        await this.sc.execute(code);
        effect.bypassed = false;
        return `Un-bypassed ${slot}`;
      }
      return `Cannot restore ${slot}: original code not stored`;
    }
  }

  /**
   * Remove an effect and free its resources
   */
  async remove(slot: string): Promise<string> {
    // Check if it's a chain
    const chain = this.chains.get(slot);
    if (chain) {
      // Remove all effects in the chain
      let code = '';
      for (const effect of chain.effects) {
        code += `Ndef(\\${effect.slot}).clear;\n`;
        this.effects.delete(effect.slot);
      }
      // Free the chain buses
      code += `${chain.inputBus}.free;\n`;
      for (let i = 1; i < chain.effects.length; i++) {
        code += `~chain_${chain.name}_${i}.free;\n`;
      }
      code += `"Chain ${slot} removed"`;
      await this.sc.execute(code);
      this.chains.delete(slot);
      return `Removed chain ${slot} with ${chain.effects.length} effects`;
    }

    // Check if it's a standalone effect
    const effect = this.effects.get(slot);
    if (!effect) {
      throw new Error(`Effect or chain not found: ${slot}`);
    }

    const code = `
Ndef(\\${slot}).clear;
${effect.inputBus}.free;
"Effect ${slot} removed"
`;
    await this.sc.execute(code);
    this.effects.delete(slot);
    return `Removed effect ${slot}`;
  }

  /**
   * List all loaded effects and chains
   */
  async list(): Promise<FxListResult> {
    const effectsList: FxListResult['effects'] = [];
    const chainsList: FxListResult['chains'] = [];

    // List effects
    for (const [slot, effect] of this.effects) {
      const params: Record<string, { min: number; max: number; default: number }> = {};
      for (const [paramName, paramDef] of Object.entries(effect.params)) {
        params[paramName] = {
          min: paramDef.min,
          max: paramDef.max,
          default: paramDef.default,
        };
      }

      effectsList.push({
        slot,
        type: effect.type,
        chain: effect.chain,
        params,
        playing: true, // Assume playing if tracked
        bypassed: effect.bypassed,
      });
    }

    // List chains
    for (const [name, chain] of this.chains) {
      chainsList.push({
        name,
        inputBus: chain.inputBus,
        effects: chain.effects.map(e => e.name),
      });
    }

    return {
      effects: effectsList,
      chains: chainsList,
    };
  }

  /**
   * Get available effect names
   */
  getAvailableEffects(): string[] {
    return Object.keys(effectsLibrary);
  }

  /**
   * Get effect definition by name
   */
  getEffectDefinition(name: string): EffectDefinition | undefined {
    return effectsLibrary[name];
  }
}
