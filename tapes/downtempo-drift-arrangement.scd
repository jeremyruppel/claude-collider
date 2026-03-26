// Downtempo Drift - Arrangement (~3 min at 108 BPM)
// Requires downtempo-drift.scd elements to be defined (but not playing)

CCArrangement([
    // INTRO - pad + texture
    [\intro, 8, [\pad, \texture]],

    // BASS ENTERS
    [\bass_enters, 8, [\pad, \texture, \bass]],

    // KICK DROPS
    [\kick_drops, 8, [\pad, \texture, \bass, \kick]],

    // HATS + OHAT
    [\hats, 4, [\pad, \texture, \bass, \kick, \hat]],
    [\hats2, 4, [\pad, \texture, \bass, \kick, \hat, \ohat]],

    // FULL GROOVE
    [\full, 16, [\pad, \texture, \bass, \kick, \hat, \ohat, \clap, \stabs]],

    // BREAKDOWN - bass + pad + texture
    [\breakdown, 8, [\pad, \texture, \bass]],

    // REBUILD
    [\rebuild1, 2, [\pad, \texture, \bass, \kick]],
    [\rebuild2, 2, [\pad, \texture, \bass, \kick, \hat]],
    [\rebuild3, 2, [\pad, \texture, \bass, \kick, \hat, \ohat]],
    [\rebuild4, 2, [\pad, \texture, \bass, \kick, \hat, \ohat, \clap, \stabs]],

    // OUTRO
    [\outro1, 2, [\pad, \texture, \bass, \kick, \hat, \ohat]],
    [\outro2, 2, [\pad, \texture, \bass, \kick]],
    [\outro3, 2, [\pad, \texture, \bass]],
    [\outro4, 2, [\pad, \texture]],
]).play;
