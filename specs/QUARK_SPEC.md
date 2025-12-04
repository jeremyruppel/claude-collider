# VibeCoding Quark — Technical Specification

A SuperCollider Quark that provides the SC-side API for the Vibe Music Server. This moves complexity out of Node.js string templating and into proper, testable SC code.

## Architecture

```
┌─────────────┐     MCP/stdio      ┌──────────────────┐
│   Claude    │◄──────────────────►│  MCP Server      │
│   Desktop   │                    │  (Node.js)       │
└─────────────┘                    └────────┬─────────┘
                                            │
                                    thin wrapper calls
                                            │
                                            ▼
                                   ┌──────────────────┐
                                   │  VibeCoding      │
                                   │  Quark (SC)      │
                                   ├──────────────────┤
                                   │ • VibeSynths     │
                                   │ • VibeFX         │
                                   │ • VibeMIDI       │
                                   │ • VibeMIDIFile   │
                                   │ • VibePatterns   │
                                   └──────────────────┘
```

## Why a Quark?

| Before (Node templates) | After (Quark) |
|-------------------------|---------------|
| `Pdef(\\${name}, Pbind(...` | `~vibe.pattern(\name, ...)` |
| Fragile string concatenation | Type-safe SC methods |
| Errors surface in sclang | Errors caught in Quark |
| Hard to test | Test in SC IDE |
| Lives in MCP server | Standalone, distributable |

## File Structure

```
VibeCoding/
├── VibeCoding.quark              # Quark metadata
├── classes/
│   ├── VibeCoding.sc             # Main facade class
│   ├── VibeSynths.sc             # SynthDef management
│   ├── VibeFX.sc                 # Effects system
│   ├── VibeMIDI.sc               # MIDI I/O and mapping
│   ├── VibeMIDIFile.sc           # MIDI file import
│   └── VibeState.sc              # Session state management
├── synthdefs/
│   ├── drums.scd                 # Kick, snare, hat, clap
│   ├── bass.scd                  # Bass synths
│   ├── leads.scd                 # Lead synths
│   ├── pads.scd                  # Pad synths
│   └── fx.scd                    # Effect SynthDefs
├── HelpSource/
│   └── Classes/
│       └── VibeCoding.schelp     # Documentation
└── examples/
    ├── basic.scd
    ├── midi_setup.scd
    └── effects_chain.scd
```

## Quark Metadata

```supercollider
// VibeCoding.quark
(
  name: "VibeCoding",
  summary: "Live coding toolkit for MCP/Claude integration",
  author: "Your Name",
  version: "0.1.0",
  schelp: "VibeCoding",
  dependencies: [],  // wslib optional for extended MIDI file support
  url: "https://github.com/yourname/VibeCoding"
)
```

---

# Class Specifications

## VibeCoding (Main Facade)

The main entry point. Stored in `~vibe` by convention.

### Class Definition

```supercollider
VibeCoding {
  classvar <instance;
  
  var <server;
  var <synths;
  var <fx;
  var <midi;
  var <midiFile;
  var <state;
  var <isBooted;
  
  *new { |server|
    ^super.new.init(server ?? Server.default);
  }
  
  *boot { |server, onComplete|
    instance = this.new(server);
    instance.boot(onComplete);
    ^instance;
  }
  
  init { |argServer|
    server = argServer;
    synths = VibeSynths(this);
    fx = VibeFX(this);
    midi = VibeMIDI(this);
    midiFile = VibeMIDIFile(this);
    state = VibeState(this);
    isBooted = false;
  }
  
  boot { |onComplete|
    server.waitForBoot {
      this.loadSynthDefs;
      Pdef.defaultQuant = 4;
      isBooted = true;
      "*** VibeCoding ready ***".postln;
      onComplete.value(this);
    };
  }
  
  loadSynthDefs {
    synths.loadAll;
    fx.loadDefs;
  }
  
  tempo { |bpm|
    if(bpm.notNil) {
      TempoClock.default.tempo = bpm / 60;
      ^bpm;
    } {
      ^TempoClock.default.tempo * 60;
    };
  }
  
  stop {
    Pdef.all.do(_.stop);
    Ndef.all.do(_.stop);
  }
  
  clear {
    this.stop;
    Pdef.all.do(_.clear);
    Ndef.all.do(_.clear);
    fx.clearAll;
    midi.clearAll;
    state.clear;
  }
  
  status {
    ^(
      booted: isBooted,
      tempo: this.tempo,
      synths: server.numSynths,
      cpu: server.avgCPU.round(0.1),
      pdefs: Pdef.all.select(_.isPlaying).keys.asArray,
      ndefs: Ndef.all.select(_.isPlaying).keys.asArray
    );
  }
}
```

### Usage

```supercollider
// Boot
~vibe = VibeCoding.boot;

// Or with callback
VibeCoding.boot(onComplete: { |v| "Ready!".postln });

// Access subsystems
~vibe.synths
~vibe.fx
~vibe.midi
~vibe.midiFile

// Global controls
~vibe.tempo(120);
~vibe.stop;
~vibe.clear;
~vibe.status;
```

---

## VibeSynths

Manages SynthDef loading and triggering.

### Class Definition

```supercollider
VibeSynths {
  var <vibe;
  var <loaded;
  var <defs;
  
  *new { |vibe|
    ^super.new.init(vibe);
  }
  
  init { |argVibe|
    vibe = argVibe;
    loaded = Set[];
    defs = this.defineSynths;
  }
  
  defineSynths {
    ^(
      // ========== DRUMS ==========
      kick: {
        SynthDef(\vibe_kick, { |out=0, freq=60, amp=0.8, pan=0, att=0.001, rel=0.3|
          var sig, env, fenv;
          fenv = EnvGen.kr(Env.perc(0.001, 0.1), levelScale: 4);
          env = EnvGen.kr(Env.perc(att, rel), doneAction: 2);
          sig = SinOsc.ar(freq * (1 + fenv)) * env;
          sig = sig + (SinOsc.ar(freq * 0.5) * env * 0.5);
          sig = (sig * 2).tanh;
          Out.ar(out, Pan2.ar(sig * amp, pan));
        })
      },
      
      snare: {
        SynthDef(\vibe_snare, { |out=0, freq=180, amp=0.7, pan=0, att=0.001, rel=0.2|
          var sig, env, noise;
          env = EnvGen.kr(Env.perc(att, rel), doneAction: 2);
          sig = SinOsc.ar(freq) * EnvGen.kr(Env.perc(0.001, 0.05));
          noise = WhiteNoise.ar * EnvGen.kr(Env.perc(0.001, rel));
          sig = HPF.ar(sig + noise, 100);
          Out.ar(out, Pan2.ar(sig * amp, pan));
        })
      },
      
      hihat: {
        SynthDef(\vibe_hihat, { |out=0, amp=0.5, pan=0, att=0.001, rel=0.08, ffreq=8000|
          var sig, env;
          env = EnvGen.kr(Env.perc(att, rel), doneAction: 2);
          sig = WhiteNoise.ar;
          sig = HPF.ar(sig, ffreq);
          Out.ar(out, Pan2.ar(sig * env * amp, pan));
        })
      },
      
      clap: {
        SynthDef(\vibe_clap, { |out=0, amp=0.7, pan=0, rel=0.3|
          var sig, env;
          env = EnvGen.kr(
            Env([0, 1, 0.3, 0.8, 0], [0.001, 0.01, 0.002, rel]),
            doneAction: 2
          );
          sig = BPF.ar(WhiteNoise.ar, 1500, 0.5);
          Out.ar(out, Pan2.ar(sig * env * amp, pan));
        })
      },
      
      // ========== BASS ==========
      bass: {
        SynthDef(\vibe_bass, { |out=0, freq=55, amp=0.6, pan=0, gate=1,
                               att=0.01, rel=0.3, cutoff=1000, res=0.3|
          var sig, env;
          env = EnvGen.kr(Env.adsr(att, 0.1, 0.7, rel), gate, doneAction: 2);
          sig = Saw.ar(freq) + Pulse.ar(freq * 0.5, 0.3, 0.5);
          sig = RLPF.ar(sig, cutoff * env.range(0.5, 1), res);
          Out.ar(out, Pan2.ar(sig * env * amp, pan));
        })
      },
      
      acid: {
        SynthDef(\vibe_acid, { |out=0, freq=55, amp=0.6, pan=0, gate=1,
                               att=0.01, dec=0.2, rel=0.1,
                               cutoff=1000, res=0.4, envamt=4000|
          var sig, env, fenv;
          env = EnvGen.kr(Env.adsr(att, dec, 0.6, rel), gate, doneAction: 2);
          fenv = EnvGen.kr(Env.perc(0.001, dec * 2));
          sig = Saw.ar(freq);
          sig = RLPF.ar(sig, cutoff + (fenv * envamt), res);
          sig = (sig * 1.5).tanh;
          Out.ar(out, Pan2.ar(sig * env * amp, pan));
        })
      },
      
      // ========== LEADS ==========
      lead: {
        SynthDef(\vibe_lead, { |out=0, freq=440, amp=0.5, pan=0, gate=1,
                               att=0.01, rel=0.3, cutoff=4000, res=0.2|
          var sig, env;
          env = EnvGen.kr(Env.adsr(att, 0.1, 0.8, rel), gate, doneAction: 2);
          sig = Saw.ar(freq) + Saw.ar(freq * 1.005);
          sig = RLPF.ar(sig, cutoff, res);
          Out.ar(out, Pan2.ar(sig * env * amp, pan));
        })
      },
      
      // ========== PADS ==========
      pad: {
        SynthDef(\vibe_pad, { |out=0, freq=440, amp=0.4, pan=0, gate=1,
                              att=0.5, rel=1, cutoff=2000|
          var sig, env;
          env = EnvGen.kr(Env.adsr(att, 0.3, 0.7, rel), gate, doneAction: 2);
          sig = Saw.ar(freq * [1, 1.003, 0.997]).sum / 3;
          sig = LPF.ar(sig, cutoff);
          sig = sig + (SinOsc.ar(freq) * 0.3);
          Out.ar(out, Pan2.ar(sig * env * amp, pan));
        })
      }
    );
  }
  
  loadAll {
    defs.keysValuesDo { |name, defFunc|
      defFunc.value.add;
      loaded.add(name);
    };
    "VibeSynths: loaded %".format(loaded.size).postln;
  }
  
  load { |name|
    var def = defs[name];
    if(def.notNil) {
      def.value.add;
      loaded.add(name);
      ^true;
    } {
      "VibeSynths: unknown synth '%'".format(name).warn;
      ^false;
    };
  }
  
  play { |name ...args|
    var synthName = ("vibe_" ++ name).asSymbol;
    if(loaded.includes(name).not) { this.load(name) };
    ^Synth(synthName, args);
  }
  
  list {
    ^defs.keys.asArray.sort;
  }
  
  info { |name|
    ^defs[name].notNil;
  }
}
```

### Usage

```supercollider
// Load a synth
~vibe.synths.load(\kick);

// Play a synth directly
~vibe.synths.play(\kick, \amp, 0.8);
~vibe.synths.play(\acid, \freq, 55, \cutoff, 2000);

// List available synths
~vibe.synths.list;  // -> [acid, bass, clap, hihat, kick, lead, pad, snare]

// Get full name for Pbind
\vibe_kick  // synths are prefixed
```

---

## VibeFX

Effects loading, chaining, and routing.

### Class Definition

```supercollider
VibeFX {
  var <vibe;
  var <defs;
  var <loaded;      // slot -> (name, ndef, inBus)
  var <chains;      // chainName -> [slots]
  
  *new { |vibe|
    ^super.new.init(vibe);
  }
  
  init { |argVibe|
    vibe = argVibe;
    loaded = Dictionary[];
    chains = Dictionary[];
    defs = this.defineEffects;
  }
  
  defineEffects {
    ^(
      // ========== FILTERS ==========
      lpf: (
        params: [\cutoff, 1000, \res, 0.5],
        func: { |in, cutoff=1000, res=0.5|
          RLPF.ar(In.ar(in, 2), cutoff, res);
        }
      ),
      
      hpf: (
        params: [\cutoff, 200, \res, 0.5],
        func: { |in, cutoff=200, res=0.5|
          RHPF.ar(In.ar(in, 2), cutoff, res);
        }
      ),
      
      bpf: (
        params: [\freq, 1000, \bw, 0.5],
        func: { |in, freq=1000, bw=0.5|
          BPF.ar(In.ar(in, 2), freq, bw);
        }
      ),
      
      // ========== TIME-BASED ==========
      reverb: (
        params: [\mix, 0.33, \room, 0.8, \damp, 0.5],
        func: { |in, mix=0.33, room=0.8, damp=0.5|
          var sig = In.ar(in, 2);
          FreeVerb2.ar(sig[0], sig[1], mix, room, damp);
        }
      ),
      
      delay: (
        params: [\time, 0.375, \fb, 0.5, \mix, 0.5],
        func: { |in, time=0.375, fb=0.5, mix=0.5|
          var sig = In.ar(in, 2);
          var delayed = CombL.ar(sig, 2, time, fb * 4);
          (sig * (1 - mix)) + (delayed * mix);
        }
      ),
      
      pingpong: (
        params: [\time, 0.375, \fb, 0.5, \mix, 0.5],
        func: { |in, time=0.375, fb=0.5, mix=0.5|
          var sig = In.ar(in, 2);
          var left = CombL.ar(sig[0], 2, time, fb * 4);
          var right = DelayL.ar(left, 2, time);
          (sig * (1 - mix)) + ([left, right] * mix);
        }
      ),
      
      // ========== MODULATION ==========
      chorus: (
        params: [\rate, 0.5, \depth, 0.005, \mix, 0.5],
        func: { |in, rate=0.5, depth=0.005, mix=0.5|
          var sig = In.ar(in, 2);
          var mod = SinOsc.kr([rate, rate * 1.01], [0, 0.5]).range(0.001, depth);
          var wet = DelayL.ar(sig, 0.1, mod);
          (sig * (1 - mix)) + (wet * mix);
        }
      ),
      
      flanger: (
        params: [\rate, 0.2, \depth, 0.003, \fb, 0.5, \mix, 0.5],
        func: { |in, rate=0.2, depth=0.003, fb=0.5, mix=0.5|
          var sig = In.ar(in, 2);
          var mod = SinOsc.kr(rate).range(0.0001, depth);
          var wet = DelayL.ar(sig + (LocalIn.ar(2) * fb), 0.02, mod);
          LocalOut.ar(wet);
          (sig * (1 - mix)) + (wet * mix);
        }
      ),
      
      phaser: (
        params: [\rate, 0.3, \depth, 2, \mix, 0.5],
        func: { |in, rate=0.3, depth=2, mix=0.5|
          var sig = In.ar(in, 2);
          var mod = SinOsc.kr(rate).range(100, 4000);
          var wet = AllpassL.ar(sig, 0.1, mod.reciprocal, 0);
          (sig * (1 - mix)) + (wet * mix);
        }
      ),
      
      tremolo: (
        params: [\rate, 4, \depth, 0.5],
        func: { |in, rate=4, depth=0.5|
          var sig = In.ar(in, 2);
          var mod = SinOsc.kr(rate).range(1 - depth, 1);
          sig * mod;
        }
      ),
      
      // ========== DISTORTION ==========
      distortion: (
        params: [\drive, 2, \tone, 0.5, \mix, 1],
        func: { |in, drive=2, tone=0.5, mix=1|
          var sig = In.ar(in, 2);
          var wet = (sig * drive).tanh;
          wet = LPF.ar(wet, tone.linexp(0, 1, 1000, 12000));
          (sig * (1 - mix)) + (wet * mix);
        }
      ),
      
      bitcrush: (
        params: [\bits, 8, \rate, 44100, \mix, 1],
        func: { |in, bits=8, rate=44100, mix=1|
          var sig = In.ar(in, 2);
          var crushed = sig.round(2.pow(1 - bits));
          var wet = Latch.ar(crushed, Impulse.ar(rate));
          (sig * (1 - mix)) + (wet * mix);
        }
      ),
      
      wavefold: (
        params: [\amount, 2, \mix, 1],
        func: { |in, amount=2, mix=1|
          var sig = In.ar(in, 2);
          var wet = (sig * amount).fold(-1, 1);
          (sig * (1 - mix)) + (wet * mix);
        }
      ),
      
      // ========== DYNAMICS ==========
      compressor: (
        params: [\thresh, 0.5, \ratio, 4, \att, 0.01, \rel, 0.1, \makeup, 1],
        func: { |in, thresh=0.5, ratio=4, att=0.01, rel=0.1, makeup=1|
          var sig = In.ar(in, 2);
          Compander.ar(sig, sig, thresh, 1, ratio.reciprocal, att, rel, makeup);
        }
      ),
      
      limiter: (
        params: [\level, 0.95, \lookahead, 0.01],
        func: { |in, level=0.95, lookahead=0.01|
          Limiter.ar(In.ar(in, 2), level, lookahead);
        }
      ),
      
      gate: (
        params: [\thresh, 0.1, \att, 0.01, \rel, 0.1],
        func: { |in, thresh=0.1, att=0.01, rel=0.1|
          var sig = In.ar(in, 2);
          var env = Amplitude.kr(sig).max > thresh;
          sig * Lag.kr(env, att, rel);
        }
      ),
      
      // ========== STEREO ==========
      widener: (
        params: [\width, 1.5],
        func: { |in, width=1.5|
          var sig = In.ar(in, 2);
          var mid = (sig[0] + sig[1]) * 0.5;
          var side = (sig[0] - sig[1]) * 0.5 * width;
          [mid + side, mid - side];
        }
      ),
      
      autopan: (
        params: [\rate, 1, \depth, 1],
        func: { |in, rate=1, depth=1|
          var sig = In.ar(in, 2);
          var pan = SinOsc.kr(rate).range(-1 * depth, depth);
          Balance2.ar(sig[0], sig[1], pan);
        }
      )
    );
  }
  
  loadDefs {
    // Precompile all effect defs
    "VibeFX: % effects available".format(defs.size).postln;
  }
  
  load { |name, slot|
    var def = defs[name];
    var slotName, inBus, ndef;
    
    if(def.isNil) {
      "VibeFX: unknown effect '%'".format(name).warn;
      ^nil;
    };
    
    slotName = slot ?? ("fx_" ++ name);
    inBus = Bus.audio(vibe.server, 2);
    
    ndef = Ndef(slotName.asSymbol, { |in|
      var params = def.params.clump(2).collect { |pair|
        NamedControl.kr(pair[0], pair[1]);
      };
      def.func.valueArray([in] ++ params);
    });
    ndef.set(\in, inBus);
    ndef.play;
    
    loaded[slotName.asSymbol] = (
      name: name,
      ndef: ndef,
      inBus: inBus,
      params: def.params
    );
    
    ^(slot: slotName, bus: inBus);
  }
  
  set { |slot, ...args|
    var info = loaded[slot.asSymbol];
    if(info.notNil) {
      info.ndef.set(*args);
      ^true;
    } {
      "VibeFX: slot '%' not loaded".format(slot).warn;
      ^false;
    };
  }
  
  chain { |name, effects|
    var buses = [];
    var slots = [];
    var chainIn = Bus.audio(vibe.server, 2);
    
    buses = buses.add(chainIn);
    
    effects.do { |fx, i|
      var fxName, fxParams, slotName, def, ndef, nextBus;
      
      if(fx.isKindOf(Association)) {
        fxName = fx.key;
        fxParams = fx.value;
      } {
        fxName = fx;
        fxParams = [];
      };
      
      def = defs[fxName];
      if(def.isNil) {
        "VibeFX: unknown effect '%' in chain".format(fxName).warn;
      } {
        slotName = "chain_%_%".format(name, fxName).asSymbol;
        
        if(i < (effects.size - 1)) {
          nextBus = Bus.audio(vibe.server, 2);
          buses = buses.add(nextBus);
        };
        
        ndef = Ndef(slotName, { |in|
          var sig = def.func.value(in);
          if(nextBus.notNil) {
            Out.ar(nextBus, sig);
          } {
            sig;
          };
        });
        ndef.set(\in, buses[i]);
        ndef.set(*fxParams);
        ndef.play;
        
        loaded[slotName] = (
          name: fxName,
          ndef: ndef,
          inBus: buses[i],
          chain: name
        );
        
        slots = slots.add(slotName);
      };
    };
    
    chains[name.asSymbol] = (
      inBus: chainIn,
      slots: slots,
      buses: buses
    );
    
    ^(name: name, bus: chainIn, slots: slots);
  }
  
  route { |source, target|
    var targetInfo, targetBus;
    
    // Find target bus
    if(chains[target.asSymbol].notNil) {
      targetBus = chains[target.asSymbol].inBus;
    } {
      if(loaded[target.asSymbol].notNil) {
        targetBus = loaded[target.asSymbol].inBus;
      };
    };
    
    if(targetBus.isNil) {
      "VibeFX: target '%' not found".format(target).warn;
      ^false;
    };
    
    // Route the source
    if(Pdef.all[source.asSymbol].notNil) {
      // It's a Pdef - set the out parameter
      Pdef(source.asSymbol).set(\out, targetBus);
    } {
      if(Ndef.all[source.asSymbol].notNil) {
        // It's an Ndef - create a routing Ndef
        Ndef((source ++ "_routed").asSymbol, {
          Out.ar(targetBus, Ndef(source.asSymbol).ar);
        }).play;
      } {
        "VibeFX: source '%' not found".format(source).warn;
        ^false;
      };
    };
    
    ^true;
  }
  
  bypass { |slot, shouldBypass=true|
    var info = loaded[slot.asSymbol];
    if(info.notNil) {
      if(shouldBypass) {
        info.ndef.stop;
      } {
        info.ndef.play;
      };
      ^true;
    };
    ^false;
  }
  
  remove { |slot|
    var info = loaded[slot.asSymbol];
    if(info.notNil) {
      info.ndef.clear;
      info.inBus.free;
      loaded.removeAt(slot.asSymbol);
      ^true;
    };
    ^false;
  }
  
  clearAll {
    loaded.keysValuesDo { |slot, info|
      info.ndef.clear;
      info.inBus.free;
    };
    chains.keysValuesDo { |name, info|
      info.buses.do(_.free);
    };
    loaded.clear;
    chains.clear;
  }
  
  list {
    ^defs.keys.asArray.sort;
  }
  
  status {
    ^(
      effects: loaded.keys.asArray,
      chains: chains.keys.asArray
    );
  }
}
```

### Usage

```supercollider
// Load a single effect
~vibe.fx.load(\reverb);
~vibe.fx.load(\lpf, \bassFilter);  // custom slot name

// Set parameters
~vibe.fx.set(\fx_reverb, \mix, 0.6, \room, 0.9);

// Create a chain
~vibe.fx.chain(\synthFX, [\lpf, \chorus, \reverb]);

// With parameters
~vibe.fx.chain(\synthFX, [
  \lpf -> [\cutoff, 2000],
  \chorus -> [\mix, 0.3],
  \reverb -> [\room, 0.9]
]);

// Route a pattern to effects
~vibe.fx.route(\bass, \synthFX);

// Bypass
~vibe.fx.bypass(\fx_reverb, true);
~vibe.fx.bypass(\fx_reverb, false);

// Remove
~vibe.fx.remove(\fx_reverb);

// List available
~vibe.fx.list;
```

---

## VibeMIDI

MIDI device management and mapping.

### Class Definition

```supercollider
VibeMIDI {
  var <vibe;
  var <initialized;
  var <inputs;
  var <output;
  var <noteMappings;
  var <ccMappings;
  var <noteState;     // for polyphonic note tracking
  var <eventLog;      // ring buffer of recent events
  var <logEnabled;
  var <defCounter;
  
  *new { |vibe|
    ^super.new.init(vibe);
  }
  
  init { |argVibe|
    vibe = argVibe;
    initialized = false;
    inputs = [];
    output = nil;
    noteMappings = [];
    ccMappings = Dictionary[];
    noteState = Array.fill(128, { nil });
    eventLog = List[];
    logEnabled = false;
    defCounter = 0;
  }
  
  initMIDI {
    if(initialized.not) {
      MIDIClient.init;
      initialized = true;
    };
  }
  
  listDevices {
    this.initMIDI;
    ^(
      inputs: MIDIClient.sources.collect { |src, i|
        (index: i, device: src.device, name: src.name)
      },
      outputs: MIDIClient.destinations.collect { |dst, i|
        (index: i, device: dst.device, name: dst.name)
      }
    );
  }
  
  connect { |device, direction=\in|
    var endpoint;
    
    this.initMIDI;
    
    // Find by index or name
    endpoint = if(device.isKindOf(Number)) {
      if(direction == \in) {
        MIDIClient.sources[device];
      } {
        MIDIClient.destinations[device];
      };
    } {
      if(direction == \in) {
        MIDIClient.sources.detect { |src|
          src.device.containsi(device.asString) or:
          { src.name.containsi(device.asString) }
        };
      } {
        MIDIClient.destinations.detect { |dst|
          dst.device.containsi(device.asString) or:
          { dst.name.containsi(device.asString) }
        };
      };
    };
    
    if(endpoint.isNil) {
      "VibeMIDI: device '%' not found".format(device).warn;
      ^false;
    };
    
    if(direction == \in) {
      MIDIIn.connect(inputs.size, endpoint);
      inputs = inputs.add(endpoint);
      "VibeMIDI: connected input '%'".format(endpoint.device).postln;
    } {
      output = MIDIOut(0);
      output.connect(endpoint.uid);
      "VibeMIDI: connected output '%'".format(endpoint.device).postln;
    };
    
    ^true;
  }
  
  connectAll {
    this.initMIDI;
    MIDIIn.connectAll;
    inputs = MIDIClient.sources.copy;
    "VibeMIDI: connected all inputs".postln;
  }
  
  mapNotes { |synthName, channel, velocityToAmp=true, mono=false|
    var onName = ("vibe_noteOn_" ++ defCounter).asSymbol;
    var offName = ("vibe_noteOff_" ++ defCounter).asSymbol;
    var fullSynthName = ("vibe_" ++ synthName).asSymbol;
    var chanArg = channel ?? nil;
    
    defCounter = defCounter + 1;
    
    if(mono) {
      // Monophonic mapping
      var monoSynth = nil;
      var monoNote = nil;
      
      MIDIdef.noteOn(onName, { |vel, note, chan|
        if(monoSynth.notNil) { monoSynth.set(\gate, 0) };
        monoSynth = Synth(fullSynthName, [
          \freq, note.midicps,
          \amp, if(velocityToAmp) { vel.linlin(0, 127, 0, 0.8) } { 0.8 },
          \gate, 1
        ]);
        monoNote = note;
        this.logEvent(\noteOn, note, vel, chan);
      }, chan: chanArg);
      
      MIDIdef.noteOff(offName, { |vel, note, chan|
        if(note == monoNote) {
          monoSynth.!?(_.set(\gate, 0));
          monoSynth = nil;
        };
        this.logEvent(\noteOff, note, vel, chan);
      }, chan: chanArg);
    } {
      // Polyphonic mapping
      MIDIdef.noteOn(onName, { |vel, note, chan|
        noteState[note] = Synth(fullSynthName, [
          \freq, note.midicps,
          \amp, if(velocityToAmp) { vel.linlin(0, 127, 0, 0.8) } { 0.8 },
          \gate, 1
        ]);
        this.logEvent(\noteOn, note, vel, chan);
      }, chan: chanArg);
      
      MIDIdef.noteOff(offName, { |vel, note, chan|
        noteState[note].!?(_.set(\gate, 0));
        noteState[note] = nil;
        this.logEvent(\noteOff, note, vel, chan);
      }, chan: chanArg);
    };
    
    noteMappings = noteMappings.add((
      onDef: onName,
      offDef: offName,
      synth: synthName,
      mono: mono
    ));
    
    ^true;
  }
  
  mapCC { |cc, busName, range=#[0, 1], curve=\lin, channel|
    var defName = ("vibe_cc_" ++ busName).asSymbol;
    var bus = currentEnvironment[busName.asSymbol];
    var chanArg = channel ?? nil;
    
    // Create bus if needed
    if(bus.isNil) {
      bus = Bus.control(vibe.server, 1);
      bus.set(range[0] + (range[1] - range[0]) / 2);  // midpoint
      currentEnvironment[busName.asSymbol] = bus;
    };
    
    MIDIdef.cc(defName, { |val, ccNum, chan|
      var mapped = if(curve == \exp) {
        val.linexp(0, 127, range[0], range[1]);
      } {
        val.linlin(0, 127, range[0], range[1]);
      };
      bus.set(mapped);
      this.logEvent(\cc, ccNum, val, chan);
    }, ccNum: cc, chan: chanArg);
    
    ccMappings[busName.asSymbol] = (
      cc: cc,
      bus: bus,
      range: range,
      curve: curve,
      def: defName
    );
    
    ^bus;
  }
  
  learn { |timeout=10, callback|
    var defName = \vibe_learn;
    var result = nil;
    
    this.initMIDI;
    this.connectAll;
    
    MIDIdef.cc(defName, { |val, cc, chan|
      result = (cc: cc, channel: chan, value: val);
      MIDIdef(defName).free;
      callback.value(result);
    });
    
    // Timeout
    SystemClock.sched(timeout, {
      if(result.isNil) {
        MIDIdef(defName).free;
        callback.value(nil);
      };
      nil;
    });
    
    ^"Waiting for CC...";
  }
  
  send { |type, channel=0 ...args|
    if(output.isNil) {
      "VibeMIDI: no output connected".warn;
      ^false;
    };
    
    switch(type,
      \noteOn, { output.noteOn(channel, args[0], args[1] ?? 64) },
      \noteOff, { output.noteOff(channel, args[0], args[1] ?? 0) },
      \cc, { output.control(channel, args[0], args[1]) },
      \program, { output.program(channel, args[0]) }
    );
    
    ^true;
  }
  
  enableLog { |enabled=true|
    logEnabled = enabled;
  }
  
  logEvent { |type, note, vel, chan|
    if(logEnabled) {
      eventLog.add((
        type: type,
        note: note,
        vel: vel,
        chan: chan,
        time: Main.elapsedTime
      ));
      if(eventLog.size > 200) { eventLog.removeAt(0) };
    };
  }
  
  getRecent { |count=20, type, since|
    var events = eventLog;
    
    if(type.notNil) {
      events = events.select { |e| e.type == type };
    };
    
    if(since.notNil) {
      var cutoff = Main.elapsedTime - since;
      events = events.select { |e| e.time > cutoff };
    };
    
    ^events.keep(count.neg);
  }
  
  clearMappings {
    MIDIdef.freeAll;
    noteMappings = [];
    ccMappings.clear;
    noteState = Array.fill(128, { nil });
    defCounter = 0;
  }
  
  clearAll {
    this.clearMappings;
    eventLog.clear;
    inputs = [];
    output = nil;
  }
  
  status {
    ^(
      initialized: initialized,
      inputs: inputs.collect(_.device),
      output: output.notNil,
      noteMappings: noteMappings.size,
      ccMappings: ccMappings.keys.asArray
    );
  }
}
```

### Usage

```supercollider
// List devices
~vibe.midi.listDevices;

// Connect
~vibe.midi.connect("KeyStep", \in);
~vibe.midi.connect(0, \out);
~vibe.midi.connectAll;

// Map notes to synth
~vibe.midi.mapNotes(\pad);
~vibe.midi.mapNotes(\lead, mono: true);
~vibe.midi.mapNotes(\bass, channel: 0, velocityToAmp: true);

// Map CC to control bus
~vibe.midi.mapCC(74, \cutoff, [200, 8000], \exp);
~vibe.midi.mapCC(1, \modwheel, [0, 1]);

// Use the bus
Ndef(\synth).map(\cutoff, ~cutoff);

// Learn a CC
~vibe.midi.learn(10, { |result|
  if(result.notNil) {
    "Got CC %".format(result.cc).postln;
  };
});

// Send MIDI out
~vibe.midi.send(\noteOn, 0, 60, 100);
~vibe.midi.send(\cc, 0, 74, 64);

// Event logging
~vibe.midi.enableLog(true);
~vibe.midi.getRecent(10);
~vibe.midi.getRecent(type: \noteOn, since: 5);

// Clear
~vibe.midi.clearMappings;
```

---

## VibeMIDIFile

MIDI file import and playback.

### Class Definition

```supercollider
VibeMIDIFile {
  var <vibe;
  
  *new { |vibe|
    ^super.new.init(vibe);
  }
  
  init { |argVibe|
    vibe = argVibe;
  }
  
  parse { |path|
    // Simple MIDI file parser
    // Returns structured data
    var file, data, tracks;
    
    file = File(path, "rb");
    if(file.isOpen.not) {
      "VibeMIDIFile: could not open '%'".format(path).warn;
      ^nil;
    };
    
    data = Int8Array.newFrom(file.readAllString.ascii);
    file.close;
    
    ^this.parseMIDIData(data);
  }
  
  parseMIDIData { |data|
    // MIDI file format parsing
    // This is a simplified implementation
    var pos = 0;
    var headerChunk, format, numTracks, division;
    var tracks = [];
    var tempo = 120;
    
    // Read header "MThd"
    if(data[0..3].collect(_.asAscii).join != "MThd") {
      "VibeMIDIFile: invalid MIDI file".warn;
      ^nil;
    };
    
    pos = 8;  // skip header chunk length
    format = (data[pos] << 8) | data[pos + 1];
    numTracks = (data[pos + 2] << 8) | data[pos + 3];
    division = (data[pos + 4] << 8) | data[pos + 5];
    pos = pos + 6;
    
    // Parse tracks
    numTracks.do { |trackNum|
      var track, trackLen, trackEnd;
      var events = [];
      var currentTime = 0;
      
      // Find "MTrk"
      while { (pos < data.size) and: { data[pos..pos+3].collect(_.asAscii).join != "MTrk" } } {
        pos = pos + 1;
      };
      
      if(pos >= data.size) {
        "VibeMIDIFile: unexpected end of file".warn;
        ^nil;
      };
      
      pos = pos + 4;
      trackLen = (data[pos] << 24) | (data[pos+1] << 16) | (data[pos+2] << 8) | data[pos+3];
      pos = pos + 4;
      trackEnd = pos + trackLen;
      
      // Parse events
      while { pos < trackEnd } {
        var deltaTime, eventType, event;
        
        // Variable length delta time
        deltaTime = 0;
        while { data[pos] > 127 } {
          deltaTime = (deltaTime << 7) | (data[pos] & 0x7F);
          pos = pos + 1;
        };
        deltaTime = (deltaTime << 7) | data[pos];
        pos = pos + 1;
        
        currentTime = currentTime + deltaTime;
        
        // Event type
        eventType = data[pos];
        
        case
        { (eventType & 0xF0) == 0x90 } {  // Note On
          var chan = eventType & 0x0F;
          var note = data[pos + 1];
          var vel = data[pos + 2];
          if(vel > 0) {
            events = events.add((
              type: \noteOn,
              time: currentTime / division,
              note: note,
              vel: vel,
              chan: chan
            ));
          } {
            events = events.add((
              type: \noteOff,
              time: currentTime / division,
              note: note,
              chan: chan
            ));
          };
          pos = pos + 3;
        }
        { (eventType & 0xF0) == 0x80 } {  // Note Off
          var chan = eventType & 0x0F;
          events = events.add((
            type: \noteOff,
            time: currentTime / division,
            note: data[pos + 1],
            chan: chan
          ));
          pos = pos + 3;
        }
        { eventType == 0xFF } {  // Meta event
          var metaType = data[pos + 1];
          var len = data[pos + 2];
          if(metaType == 0x51) {  // Tempo
            var uspqn = (data[pos+3] << 16) | (data[pos+4] << 8) | data[pos+5];
            tempo = 60000000 / uspqn;
          };
          pos = pos + 3 + len;
        }
        { true } {  // Skip unknown
          pos = pos + 1;
        };
      };
      
      tracks = tracks.add((
        index: trackNum,
        events: events
      ));
    };
    
    ^(
      format: format,
      numTracks: numTracks,
      division: division,
      tempo: tempo,
      tracks: tracks
    );
  }
  
  toPattern { |path, trackIndex=0, instrument=\default|
    var parsed = this.parse(path);
    var track, noteOns, pattern;
    
    if(parsed.isNil) { ^nil };
    
    track = parsed.tracks[trackIndex];
    if(track.isNil) {
      "VibeMIDIFile: track % not found".format(trackIndex).warn;
      ^nil;
    };
    
    noteOns = track.events.select { |e| e.type == \noteOn };
    
    if(noteOns.isEmpty) {
      "VibeMIDIFile: no notes in track %".format(trackIndex).warn;
      ^nil;
    };
    
    // Calculate durations
    noteOns = noteOns.collect { |e, i|
      var dur = if(i < (noteOns.size - 1)) {
        noteOns[i + 1].time - e.time;
      } {
        1;  // default last note duration
      };
      e.put(\dur, dur.max(0.01));
    };
    
    // Build Pbind
    pattern = Pbind(
      \instrument, instrument,
      \midinote, Pseq(noteOns.collect(_.note), 1),
      \dur, Pseq(noteOns.collect(_.dur), 1),
      \amp, Pseq(noteOns.collect { |e| e.vel.linlin(0, 127, 0, 0.8) }, 1)
    );
    
    ^(
      pattern: pattern,
      tempo: parsed.tempo,
      noteCount: noteOns.size
    );
  }
  
  play { |path, instrument=\vibe_default, name, loop=false, trackIndex=0|
    var result = this.toPattern(path, trackIndex, instrument);
    var pdefName;
    
    if(result.isNil) { ^nil };
    
    pdefName = (name ?? PathName(path).fileNameWithoutExtension).asSymbol;
    
    // Set tempo
    vibe.tempo(result.tempo);
    
    // Create and play Pdef
    Pdef(pdefName, if(loop) {
      Pn(result.pattern, inf);
    } {
      result.pattern;
    }).play;
    
    ^(
      name: pdefName,
      tempo: result.tempo,
      notes: result.noteCount,
      loop: loop
    );
  }
  
  tracks { |path|
    var parsed = this.parse(path);
    if(parsed.isNil) { ^nil };
    
    ^parsed.tracks.collect { |t|
      var noteOns = t.events.select { |e| e.type == \noteOn };
      (
        index: t.index,
        noteCount: noteOns.size,
        duration: if(noteOns.notEmpty) {
          noteOns.last.time - noteOns.first.time;
        } { 0 }
      );
    };
  }
}
```

### Usage

```supercollider
// Parse a MIDI file (returns structured data)
~vibe.midiFile.parse("/path/to/file.mid");

// List tracks
~vibe.midiFile.tracks("/path/to/song.mid");

// Convert to pattern
~vibe.midiFile.toPattern("/path/to/bass.mid", 0, \vibe_acid);

// Play directly
~vibe.midiFile.play("/path/to/bass.mid", \vibe_acid);
~vibe.midiFile.play("/path/to/bass.mid", \vibe_acid, loop: true);
~vibe.midiFile.play("/path/to/song.mid", \vibe_pad, trackIndex: 2);
```

---

## VibeState

Session state management for save/restore.

### Class Definition

```supercollider
VibeState {
  var <vibe;
  var <buses;
  
  *new { |vibe|
    ^super.new.init(vibe);
  }
  
  init { |argVibe|
    vibe = argVibe;
    buses = Dictionary[];
  }
  
  bus { |name, numChannels=1, rate=\control|
    var bus = buses[name.asSymbol];
    if(bus.isNil) {
      bus = if(rate == \audio) {
        Bus.audio(vibe.server, numChannels);
      } {
        Bus.control(vibe.server, numChannels);
      };
      buses[name.asSymbol] = bus;
      currentEnvironment[name.asSymbol] = bus;
    };
    ^bus;
  }
  
  setBus { |name, value|
    var bus = buses[name.asSymbol];
    if(bus.notNil) {
      bus.set(value);
      ^true;
    };
    ^false;
  }
  
  clear {
    buses.do(_.free);
    buses.clear;
  }
  
  status {
    ^(
      buses: buses.keys.asArray
    );
  }
}
```

---

# MCP Integration

With the Quark, MCP tools become thin wrappers:

## Example Tool Implementations

```javascript
// src/index.js

// Boot
case "sc_boot":
  await sc.execute(`~vibe = VibeCoding.boot`);
  break;

// Synths  
case "sc_load_synthdef":
  await sc.execute(`~vibe.synths.load(\\${args.name})`);
  break;

// Effects
case "fx_load":
  return await sc.execute(`~vibe.fx.load(\\${args.name})`);

case "fx_chain":
  const effects = args.effects.map(e => 
    e.params ? `\\${e.name} -> [${formatParams(e.params)}]` : `\\${e.name}`
  ).join(', ');
  return await sc.execute(`~vibe.fx.chain(\\${args.name}, [${effects}])`);

case "fx_set":
  return await sc.execute(`~vibe.fx.set(\\${args.slot}, ${formatParams(args.params)})`);

case "fx_route":
  return await sc.execute(`~vibe.fx.route(\\${args.source}, \\${args.target})`);

// MIDI
case "midi_list_devices":
  return await sc.execute(`~vibe.midi.listDevices`);

case "midi_connect":
  return await sc.execute(`~vibe.midi.connect("${args.device}", \\${args.direction})`);

case "midi_map_notes":
  return await sc.execute(`~vibe.midi.mapNotes(\\${args.synthName}, mono: ${args.mono ?? false})`);

case "midi_map_cc":
  return await sc.execute(`~vibe.midi.mapCC(${args.cc}, \\${args.busName}, [${args.range.join(', ')}])`);

// MIDI Files
case "midi_file_import":
  return await sc.execute(`~vibe.midiFile.parse("${args.path}")`);

case "midi_file_play":
  return await sc.execute(`~vibe.midiFile.play("${args.path}", \\vibe_${args.instrument}, loop: ${args.loop ?? false})`);

// Status
case "sc_status":
  return await sc.execute(`~vibe.status`);
```

## Simplified Node.js Layer

```javascript
// src/supercollider.js

class SuperCollider {
  async execute(code) {
    // Just send to sclang, parse result
    // All the complexity is in the Quark now
  }
}
```

---

# Installation

## For Development

```bash
# Clone to extensions directory
cd ~/.local/share/SuperCollider/Extensions
git clone https://github.com/yourname/VibeCoding.git

# Or symlink
ln -s /path/to/VibeCoding ~/.local/share/SuperCollider/Extensions/VibeCoding
```

## Via Quarks

```supercollider
// In SC
Quarks.install("https://github.com/yourname/VibeCoding");
```

## Boot Sequence

```supercollider
// Automatically load at startup (add to startup.scd)
~vibe = VibeCoding.boot;
```

---

# Testing

Test the Quark standalone in SC IDE:

```supercollider
// Boot
~vibe = VibeCoding.boot;

// Test synths
~vibe.synths.play(\kick);
~vibe.synths.play(\acid, \freq, 55, \cutoff, 1000).release(2);

// Test patterns
Pdef(\test, Pbind(
  \instrument, \vibe_kick,
  \dur, 0.5
)).play;

// Test effects
~vibe.fx.load(\reverb);
~vibe.fx.set(\fx_reverb, \mix, 0.6);

// Test MIDI
~vibe.midi.connectAll;
~vibe.midi.mapNotes(\pad);
~vibe.midi.mapCC(74, \cutoff, [200, 8000]);

// Test MIDI file
~vibe.midiFile.play("/path/to/test.mid", \vibe_acid, loop: true);

// Status
~vibe.status;
```

---

# Future Enhancements

- **VibeRecorder**: Record audio output to file
- **VibeClock**: MIDI clock sync in/out
- **VibeSampler**: Sample loading and playback
- **VibeScales**: Scale/chord helpers
- **VibeSequencer**: Step sequencer UI
- **VibeOSC**: OSC message routing for visuals
