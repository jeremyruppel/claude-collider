// CCArrangement - Declarative song arrangement sequencer
// Define sections as [name, bars, elements, action] arrays (action optional).
// Handles Pdef/Ndef start/stop diffing, beat-quantized timing, and status.
//
// The action runs on every beat of its section as { |beat, arrangement| },
// with beat counting from 0 within the section. Use `if(beat == 0)` for
// things that should happen once when the section starts. A CCFade also
// works as an action, fading the master volume over the section.
//
// The master volume is recorded on play and restored on stop and finish,
// so fades never leave it changed. goto undoes a fade in progress, and on a
// stopped or finished arrangement it starts up the same way play does.
//
// One Routine on TempoClock.default runs the whole timeline. Its waits are in
// logical time, so it doesn't drift, and stop/goto cancel it with routine.stop.

CCArrangement {
  classvar <current;

  var <sections;
  var <beatsPerBar;
  var <sectionIndex;
  var <sectionName;
  var <sectionBars;
  var <isPlaying;
  var <activeElements;
  var routine;      // runs the timeline
  var sectionStart; // clock beat the current section started on
  var <volume;      // master volume in dB when play started; fades are relative to it
  var volumeLag;    // master volume lag when play started

  *new { |sections, beatsPerBar=4|
    ^super.new.init(sections, beatsPerBar);
  }

  init { |argSections, argBeatsPerBar|
    sections = argSections;
    beatsPerBar = argBeatsPerBar;
    isPlaying = false;
    activeElements = Set[];
  }

  play {
    if(isPlaying) { this.stop };
    this.begin;
    this.playFrom(0);
  }

  stop {
    routine.stop;
    CCMIDIClock.stop;
    activeElements.do { |el| this.stopElement(el) };
    activeElements = Set[];
    isPlaying = false;
    this.restoreVolume;
    if(current === this) { current = nil };
    ">>> ARRANGEMENT STOPPED".postln;
  }

  // Set the master volume to level (0-1) of what it was when play started,
  // gliding there over lag seconds. Used by CCFade.
  fade { |level, lag=0.1|
    if(volume.notNil) {
      this.masterVolume.lag = lag;
      this.masterVolume.volume = volume + level.max(0.00001).ampdb;
    };
  }

  goto { |name|
    var targetIndex = sections.detectIndex { |s| s[0] == name };
    if(targetIndex.isNil) {
      "CCArrangement: section '%' not found".format(name).warn;
      ^this;
    };
    if(isPlaying) {
      // Stop current elements but keep arrangement alive, and undo any fade in progress
      routine.stop;
      activeElements.do { |el| this.stopElement(el) };
      activeElements = Set[];
      this.fade(1);
    } {
      // Stopped or finished: start up as play would
      this.begin;
    };
    this.playFrom(targetIndex);
  }

  sectionBar {
    if(isPlaying.not) { ^0 };
    if(sectionStart.isNil) { ^0 };
    ^((TempoClock.default.beats - sectionStart) / beatsPerBar).floor.asInteger + 1;
  }

  status {
    if(isPlaying.not) { ^"Arrangement: stopped" };
    ^"Arrangement: % (bar %/%)".format(sectionName, this.sectionBar, sectionBars);
  }

  // --- internal ---

  // Become the current arrangement and record the master volume to fade from
  begin {
    if(current.notNil and: { current !== this }) { current.stop };
    current = this;
    volume = this.masterVolume.volume;
    volumeLag = this.masterVolume.lag;
  }

  playFrom { |fromIndex|
    isPlaying = true;
    CCMIDIClock.start;

    // Starts on the next bar line
    routine = Routine {
      sections[fromIndex..].do { |section, i|
        var action = section[3];
        var numBeats = section[1] * beatsPerBar;
        var beat = 0;

        this.transitionTo(fromIndex + i);
        while { beat < numBeats } {
          action.value(beat, this);
          min(1, numBeats - beat).wait;
          beat = beat + 1;
        };
      };
      this.finish;
    }.play(TempoClock.default, quant: beatsPerBar);
  }

  transitionTo { |index|
    var section = sections[index];
    var name = section[0];
    var bars = section[1];
    var elements = section[2].asSet;
    var diff = this.diffElements(activeElements, elements);

    sectionIndex = index;
    sectionName = name;
    sectionBars = bars;
    sectionStart = TempoClock.default.beats;

    // Stop removed elements
    diff[\stop].do { |el| this.stopElement(el) };

    // Start new elements
    diff[\start].do { |el| this.startElement(el) };

    activeElements = elements;

    ">>> % (% bars)".format(name.asString.toUpper, bars).postln;
  }

  finish {
    CCMIDIClock.stop;
    activeElements.do { |el| this.stopElement(el) };
    activeElements = Set[];
    sectionName = \done;
    isPlaying = false;
    this.restoreVolume;
    if(current === this) { current = nil };
    ">>> DONE".postln;
  }

  restoreVolume {
    if(volume.notNil) {
      this.masterVolume.lag = volumeLag;
      this.masterVolume.volume = volume;
      volume = nil;
    };
  }

  masterVolume {
    ^(CC.instance !? { |cc| cc.server } ?? { Server.default }).volume;
  }

  diffElements { |currentSet, nextSet|
    ^(
      start: nextSet - currentSet,
      stop: currentSet - nextSet
    );
  }

  // Main mix bus, or hardware bus 0 when there is no CC instance (e.g. in tests)
  mainOut {
    ^CC.instance !? { |cc| cc.fx.router.mainOut } ?? 0;
  }

  startElement { |name|
    if(Pdef.all[name].notNil) {
      Pdef(name).play(quant: beatsPerBar);
    } {
      var proxySpace = Ndef.all[Server.default.name];
      if(proxySpace.notNil and: { proxySpace[name].notNil }) {
        // Keep an Ndef's existing output (e.g. an effect route); otherwise use main
        if(Ndef(name).monitor.notNil) {
          Ndef(name).play;
        } {
          Ndef(name).play(out: this.mainOut);
        };
      } {
        "CCArrangement: element '%' not found".format(name).warn;
      };
    };
  }

  stopElement { |name|
    if(Pdef.all[name].notNil) {
      Pdef(name).stop;
    } {
      var proxySpace = Ndef.all[Server.default.name];
      if(proxySpace.notNil and: { proxySpace[name].notNil }) {
        Ndef(name).stop;
      };
    };
  }
}
