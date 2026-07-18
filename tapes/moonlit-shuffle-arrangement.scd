// Moonlit Shuffle - arrangement (~40 bars @ 124 BPM ≈ 1:17)
// Prerequisite: execute moonlit-shuffle.scd first to define all elements + the ~mkBass/~mkChord
// factories and the A/B material. Then run this file to perform the piece.

// A/B swap helpers - hot-swap the Pdef sources at section boundaries
~setA = { Pdef(\bass, ~mkBass.(~bassA)); Pdef(\lead, ~mkChord.(~chordsA_freqs, ~chordsA_durs)); };
~setB = { Pdef(\bass, ~mkBass.(~bassB)); Pdef(\lead, ~mkChord.(~chordsB_freqs, ~chordsB_durs)); };

// Clean slate so the arrangement has full control, then start from the A world
~cc.stop;
~setA.value;

~arr = CCArrangement([
    [\intro,    4, [\kick, \hats]],                                                   // establish the broken beat
    [\groove,   4, [\kick, \hats, \clap]],                                            // backbeat locks in
    [\sectionA, 8, [\kick, \hats, \clap, \bass, \lead, \fill_snare, \fill_tom], ~setA], // full A world
    [\sectionB, 8, [\kick, \hats, \clap, \bass, \lead, \fill_snare, \fill_tom], ~setB], // 4-5-6-5 lift
    [\break,    4, [\hats, \lead], ~setA],                                            // strip down; harmony slips home to A
    [\drop,     8, [\kick, \hats, \clap, \bass, \lead, \fill_snare, \fill_tom]],       // full-energy return in A
    [\outro,    4, [\kick, \hats]],                                                   // strip back to the pulse and out
]).play;
