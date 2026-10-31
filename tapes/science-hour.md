---
name: Science Hour
tempo: 112
key: C Lydian
scd: science-hour.scd
---

> **IMPORTANT:** This tape has a companion script at `science-hour.scd`. Execute each block from that file verbatim via `cc_execute` — do not improvise the patterns.

# Science Hour

Retro 1970s educational science TV intro at 112 BPM in C Lydian: a cosmic drone and computer bleeps open into an analog arpeggio, string chords, funky drums, and a hopeful fanfare, ending on a big title-card chord.

## Chord Progression

Cmaj7 | D7 | Am7 | Gmaj7 (4 bars). The D7 (II chord) carries the Lydian F#, which gives the sense of wonder.

## Elements

- **Drums** (cc_kick, cc_snare, cc_hihat) - Light funk groove, kick with a pushed 16th, backbeat snare, accented 16th hats
- **Hats** (cc_hihat) - Hats only, used for the build
- **Bass** (cc_fmbass) - Octave-bouncing 8ths on chord roots
- **Drone** (Ndef) - Low filtered saw bed with a slowly gliding, vibrato "UFO" sine on top
- **Beeps** (cc_sine) - Random high bleeps panned around, like a computer bank
- **Arp** (cc_pluck) - 16th-note up/down arpeggio through each chord, two cycles per bar
- **Pad** (cc_strings) - Chord per bar, soft attack
- **Fanfare** (cc_lead) - Four-note motif C–G–C'–B (rhythm 8th, 8th, quarter, half) moved through each chord: state, +1, −2, −3. Ends on F# over Gmaj7, left unresolved on purpose
- **Stinger** (cc_sine, cc_strings, cc_bell, cc_kick, cc_openhat, cc_fmbass) - Plays once: a two-beat rising bleep run, then a big Cmaj13#11 chord with bells, a kick and cymbal hit, and a low C

## Effects

- **Delay** on beep, fanfare (time 0.402s ≈ a dotted 8th at 112 BPM, feedback 0.35, mix 0.3), feeding into reverb
- **Reverb** on drone, pad, arp, stinger, and the delay output (mix 0.35, room 0.85)

## Notes

This is a one-shot intro, not a loop. The arrangement in `science-hour-arrangement.scd` is what gives it its shape, and a riser is fired from the build section's action. Pdefs are not played in the `.scd`; the arrangement starts them.

Known issue: `~cc.fx.route` can't currently find Ndefs (CCRouter looks up `Ndef.all` by server object instead of server name), so the drone plays dry. Workaround for live playback: `Ndef(\drone).play(out: <fx_reverb bus index>)` after the arrangement starts.

## Playback

Execute each block from `science-hour.scd` verbatim via `cc_execute`. Run blocks in order — do not rewrite or improvise the patterns. Then run `science-hour-arrangement.scd`.
