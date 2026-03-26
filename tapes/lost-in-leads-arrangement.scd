// Lost in Leads - Arrangement (~2:12 at 124 BPM)
// Execute lost-in-leads.scd first (without .play), then run this

CCArrangement([
    // INTRO - kick + hat
    [\intro, 8, [\kick, \hat]],

    // BUILD - +bass +rim
    [\build, 8, [\kick, \hat, \bass, \rim]],

    // DROP 1 - +snare +ohat +pad
    [\drop1, 16, [\kick, \hat, \bass, \rim, \snare, \ohat, \pad]],

    // BREAK - +arp -kick -snare -ohat
    [\break, 8, [\hat, \bass, \rim, \pad, \arp]],

    // DROP 2 - everything
    [\drop2, 16, [\kick, \hat, \bass, \rim, \snare, \ohat, \pad, \arp]],

    // OUTRO - peel away
    [\outro1, 4, [\kick, \hat, \bass, \rim]],
    [\outro2, 4, [\kick, \hat]],
    [\outro3, 2, [\kick]],
]).play;
