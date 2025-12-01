# Live Coding Conventions

Guidelines for writing SuperCollider code that can be updated while playing.

## Formatting

ALWAYS SEND COMMANDS AS A SINGLE LINE

## Core Principle

**Always use proxy objects.** Proxies can be redefined at any time, and the running sound updates seamlessly.

| Proxy Type | Use Case                                  | Update Behavior               |
| ---------- | ----------------------------------------- | ----------------------------- |
| `Pdef`     | Patterns (beats, sequences)               | Crossfades at next loop point |
| `Ndef`     | Continuous synths (drones, pads, effects) | Crossfades over `fadeTime`    |
| `Tdef`     | Tasks (algorithmic sequences)             | Restarts with new definition  |

## Pdef — Pattern Proxies

Use `Pdef` for anything rhythmic or sequenced.

```supercollider
// Define and play
Pdef(\kick, Pbind(
  \instrument, \kick,
  \dur, 1,
  \amp, 0.8
)).play;

// Redefine while playing — waits for current loop to finish
Pdef(\kick, Pbind(
  \instrument, \kick,
  \dur, 0.5,
  \amp, Pseq([0.9, 0.5, 0.7, 0.5], inf)
));

// Stop
Pdef(\kick).stop;

// Resume
Pdef(\kick).resume;

// Clear definition entirely
Pdef(\kick).clear;
```

### Pdef Options

```supercollider
// Quantize changes to bar boundaries — see Quantization section
Pdef(\kick).quant = 4;

// Check if playing
Pdef(\kick).isPlaying;
```

### Layering with Pdef

```supercollider
// Multiple patterns play together
Pdef(\kick, Pbind(\instrument, \kick, \dur, 1)).play;
Pdef(\snare, Pbind(\instrument, \snare, \dur, 2, \phase, 1)).play;
Pdef(\hats, Pbind(\instrument, \hat, \dur, 0.25)).play;

// Stop just the hats
Pdef(\hats).stop;

// Stop everything
Pdef.all.do(_.stop);
```

## Ndef — Node Proxies

Use `Ndef` for continuous sounds: drones, pads, effects, audio processing.

```supercollider
// Define and play
Ndef(\drone, {
  SinOsc.ar([220, 221], 0, 0.3)
}).play;

// Redefine — crossfades to new sound
Ndef(\drone, {
  LPF.ar(Saw.ar([220, 221], 0.3), 800)
});

// Set crossfade time (seconds)
Ndef(\drone).fadeTime = 2;

// Stop (with fade)
Ndef(\drone).stop;

// Clear
Ndef(\drone).clear;
```

### Ndef with Parameters

```supercollider
// Define with arguments
Ndef(\pad, { |freq=220, amp=0.3, cutoff=1000|
  LPF.ar(Saw.ar(freq, amp), cutoff)
}).play;

// Update parameters without redefining
Ndef(\pad).set(\freq, 330, \cutoff, 2000);

// Map parameter to a control bus
~lfo = Bus.control(s, 1);
{ Out.kr(~lfo, SinOsc.kr(0.1).range(400, 2000)) }.play;
Ndef(\pad).map(\cutoff, ~lfo);
```

### Ndef for Effects

```supercollider
// Create an effect that processes other signals
Ndef(\reverb, { |in|
  FreeVerb.ar(In.ar(in, 2), 0.8, 0.9, 0.5)
}).play;

// Route other Ndefs through it
Ndef(\pad).playThrough(\reverb);
```

## Tdef — Task Proxies

Use `Tdef` for algorithmic or generative sequences that aren't simple patterns.

```supercollider
Tdef(\algo, {
  var notes = Scale.minor.degrees + 60;
  loop {
    Synth(\pluck, [\freq, notes.choose.midicps]);
    [0.125, 0.25, 0.5].choose.wait;
  }
}).play;

// Redefine — restarts with new behavior
Tdef(\algo, {
  var notes = Scale.major.degrees + 72;
  loop {
    3.do { |i|
      Synth(\pluck, [\freq, (notes[i]).midicps]);
      0.125.wait;
    };
    0.5.wait;
  }
});
```

## Naming Conventions

Use descriptive, consistent names:

```supercollider
// Drums
Pdef(\kick), Pdef(\snare), Pdef(\hats), Pdef(\perc)

// Melodic
Pdef(\bass), Pdef(\lead), Pdef(\arp), Pdef(\chords)

// Continuous/textural
Ndef(\pad), Ndef(\drone), Ndef(\noise), Ndef(\atmos)

// Effects
Ndef(\reverb), Ndef(\delay), Ndef(\distort), Ndef(\filter)

// Algorithmic
Tdef(\algo), Tdef(\generative), Tdef(\random)
```

## Global Tempo

Use `TempoClock.default` so all patterns sync:

```supercollider
// Set BPM
TempoClock.default.tempo = 120/60;  // 120 BPM

// All Pdefs automatically use default clock
Pdef(\kick, Pbind(\instrument, \kick, \dur, 1)).play;

// Change tempo — everything follows
TempoClock.default.tempo = 140/60;
```

## Quantization

Control when pattern changes take effect using `.quant`.

### Basic Quantization

```supercollider
// Quantize to 4-beat boundaries (1 bar in 4/4)
Pdef(\kick).quant = 4;

// Now any redefinition waits for the next downbeat
Pdef(\kick, Pbind(\instrument, \kick, \dur, 0.5));  // change kicks in on beat 1

// Quantize to 8 beats (2 bars)
Pdef(\kick).quant = 8;

// Immediate update (no quantization)
Pdef(\kick).quant = 0;
```

### Quant with Phase Offset

Use an array `[quant, phase]` to offset when changes land:

```supercollider
// [quantization, phase offset]
Pdef(\snare).quant = [4, 2];  // every 4 beats, but on beat 3 (0-indexed)

// Snare on beats 2 and 4
Pdef(\snare).quant = [2, 1];  // every 2 beats, offset by 1 beat
```

### Full Quant Array

The complete form is `[quant, phase, timing offset, outset]`:

```supercollider
// [quant, phase, timing offset]
Pdef(\hats).quant = [4, 0, 0.05];  // slight timing offset for swing feel

// outset: start before the quant point (for instruments with slow attacks)
Pdef(\pad).quant = [4, 0, -0.1];  // start 0.1 beats early
```

### Global Default

Set a default for all new Pdefs:

```supercollider
// All new Pdefs will quantize to 4 beats
Pdef.defaultQuant = 4;

// These inherit the default
Pdef(\kick, Pbind(...)).play;
Pdef(\bass, Pbind(...)).play;

// Override for specific patterns
Pdef(\fill).quant = 8;  // this one waits for 2-bar boundary
```

### Checking Current Quant

```supercollider
Pdef(\kick).quant;  // returns current setting
```

### Recommended Settings

| Pattern Type        | Quant   | Reason                      |
| ------------------- | ------- | --------------------------- |
| Drums (kick, snare) | 4       | Change on bar boundaries    |
| Hats, percussion    | 1 or 2  | Can change more frequently  |
| Bass                | 4       | Stay locked with kick       |
| Lead/melody         | 4 or 8  | Musical phrase boundaries   |
| Transitions/fills   | 8 or 16 | Wait for section boundaries |
| Immediate FX        | 0       | Instant response            |

### Example: Tight Live Changes

```supercollider
// Setup: everything quantized to bar
Pdef.defaultQuant = 4;
TempoClock.default.tempo = 120/60;

Pdef(\kick, Pbind(\instrument, \kick, \dur, 1)).play;
Pdef(\hats, Pbind(\instrument, \hat, \dur, 0.25)).play;

// User: "double the kick"
// Change queues up, lands perfectly on next bar
Pdef(\kick, Pbind(\instrument, \kick, \dur, 0.5));

// User: "add snare on 2 and 4"
// Also lands on bar boundary
Pdef(\snare, Pbind(
  \instrument, \snare,
  \dur, 2,
  \lag, 1  // offset within pattern
)).play;
```

### Ndef Crossfade (Different Mechanism)

`Ndef` doesn't use quant—it uses `fadeTime` for smooth crossfades:

```supercollider
// Crossfade over 2 seconds
Ndef(\drone).fadeTime = 2;

// Change morphs smoothly
Ndef(\drone, { LPF.ar(Saw.ar([55, 55.1]), 400) });
```

For rhythmic changes with Ndef, wrap it in a Pdef or use `Ndef.reshaping`:

```supercollider
// Elastic reshaping — waits for natural release
Ndef(\stab).reshaping = \elastic;
```

## Control Buses

Use named buses for modulatable parameters:

```supercollider
// Create named control buses
~cutoff = Bus.control(s, 1).set(1000);
~resonance = Bus.control(s, 1).set(0.5);
~tempo = Bus.control(s, 1).set(120);

// Use in synths
Ndef(\bass, {
  RLPF.ar(
    Saw.ar(55),
    In.kr(~cutoff),
    In.kr(~resonance)
  )
}).play;

// Update via bus
~cutoff.set(2000);

// Map MIDI CC to bus (see MIDI section)
MIDIdef.cc(\cutoffCC, { |val|
  ~cutoff.set(val.linexp(0, 127, 200, 8000));
}, ccNum: 74);
```

## Groups and Routing

Organize synths into groups for clean signal flow:

```supercollider
// Create group hierarchy
~srcGroup = Group.new;
~fxGroup = Group.after(~srcGroup);

// Sources play in source group
Pdef(\kick).play(group: ~srcGroup);
Ndef(\bass).play(group: ~srcGroup);

// Effects in fx group (after sources)
Ndef(\reverb).play(group: ~fxGroup);
```

## Quick Reference

### Start/Stop

```supercollider
// Start
Pdef(\x).play;
Ndef(\x).play;
Tdef(\x).play;

// Stop
Pdef(\x).stop;
Ndef(\x).stop;
Tdef(\x).stop;

// Stop all
Pdef.all.do(_.stop);
Ndef.all.do(_.stop);
Tdef.all.do(_.stop);

// Nuclear option — stop everything
CmdPeriod.run;
```

### Check State

```supercollider
Pdef(\x).isPlaying;  // true/false
Ndef(\x).isPlaying;
Pdef.all.select(_.isPlaying);  // all playing Pdefs
```

### List All

```supercollider
Pdef.all.keys;  // all defined pattern names
Ndef.all.keys;  // all defined node names
```

## Anti-Patterns

**Don't do this:**

```supercollider
// BAD: Raw Pbind.play — can't update it
x = Pbind(\instrument, \kick, \dur, 1).play;

// BAD: Raw function.play — can't update it
y = { SinOsc.ar(440, 0, 0.3) }.play;

// BAD: Hardcoded Synth — no proxy
z = Synth(\drone);
```

**Do this instead:**

```supercollider
// GOOD: Pdef — updatable
Pdef(\kick, Pbind(\instrument, \kick, \dur, 1)).play;

// GOOD: Ndef — updatable
Ndef(\sine, { SinOsc.ar(440, 0, 0.3) }).play;

// GOOD: Pdef triggering synths — updatable
Pdef(\drone, Pbind(\instrument, \drone, \dur, inf)).play;
```

## Example Session

```supercollider
// === SETUP ===
TempoClock.default.tempo = 120/60;
~cutoff = Bus.control(s, 1).set(1000);

// === DRUMS ===
Pdef(\kick, Pbind(
  \instrument, \kick,
  \dur, 1,
  \amp, 0.8
)).play;

Pdef(\hats, Pbind(
  \instrument, \hat,
  \dur, 0.25,
  \amp, Pseq([0.3, 0.1, 0.2, 0.1], inf)
)).play;

// === BASS ===
Pdef(\bass, Pbind(
  \instrument, \bass,
  \midinote, Pseq([36, 36, 38, 36], inf),
  \dur, 0.5,
  \cutoff, ~cutoff.asMap
)).play;

// === EVOLVE ===

// "Make it faster"
TempoClock.default.tempo = 140/60;

// "Add snare on 2 and 4"
Pdef(\snare, Pbind(
  \instrument, \snare,
  \dur, 2,
  \phase, 1
)).play;

// "Open up the filter"
~cutoff.set(4000);

// "Make the hats more sparse"
Pdef(\hats, Pbind(
  \instrument, \hat,
  \dur, Pseq([0.25, 0.25, 0.5], inf),
  \amp, 0.2
));

// === BREAKDOWN ===
Pdef(\kick).stop;
Pdef(\snare).stop;
~cutoff.set(500);

// === DROP ===
Pdef(\kick).play;
Pdef(\snare).play;
~cutoff.set(8000);
```

## MCP Integration Notes

When Claude generates code via `sc_execute`:

1. **Always use proxies** — Never raw `.play` on patterns or functions
2. **Use consistent naming** — Same names for same musical roles across updates
3. **Prefer `Pdef` for rhythmic content** — Even single-shot sounds
4. **Set `Pdef.defaultQuant = 4`** — Changes land on bar boundaries
5. **Use control buses for parameters** — Allows MIDI mapping and smooth transitions
6. **Set reasonable `fadeTime` for Ndef** — Prevents jarring transitions

This ensures that follow-up requests like "make it faster" or "add more reverb" work seamlessly without stopping the music, and changes land on musically sensible boundaries.
