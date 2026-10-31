/*
# Tension Break

Chopped breakbeat track at 118 BPM in G dorian with pluck melody and sub bass.

## Elements

- **breakStraight** (CCBreakbeat / drum_break_118bpm_4_bar) - Original 4-bar break played in order, used for intro and outro
- **breakChopped** (CCBreakbeat / drum_break_118bpm_4_bar) - Rearranged slices with reversed hits for the main sections
- **melody** (cc_pluck) - 2-bar CCMotif phrase: states twice, transposes up, then retrograde resolution
- **bass** (cc_fmsub) - 2-bar syncopated CCMotif phrase with ghost notes (amp variation), states then transposes down. Short sustain, medium release, FM index 1.5

## Arrangement (32 bars, ~1:05)

1. **Intro** (4 bars) - Straight break only
2. **Build** (4 bars) - Chopped break + sub bass
3. **Drop** (8 bars) - Chopped break + sub bass + melody
4. **Break** (4 bars) - Melody solo
5. **Drop 2** (8 bars) - Full: chopped break + sub bass + melody
6. **Outro** (4 bars) - Straight break only

## Notes

- Both breakbeat patterns use the repo sample `drum_break_118bpm_4_bar` sliced into 16 segments
- No effects or sidechain — raw and direct
*/

// 1. Clear and set tempo
~cc.clear;
~cc.tempo(118);

// 2. Load break sample
~cc.samples.load(\drum_break_118bpm_4_bar);

// 3. Breakbeat (straight) - original slice order
~bbStraight = CCBreakbeat(\breakStraight, ~cc.samples.at(\drum_break_118bpm_4_bar), 16).bars(4);
~bbStraight.pattern([0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15]);

// 4. Breakbeat (chopped) - rearranged with reversed slices
~bbChop = CCBreakbeat(\breakChopped, ~cc.samples.at(\drum_break_118bpm_4_bar), 16).bars(4);
~bbChop.pattern([0, 0, 3, 2, 7, 6, 5, 4, 4, 5, -10, 11, 12, 13, -14, 15]);

// 5. Melody - pluck in G dorian, 2-bar CCMotif phrase
(
m = CCMotif([0, 2, 4, Rest()]);

Pdef(\melody, Pbind(
	\instrument, \cc_pluck,
	\scale, Scale.dorian,
	\root, 7,
	\octave, 5,
	\degree, CCPhrase(m, [
		\state,
		\state,
		\transpose, 2,
		\retrograde
	]),
	\dur, 0.5,
	\amp, 0.4,
	\decay, 1.5,
	\color, 0.6
));
);

// 6. Bass - FM sub in G dorian, 2-bar syncopated CCMotif phrase with ghost notes
(
b = CCMotif([0, Rest(), 4, 0, 2, Rest()]);

Pdef(\bass, Pbind(
	\instrument, \cc_fmsub,
	\scale, Scale.dorian,
	\root, 7,
	\octave, 3,
	\degree, CCPhrase(b, [
		\state,
		\transpose, -1
	]),
	\dur, Pseq([0.25, 0.25, 0.5, 0.25, 0.5, 0.25], inf),
	\amp, Pseq([0.5, 0, 0.4, 0.2, 0.5, 0], inf),
	\index, 1.5,
	\sustain, 0.15,
	\release, 0.8
));
);

// 7. Arrangement - play with ~arrangement.play
~arrangement = CCArrangement([
	[\intro, 4, [\breakStraight]],
	[\build, 4, [\breakChopped, \bass]],
	[\drop, 8, [\breakChopped, \bass, \melody]],
	[\break, 4, [\melody]],
	[\drop2, 8, [\breakChopped, \bass, \melody]],
	[\outro, 4, [\breakStraight]],
]);
