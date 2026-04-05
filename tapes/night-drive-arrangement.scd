// Night Drive - Arrangement
// 128 BPM, 67 bars, ~2:06

CCArrangement([
    // INTRO - kick and hats emerge
    [\intro, 8, [\kick, \hat]],

    // BUILD 1 - percs layer in
    [\build1, 8, [\kick, \hat, \clap, \rim, \shaker]],

    // DROP 1a - full groove, no melody
    [\drop1a, 8, [\kick, \hat, \clap, \rim, \shaker, \bass, \pad, \tom, \cowbell]],

    // DROP 1b - harmony sneaks in alone
    [\drop1b, 8, [\kick, \hat, \clap, \rim, \shaker, \bass, \pad, \tom, \cowbell, \harmony]],

    // BREAK - harmony floats over stripped-back textures
    [\break, 8, [\rim, \shaker, \pad, \harmony]],

    // BUILD 2 - kick and hats return
    [\build2, 4, [\kick, \hat, \rim, \shaker, \pad]],

    // DROP 2a - lead enters without harmony
    [\drop2a, 8, [\kick, \hat, \clap, \bass, \tom, \cowbell, \lead, \lead_lo]],

    // DROP 2b - peak energy, everything
    [\drop2b, 8, [\kick, \hat, \clap, \bass, \tom, \cowbell, \lead, \lead_lo, \harmony]],

    // OUTRO - elements peel away
    [\outro1, 2, [\kick, \hat, \clap, \rim, \shaker, \bass, \pad, \harmony]],
    [\outro2, 2, [\kick, \hat, \clap, \rim, \shaker]],
    [\outro3, 2, [\kick, \hat]],
    [\outro4, 1, [\kick]],
]).play;
