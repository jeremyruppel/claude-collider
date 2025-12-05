# ClaudeCollider SynthDef Expansion — Tech Spec

## Overview

Add new SynthDefs to ClaudeCollider's built-in library. These should follow existing conventions and be optimized for live coding: sound great with defaults, respond well to real-time parameter changes.

## Conventions

### Naming
- All SynthDefs prefixed with `cc_` (e.g., `\cc_pluck`, `\cc_tom`)
- Register in `~cc.synths.defs` dictionary with description and params

### Standard Parameters
All synths MUST include:
- `out` (default: 0) — output bus
- `amp` (default: 0.3-0.5) — amplitude

Melodic/pitched synths MUST include:
- `freq` (default: 440) — frequency in Hz
- `gate` (default: 1) — for envelopes that need release

### Output
- All synths output STEREO (2 channels)
- Use `Out.ar(out, sig ! 2)` or `Out.ar(out, Pan2.ar(sig, pan))`
- Use `doneAction: 2` on envelopes to free synths

### Sound Design Goals
- Should sound good immediately with no params
- 2-5 tweakable params that make meaningful tonal changes
- Avoid params that break the sound if set wrong
- CPU efficient — these may run many instances

---

## Drums & Percussion

### cc_openhat
Open hi-hat to complement existing `cc_hihat`.

| Param | Default | Range | Description |
|-------|---------|-------|-------------|
| out | 0 | — | output bus |
| amp | 0.3 | 0-1 | amplitude |
| decay | 0.5 | 0.2-1.5 | envelope decay time |

**Implementation notes:**
- Filtered noise (HPF + BPF for metallic tone)
- Longer decay than closed hat
- Optional: slight pitch envelope down

### cc_tom
Tunable tom drum for fills.

| Param | Default | Range | Description |
|-------|---------|-------|-------------|
| out | 0 | — | output bus |
| freq | 120 | 60-300 | fundamental pitch |
| amp | 0.5 | 0-1 | amplitude |
| decay | 0.3 | 0.1-0.8 | envelope decay |

**Implementation notes:**
- Sine wave with pitch envelope (start high, drop to `freq`)
- Mix in some noise for attack
- Good for: `\freq, 100` (low), `\freq, 150` (mid), `\freq, 200` (high)

### cc_rim
Rimshot / sidestick.

| Param | Default | Range | Description |
|-------|---------|-------|-------------|
| out | 0 | — | output bus |
| amp | 0.4 | 0-1 | amplitude |
| freq | 1700 | 1000-3000 | resonant frequency |

**Implementation notes:**
- Very short noise burst + resonant bandpass
- Tight, clicky attack
- Think 808 rimshot

### cc_shaker
Shaker / maraca for rhythmic texture.

| Param | Default | Range | Description |
|-------|---------|-------|-------------|
| out | 0 | — | output bus |
| amp | 0.2 | 0-1 | amplitude |
| decay | 0.08 | 0.03-0.2 | grain decay |
| color | 5000 | 2000-10000 | filter frequency |

**Implementation notes:**
- Filtered noise bursts
- Very short decay by default (for 16ths)
- `color` controls brightness via HPF or BPF

### cc_cowbell
The essential cowbell.

| Param | Default | Range | Description |
|-------|---------|-------|-------------|
| out | 0 | — | output bus |
| amp | 0.4 | 0-1 | amplitude |
| decay | 0.4 | 0.2-1 | envelope decay |

**Implementation notes:**
- Two detuned square waves (e.g., 560 Hz and 845 Hz)
- Bandpass filtered
- Classic 808-style

---

## Bass

### cc_sub
Pure sub bass for layering.

| Param | Default | Range | Description |
|-------|---------|-------|-------------|
| out | 0 | — | output bus |
| freq | 55 | 30-100 | frequency |
| amp | 0.5 | 0-1 | amplitude |
| decay | 1 | 0.3-4 | envelope time |
| gate | 1 | — | gate for envelope |

**Implementation notes:**
- Pure sine wave, maybe slight saturation option
- Smooth envelope (no click)
- Designed to layer under kicks or other bass

### cc_reese
Detuned saw bass, classic DnB/dubstep tone.

| Param | Default | Range | Description |
|-------|---------|-------|-------------|
| out | 0 | — | output bus |
| freq | 55 | 30-200 | frequency |
| amp | 0.4 | 0-1 | amplitude |
| detune | 0.5 | 0-2 | detune amount in Hz |
| cutoff | 800 | 100-4000 | filter cutoff |
| gate | 1 | — | gate |

**Implementation notes:**
- 2-3 detuned sawtooth waves
- Low-pass filter with slight resonance
- Phasing/movement from detune

### cc_fmbass
FM bass for growly/aggressive tones.

| Param | Default | Range | Description |
|-------|---------|-------|-------------|
| out | 0 | — | output bus |
| freq | 55 | 30-200 | carrier frequency |
| amp | 0.4 | 0-1 | amplitude |
| index | 3 | 0.5-10 | FM modulation index |
| ratio | 1 | 0.5-4 | modulator ratio |
| gate | 1 | — | gate |

**Implementation notes:**
- Simple 2-op FM (carrier + modulator)
- `index` controls growl/harmonic content
- `ratio` changes timbre character

---

## Melodic

### cc_pluck
Karplus-Strong plucked string.

| Param | Default | Range | Description |
|-------|---------|-------|-------------|
| out | 0 | — | output bus |
| freq | 440 | 100-4000 | pitch |
| amp | 0.4 | 0-1 | amplitude |
| decay | 2 | 0.5-10 | ring time |
| color | 0.5 | 0-1 | brightness (coef) |

**Implementation notes:**
- Use `Pluck` UGen or implement with `CombL` + noise burst
- `color` maps to damping coefficient
- Good for guitar, harp, kalimba sounds

### cc_bell
FM bell / glassy tone.

| Param | Default | Range | Description |
|-------|---------|-------|-------------|
| out | 0 | — | output bus |
| freq | 440 | 200-2000 | pitch |
| amp | 0.3 | 0-1 | amplitude |
| decay | 4 | 1-10 | ring time |
| brightness | 3 | 1-8 | harmonic content |

**Implementation notes:**
- FM synthesis with inharmonic ratios (e.g., 1:2.4, 1:3.1)
- `brightness` controls modulation index
- Long decay, shimmery

### cc_keys
Electric piano / Rhodes-ish.

| Param | Default | Range | Description |
|-------|---------|-------|-------------|
| out | 0 | — | output bus |
| freq | 440 | 100-2000 | pitch |
| amp | 0.4 | 0-1 | amplitude |
| attack | 0.01 | 0.001-0.1 | attack time |
| release | 1 | 0.3-4 | release time |
| brightness | 0.5 | 0-1 | tone |
| gate | 1 | — | gate |

**Implementation notes:**
- FM with velocity-sensitive brightness
- Slightly asymmetric waveform for warmth
- Classic EP bark on hard hits

### cc_strings
Simple string ensemble pad.

| Param | Default | Range | Description |
|-------|---------|-------|-------------|
| out | 0 | — | output bus |
| freq | 440 | 100-2000 | pitch |
| amp | 0.3 | 0-1 | amplitude |
| attack | 0.5 | 0.1-2 | attack time |
| release | 1 | 0.3-4 | release time |
| detune | 0.3 | 0-1 | ensemble width |
| gate | 1 | — | gate |

**Implementation notes:**
- Multiple detuned saws (3-5 voices)
- LPF to tame brightness
- Slow attack by default

---

## Textural

### cc_noise
Filtered noise source.

| Param | Default | Range | Description |
|-------|---------|-------|-------------|
| out | 0 | — | output bus |
| amp | 0.3 | 0-1 | amplitude |
| cutoff | 2000 | 100-15000 | filter cutoff |
| res | 0.3 | 0-0.95 | filter resonance |
| type | 0 | 0,1,2 | LP, HP, BP |
| gate | 1 | — | gate |

**Implementation notes:**
- White or pink noise source
- Switchable filter type
- Useful for sweeps, textures, layering

### cc_drone
Evolving ambient texture.

| Param | Default | Range | Description |
|-------|---------|-------|-------------|
| out | 0 | — | output bus |
| freq | 55 | 30-500 | root frequency |
| amp | 0.2 | 0-1 | amplitude |
| spread | 0.5 | 0-1 | harmonic spread |
| movement | 0.1 | 0.01-1 | modulation speed |
| gate | 1 | — | gate |

**Implementation notes:**
- Multiple sine/saw oscillators at harmonic intervals
- Slow LFOs modulating pitch, filter, amplitude
- Should evolve over time without being rhythmic

### cc_riser
Tension building riser/sweep.

| Param | Default | Range | Description |
|-------|---------|-------|-------------|
| out | 0 | — | output bus |
| amp | 0.4 | 0-1 | amplitude |
| duration | 4 | 1-16 | rise time in beats |
| startFreq | 200 | 100-1000 | starting frequency |
| endFreq | 4000 | 1000-12000 | ending frequency |

**Implementation notes:**
- Noise + oscillator sweep upward
- Auto-frees after duration
- Optional: add white noise crescendo

---

## Utility

### cc_click
Metronome click.

| Param | Default | Range | Description |
|-------|---------|-------|-------------|
| out | 0 | — | output bus |
| amp | 0.3 | 0-1 | amplitude |
| freq | 1500 | 800-4000 | pitch |

**Implementation notes:**
- Very short sine or filtered click
- Useful for timing/practice
- Should cut through mix without being harsh

### cc_sine
Pure sine tone.

| Param | Default | Range | Description |
|-------|---------|-------|-------------|
| out | 0 | — | output bus |
| freq | 440 | 20-8000 | frequency |
| amp | 0.3 | 0-1 | amplitude |
| gate | 1 | — | gate |

**Implementation notes:**
- Just a sine wave with envelope
- Testing, layering, sub reinforcement

### cc_sampler
Basic sample playback.

| Param | Default | Range | Description |
|-------|---------|-------|-------------|
| out | 0 | — | output bus |
| buf | 0 | — | buffer number |
| amp | 0.5 | 0-1 | amplitude |
| rate | 1 | 0.25-4 | playback rate |
| start | 0 | 0-1 | start position (0-1) |

**Implementation notes:**
- `PlayBuf` based
- Rate can be negative for reverse
- Auto-frees when done

---

## Sample Management API

### Overview

Samples live in `~cc.samples` — a helper object for loading, organizing, and playing audio files.

### Loading Samples

```supercollider
// Load a single sample (async, returns immediately)
~cc.samples.load("/path/to/kick.wav", \kick);

// Load with callback
~cc.samples.load("/path/to/kick.wav", \kick, { |buf|
    "Loaded kick, % frames".format(buf.numFrames).postln;
});

// Load a folder of samples
~cc.samples.loadFolder("/path/to/drums/", \drums);
// Creates: ~cc.samples[\drums][\kick], ~cc.samples[\drums][\snare], etc.
// (uses filenames without extension as keys)

// Load with dialog (interactive)
~cc.samples.loadDialog(\mysample);
```

### Accessing Samples

```supercollider
// Get a buffer by name
~cc.samples[\kick]

// Get from a folder group
~cc.samples[\drums][\kick]

// Shorthand for use in Pbind
Pbind(\instrument, \cc_sampler, \buf, ~cc.samples[\kick], \dur, 1)

// List all loaded samples
~cc.samples.list
// -> [ kick, snare, drums/kick, drums/snare, drums/hat ]

// Check if loaded
~cc.samples.exists(\kick)  // -> true/false
```

### Sample Info

```supercollider
// Get sample info
~cc.samples.info(\kick)
// -> (duration: 0.45, frames: 19845, channels: 2, sampleRate: 44100)

// Duration in beats (uses current tempo)
~cc.samples.beats(\kick)
// -> 1.5
```

### Sample Playback Helpers

```supercollider
// One-shot play (fire and forget)
~cc.samples.play(\kick);
~cc.samples.play(\kick, rate: 0.5, amp: 0.8);

// Play reversed
~cc.samples.play(\kick, rate: -1);

// Play a random sample from a folder
~cc.samples.playRand(\drums);
```

### Slicing & Chopping

```supercollider
// Divide a sample into N equal slices
~cc.samples.slice(\breakbeat, 16);
// Creates: ~cc.samples[\breakbeat_0], ~cc.samples[\breakbeat_1], etc.

// Play slice by index
Pbind(
    \instrument, \cc_sampler,
    \buf, ~cc.samples[\breakbeat],
    \start, Pseq((0..15) / 16, inf),  // step through slices
    \dur, 0.25
)

// Auto-detect transients and slice
~cc.samples.autoSlice(\breakbeat, threshold: 0.3);
```

### Buffer Management

```supercollider
// Free a sample
~cc.samples.free(\kick);

// Free all samples
~cc.samples.freeAll;

// Total memory used
~cc.samples.memUsed
// -> "23.4 MB"
```

### Pbind Integration

The `cc_sampler` synth should work seamlessly with standard Pbind keys:

```supercollider
Pdef(\breaks, Pbind(
    \instrument, \cc_sampler,
    \buf, ~cc.samples[\breakbeat],
    
    // These should "just work"
    \dur, 0.25,
    \amp, Pseq([0.8, 0.4, 0.6, 0.4], inf),
    
    // Sample-specific
    \rate, Pwhite(0.9, 1.1),      // slight pitch variation
    \start, Pwhite(0, 0.5),       // random start position
    \loop, 0,                      // 0 = one-shot, 1 = loop
    \attack, 0.01,                 // fade in
    \release, 0.1,                 // fade out
)).play;
```

### cc_sampler (Updated)

| Param | Default | Range | Description |
|-------|---------|-------|-------------|
| out | 0 | — | output bus |
| buf | 0 | — | buffer number |
| amp | 0.5 | 0-1 | amplitude |
| rate | 1 | -4 to 4 | playback rate (negative = reverse) |
| start | 0 | 0-1 | start position (0-1 normalized) |
| loop | 0 | 0,1 | loop mode |
| attack | 0.001 | 0-1 | fade in time |
| release | 0.01 | 0-1 | fade out time |
| pan | 0 | -1 to 1 | stereo position |

**Implementation notes:**
- Use `PlayBuf` for one-shot, `LoopBuf` or `BufRd` for looping
- `rate` should be multiplied by `BufRateScale.kr(buf)` to maintain pitch
- Handle mono vs stereo buffers gracefully
- `doneAction: 2` for one-shot, `doneAction: 0` for loops
- Start position: `startPos = start * BufFrames.kr(buf)`

### cc_grains (Bonus: Granular Sampler)

For more experimental sample mangling:

| Param | Default | Range | Description |
|-------|---------|-------|-------------|
| out | 0 | — | output bus |
| buf | 0 | — | buffer number |
| amp | 0.3 | 0-1 | amplitude |
| pos | 0.5 | 0-1 | playback position |
| posSpeed | 0 | -2 to 2 | position movement speed |
| grainSize | 0.1 | 0.01-0.5 | grain duration |
| grainRate | 20 | 1-100 | grains per second |
| pitch | 1 | 0.25-4 | pitch multiplier |
| spread | 0.5 | 0-1 | stereo spread |
| gate | 1 | — | gate |

**Implementation notes:**
- Use `GrainBuf` or `TGrains`
- `pos` can be modulated for scrubbing/scanning
- `posSpeed = 1` plays forward at normal speed
- Great for pads, textures, glitchy stuff

---

## Registration Format

Each synth should be registered like:

```supercollider
~cc.synths.add(\pluck, (
    desc: "Karplus-Strong plucked string",
    params: [\out, 0, \freq, 440, \amp, 0.4, \decay, 2, \color, 0.5],
    func: { SynthDef(\cc_pluck, { |out=0, freq=440, amp=0.4, decay=2, color=0.5|
        // implementation
    }).add }
));
```
