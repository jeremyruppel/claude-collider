// Night Drive - Arrangement
// 128 BPM, 68 bars, ~2:08

CCArrangement([
    // INTRO - kick and hats emerge
    [\intro, 8, [\kick, \hat]],

    // BUILD 1 - percs layer in
    [\build1, 8, [\kick, \hat, \clap, \rim, \shaker]],

    // DROP 1 - full groove, no lead
    [\drop1, 16, [\kick, \hat, \clap, \rim, \shaker, \bass, \pad, \tom, \cowbell]],

    // BREAK - strip down, tension
    [\break, 8, [\rim, \shaker, \pad]],

    // BUILD 2 - kick and hats return
    [\build2, 4, [\kick, \hat, \rim, \shaker, \pad]],

    // DROP 2 - peak energy with leads
    [\drop2, 16, [\kick, \hat, \clap, \bass, \tom, \cowbell, \lead, \lead_lo]],

    // OUTRO - elements peel away
    [\outro1, 2, [\kick, \hat, \clap, \rim, \shaker, \bass, \pad]],
    [\outro2, 2, [\kick, \hat, \clap, \rim, \shaker]],
    [\outro3, 2, [\kick, \hat]],
    [\outro4, 1, [\kick]],
]).play;
