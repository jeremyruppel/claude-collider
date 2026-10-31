// CCFade - Master volume fade across one CCArrangement section
//
// Drop it into a section's action slot. It eases the master volume from one
// level to another over the section, using any Env curve (\sin, \lin, \sqr, 2...).
// Levels are 0-1, relative to the master volume when the arrangement started;
// the arrangement puts that volume back when it stops or finishes.
//
//   [\intro, 8, [\kick, \hat], CCFade.in],
//   [\outro, 8, [\kick], CCFade.out(\lin)],
//   [\dip, 4, [\pad], CCFade(1, 0.5)],
//
// Inside another action: { |beat, arr| CCFade.in.value(beat, arr); ... }

CCFade {
  var <from, <to, <curve;

  *new { |from=0, to=1, curve=\sin|
    ^super.newCopyArgs(from, to, curve);
  }

  *in { |curve=\sin|
    ^this.new(0, 1, curve);
  }

  *out { |curve=\sin|
    ^this.new(1, 0, curve);
  }

  // Called by CCArrangement on each beat of the section
  value { |beat, arrangement|
    var numBeats = arrangement.sectionBars * arrangement.beatsPerBar;
    // Aim for where the fade should be at the end of this beat, gliding there over the beat
    var t = ((beat + 1) / numBeats).clip(0, 1);
    arrangement.fade(this.at(t), TempoClock.default.beatDur);
  }

  // Eased level at progress t (0-1)
  at { |t|
    ^Env([from, to], [1], curve).at(t);
  }
}
