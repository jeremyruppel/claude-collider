// Chopped Break - arrangement (~1:08 at 142 BPM)

CCArrangement([
    [\intro, 8, [\break]],
    [\build, 8, [\break, \bass]],
    [\drop, 8, [\break, \bass, \lead], {
        b.pattern([4, 0, -7, 0, 12, 6, -3, 8, 0, 15, 2, -11, 7, 0, -5, 9], amp: 0.6);
    }],
    [\break, 4, [\lead]],
    [\drop2, 8, [\break, \bass, \lead], {
        b.pattern([0, -8, 0, 6, 3, -14, 7, 0, 11, 2, -5, 0, 15, -3, 9, 4], amp: 0.6);
    }],
    [\outro, 8, [\break], {
        b.pattern((0..15), amp: 0.6);
    }],
]).play;
