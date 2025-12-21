// CCSynthManager - Voice allocation and synth lifecycle management

CCSynthManager {
  var <server;
  var <synthName;
  var <mono;

  // Voice state
  var <noteState;   // Array[128] of Synths for poly
  var <monoSynth;   // Current synth for mono
  var <monoNote;    // Current note for mono

  *new { |synthName, server|
    ^super.new.init(synthName, server);
  }

  init { |argSynthName, argServer|
    server = argServer ?? Server.default;
    synthName = argSynthName;
    mono = false;
    noteState = Array.fill(128, { nil });
    monoSynth = nil;
    monoNote = nil;
  }

  synthName_ { |name|
    synthName = name;
  }

  mono_ { |isMono|
    if(mono != isMono) {
      this.releaseAll;
      mono = isMono;
    };
  }

  // Create a synth for the given note
  noteOn { |note, args|
    var fullName = ("cc_" ++ synthName).asSymbol;
    var synth;

    args = args ?? [];
    args = [\freq, note.midicps] ++ args;

    if(mono) {
      if(monoSynth.notNil) { monoSynth.set(\gate, 0) };
      monoSynth = Synth(fullName, args);
      monoNote = note;
      ^monoSynth;
    } {
      synth = Synth(fullName, args);
      noteState[note] = synth;
      ^synth;
    };
  }

  // Release synth for the given note
  noteOff { |note|
    if(mono) {
      if(note == monoNote and: { monoSynth.notNil }) {
        monoSynth.set(\gate, 0);
        monoSynth = nil;
        monoNote = nil;
      };
    } {
      if(noteState[note].notNil) {
        noteState[note].set(\gate, 0);
        noteState[note] = nil;
      };
    };
  }

  // Update a param on all active synths
  set { |param, value|
    if(mono) {
      if(monoSynth.notNil) {
        monoSynth.set(param, value);
      };
    } {
      noteState.do { |synth|
        if(synth.notNil) {
          synth.set(param, value);
        };
      };
    };
  }

  // Release all active synths
  releaseAll {
    if(mono) {
      if(monoSynth.notNil) {
        monoSynth.set(\gate, 0);
        monoSynth = nil;
        monoNote = nil;
      };
    } {
      noteState.do { |synth|
        if(synth.notNil) { synth.set(\gate, 0) };
      };
      noteState = Array.fill(128, { nil });
    };
  }

  // Query active notes
  activeNotes {
    if(mono) {
      ^if(monoNote.notNil) { [monoNote] } { [] };
    } {
      ^noteState.selectIndices { |s| s.notNil };
    };
  }

  hasActiveNotes {
    ^this.activeNotes.size > 0;
  }

  status {
    ^(
      synth: synthName,
      mono: mono,
      activeNotes: this.activeNotes
    );
  }

  printOn { |stream|
    stream << "CCSynthManager('" << synthName << "')";
  }
}
