// Low Tide - Arrangement (~1:27 at 72 BPM, 26 bars)
// Requires low-tide.scd elements to be defined (but not playing)
// Shape: Emerge -> Develop -> Peak -> Break -> Peak2 -> Dissolve -> Fade

CCArrangement([
    // EMERGE - just the filtered pad breathing
    [\emerge, 4, [\pad]],

    // TEXTURE - wet percussion drifts in, still no beat
    [\texture, 4, [\pad, \hat, \rim]],

    // GROOVE - full beat drops; sub + sidechain pump engage
    [\groove, 6, [\pad, \hat, \rim, \sub, \kick, \snare]],

    // BREAK - strip the drums; tension and breathing room
    [\break, 3, [\pad, \sub, \hat]],

    // RETURN - beat returns full-force
    [\return, 5, [\pad, \hat, \rim, \sub, \kick, \snare]],

    // DISSOLVE - drums fall away, low end lingers
    [\dissolve, 2, [\pad, \sub]],

    // FADE - end on the pad alone; reverb tail rings out
    [\fade, 2, [\pad]],
]).play;
