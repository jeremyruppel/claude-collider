// Science Hour - Arrangement
// 112 BPM, 22 bars, ~47s (one-shot TV intro, does not loop)

CCArrangement([
    // COSMOS - drone fades in, bleeps from the computer bank
    [\cosmos, 2, [\drone, \beep]],

    // LAB - arp and strings reveal the progression
    [\lab, 4, [\drone, \beep, \arp, \pad]],

    // BUILD - bass and hats enter, riser sweeps up over 4 bars
    [\build, 4, [\beep, \arp, \pad, \bass, \hats], {
        ~cc.synths.play(\cc_riser, \duration, 8.5, \amp, 0.25)
    }],

    // THEME - full band with the fanfare
    [\theme, 8, [\arp, \pad, \bass, \drums, \fanfare]],

    // TITLE CARD - bleep run into the final chord
    [\title, 3, [\stinger]],

    // END - let the reverb ring out
    [\end, 1, []],
]).play;
