// CCMIDISynth - MIDI-controlled synth with CC parameter mapping

CCMIDISynth {
  var <manager;      // CCSynthManager
  var <channel;      // MIDI channel filter (nil = all)
  var <velToAmp;     // Map velocity to amplitude
  var <enabled;

  // CC state
  var <ccMappings;   // Dictionary: ccNum -> (param, range, curve)
  var <ccValues;     // Dictionary: param -> current value

  // MIDIdefs
  var <noteOnDef;
  var <noteOffDef;
  var <ccDefs;       // Dictionary: ccNum -> MIDIdef
  var <defId;

  classvar <>idCounter = 0;

  *new { |synthName, server|
    ^super.new.init(synthName, server);
  }

  init { |synthName, server|
    manager = CCSynthManager(synthName, server);
    channel = nil;
    velToAmp = true;
    enabled = false;

    ccMappings = Dictionary[];
    ccValues = Dictionary[];
    ccDefs = Dictionary[];

    idCounter = idCounter + 1;
    defId = idCounter;
  }

  // Forward to manager
  synthName { ^manager.synthName }
  synthName_ { |name| manager.synthName = name }
  mono { ^manager.mono }
  mono_ { |m| manager.mono = m }

  // CC Mapping
  mapCC { |ccNum, param, range, curve=\lin|
    var actualRange = range ?? [0, 1];

    ccMappings[ccNum] = (
      param: param.asSymbol,
      range: actualRange,
      curve: curve
    );

    // Initialize to midpoint
    ccValues[param.asSymbol] = actualRange[0] + (actualRange[1] - actualRange[0] / 2);

    if(enabled) { this.createCCDef(ccNum) };
  }

  unmapCC { |ccNum|
    if(ccDefs[ccNum].notNil) {
      ccDefs[ccNum].free;
      ccDefs.removeAt(ccNum);
    };
    ccMappings.removeAt(ccNum);
  }

  clearCCMappings {
    ccDefs.do(_.free);
    ccDefs.clear;
    ccMappings.clear;
    ccValues.clear;
  }

  // Enable/disable
  enable {
    if(enabled) { ^this };

    this.createNoteDefs;
    ccMappings.keysDo { |ccNum| this.createCCDef(ccNum) };

    enabled = true;
    "CCMIDISynth: enabled '%' (%)".format(manager.synthName, if(manager.mono, "mono", "poly")).postln;
  }

  disable {
    if(enabled.not) { ^this };

    manager.releaseAll;

    if(noteOnDef.notNil) { noteOnDef.free; noteOnDef = nil };
    if(noteOffDef.notNil) { noteOffDef.free; noteOffDef = nil };
    ccDefs.do(_.free);
    ccDefs.clear;

    enabled = false;
    "CCMIDISynth: disabled '%'".format(manager.synthName).postln;
  }

  free {
    this.disable;
    this.clearCCMappings;
  }

  // Private: create note MIDIdefs
  createNoteDefs {
    noteOnDef = MIDIdef.noteOn(this.noteOnDefName, { |vel, note, chan|
      var amp = if(velToAmp) { vel.linlin(0, 127, 0, 0.8) } { 0.8 };
      var args = [\amp, amp, \gate, 1];

      // Add current CC values
      ccValues.keysValuesDo { |param, val|
        args = args ++ [param, val];
      };

      manager.noteOn(note, args);
    }, chan: channel);

    noteOffDef = MIDIdef.noteOff(this.noteOffDefName, { |vel, note, chan|
      manager.noteOff(note);
    }, chan: channel);
  }

  // Private: create CC MIDIdef
  createCCDef { |ccNum|
    var mapping = ccMappings[ccNum];
    if(mapping.isNil) { ^this };

    ccDefs[ccNum] = MIDIdef.cc(this.ccDefName(ccNum), { |val, num, chan|
      var mapped = if(mapping.curve == \exp) {
        val.linexp(0, 127, mapping.range[0], mapping.range[1]);
      } {
        val.linlin(0, 127, mapping.range[0], mapping.range[1]);
      };

      ccValues[mapping.param] = mapped;
      manager.set(mapping.param, mapped);
    }, ccNum: ccNum, chan: channel);
  }

  // Unique def names
  noteOnDefName { ^("ccms_on_" ++ defId).asSymbol }
  noteOffDefName { ^("ccms_off_" ++ defId).asSymbol }
  ccDefName { |ccNum| ^("ccms_cc_" ++ defId ++ "_" ++ ccNum).asSymbol }

  status {
    ^(
      synth: manager.synthName,
      channel: channel ?? "all",
      mono: manager.mono,
      enabled: enabled,
      ccMappings: ccMappings.collect { |m| m.param },
      activeNotes: manager.activeNotes
    );
  }

  printOn { |stream|
    stream << "CCMIDISynth('" << manager.synthName << "')";
  }
}
