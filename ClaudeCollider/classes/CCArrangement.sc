// CCArrangement - Declarative song arrangement sequencer
// Define sections as [name, bars, elements] arrays.
// Handles Pdef/Ndef start/stop diffing, beat-quantized timing, and status.
//
// Uses TempoClock.schedAbs for drift-free absolute scheduling.
// No Task or Routine — just clock callbacks at exact beat positions.

CCArrangement {
  classvar <current;

  var <sections;
  var <beatsPerBar;
  var <sectionIndex;
  var <sectionName;
  var <sectionBars;
  var <isPlaying;
  var <activeElements;
  var sectionBeats; // precomputed absolute beat positions for each section
  var startBeat;    // absolute beat when arrangement started

  *new { |sections, beatsPerBar=4|
    ^super.new.init(sections, beatsPerBar);
  }

  init { |argSections, argBeatsPerBar|
    sections = argSections;
    beatsPerBar = argBeatsPerBar;
    isPlaying = false;
    activeElements = Set[];
    sectionBeats = [];
  }

  play {
    if(isPlaying) { this.stop };
    if(current.notNil) { current.stop };
    current = this;
    this.playFrom(0);
  }

  stop {
    activeElements.do { |el| this.stopElement(el) };
    activeElements = Set[];
    isPlaying = false;
    if(current === this) { current = nil };
    ">>> ARRANGEMENT STOPPED".postln;
  }

  goto { |name|
    var targetIndex = sections.detectIndex { |s| s[0] == name };
    if(targetIndex.isNil) {
      "CCArrangement: section '%' not found".format(name).warn;
      ^this;
    };
    // Stop current elements but keep arrangement alive
    activeElements.do { |el| this.stopElement(el) };
    activeElements = Set[];
    this.playFrom(targetIndex);
  }

  sectionBar {
    if(isPlaying.not) { ^0 };
    if(sectionIndex.isNil) { ^0 };
    if(sectionIndex >= sectionBeats.size) { ^0 };
    ^((TempoClock.default.beats - sectionBeats[sectionIndex]) / beatsPerBar).floor.asInteger + 1;
  }

  status {
    if(isPlaying.not) { ^"Arrangement: stopped" };
    ^"Arrangement: % (bar %/%)".format(sectionName, this.sectionBar, sectionBars);
  }

  // --- internal ---

  playFrom { |fromIndex|
    var beat;

    isPlaying = true;
    startBeat = TempoClock.default.nextTimeOnGrid(beatsPerBar);

    // Precompute absolute beat positions
    sectionBeats = [];
    beat = startBeat;
    sections[fromIndex..].do { |section|
      sectionBeats = sectionBeats.add(beat);
      beat = beat + (section[1] * beatsPerBar);
    };

    // Schedule each section transition
    sections[fromIndex..].do { |section, i|
      var absoluteIndex = fromIndex + i;

      TempoClock.default.schedAbs(sectionBeats[i], {
        if(isPlaying and: { current === this }) {
          this.transitionTo(absoluteIndex);
        };
        nil; // don't reschedule
      });
    };

    // Schedule end
    TempoClock.default.schedAbs(beat, {
      if(isPlaying and: { current === this }) {
        this.finish;
      };
      nil;
    });
  }

  transitionTo { |index|
    var section = sections[index];
    var name = section[0];
    var bars = section[1];
    var elements = section[2].asSet;
    var action = section[3];
    var diff = this.diffElements(activeElements, elements);

    sectionIndex = index;
    sectionName = name;
    sectionBars = bars;

    // Stop removed elements
    diff[\stop].do { |el| this.stopElement(el) };

    // Start new elements
    diff[\start].do { |el| this.startElement(el) };

    activeElements = elements;

    // Run optional action
    action.value(this);

    ">>> % (% bars)".format(name.asString.toUpper, bars).postln;
  }

  finish {
    activeElements.do { |el| this.stopElement(el) };
    activeElements = Set[];
    sectionName = \done;
    isPlaying = false;
    if(current === this) { current = nil };
    ">>> DONE".postln;
  }

  diffElements { |currentSet, nextSet|
    ^(
      start: nextSet - currentSet,
      stop: currentSet - nextSet
    );
  }

  startElement { |name|
    if(Pdef.all[name].notNil) {
      Pdef(name).play(quant: beatsPerBar);
    } {
      var proxySpace = Ndef.all[Server.default.name];
      if(proxySpace.notNil and: { proxySpace[name].notNil }) {
        Ndef(name).play(0);
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
