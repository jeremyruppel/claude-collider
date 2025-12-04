// CCMIDI - MIDI device management and mapping for ClaudeCollider

CCMIDI {
  var <cc;
  var <initialized;
  var <inputs;
  var <output;
  var <noteMappings;
  var <ccMappings;
  var <noteState;
  var <eventLog;
  var <logEnabled;
  var <defCounter;

  *new { |cc|
    ^super.new.init(cc);
  }

  init { |argCC|
    cc = argCC;
    initialized = false;
    inputs = [];
    output = nil;
    noteMappings = [];
    ccMappings = Dictionary[];
    noteState = Array.fill(128, { nil });
    eventLog = List[];
    logEnabled = false;
    defCounter = 0;
  }

  initMIDI {
    if(initialized.not) {
      MIDIClient.init;
      initialized = true;
    };
  }

  listDevices {
    this.initMIDI;
    ^(
      inputs: MIDIClient.sources.collect { |src, i|
        (index: i, device: src.device, name: src.name)
      },
      outputs: MIDIClient.destinations.collect { |dst, i|
        (index: i, device: dst.device, name: dst.name)
      }
    );
  }

  connect { |device, direction=\in|
    var endpoint;

    this.initMIDI;

    // Find by index or name
    endpoint = if(device.isKindOf(Number)) {
      if(direction == \in) {
        MIDIClient.sources[device];
      } {
        MIDIClient.destinations[device];
      };
    } {
      if(direction == \in) {
        MIDIClient.sources.detect { |src|
          src.device.containsi(device.asString) or:
          { src.name.containsi(device.asString) }
        };
      } {
        MIDIClient.destinations.detect { |dst|
          dst.device.containsi(device.asString) or:
          { dst.name.containsi(device.asString) }
        };
      };
    };

    if(endpoint.isNil) {
      "CCMIDI: device '%' not found".format(device).warn;
      ^false;
    };

    if(direction == \in) {
      MIDIIn.connect(inputs.size, endpoint);
      inputs = inputs.add(endpoint);
      "CCMIDI: connected input '%'".format(endpoint.device).postln;
    } {
      output = MIDIOut(0);
      output.connect(endpoint.uid);
      "CCMIDI: connected output '%'".format(endpoint.device).postln;
    };

    ^true;
  }

  connectAll {
    this.initMIDI;
    MIDIIn.connectAll;
    inputs = MIDIClient.sources.copy;
    "CCMIDI: connected all inputs".postln;
  }

  disconnect { |direction=\all, device|
    if(direction == \in or: { direction == \all }) {
      if(device.notNil) {
        // Disconnect specific - would need to track indices
        MIDIIn.disconnectAll;
        inputs = [];
      } {
        MIDIIn.disconnectAll;
        inputs = [];
      };
    };

    if(direction == \out or: { direction == \all }) {
      output = nil;
    };

    ^true;
  }

  mapNotes { |synthName, channel, velocityToAmp=true, mono=false|
    var onName = ("cc_noteOn_" ++ defCounter).asSymbol;
    var offName = ("cc_noteOff_" ++ defCounter).asSymbol;
    var fullSynthName = ("cc_" ++ synthName).asSymbol;
    var chanArg = channel ?? nil;
    var monoSynth = nil;
    var monoNote = nil;

    defCounter = defCounter + 1;

    if(mono) {
      // Monophonic mapping

      MIDIdef.noteOn(onName, { |vel, note, chan|
        if(monoSynth.notNil) { monoSynth.set(\gate, 0) };
        monoSynth = Synth(fullSynthName, [
          \freq, note.midicps,
          \amp, if(velocityToAmp) { vel.linlin(0, 127, 0, 0.8) } { 0.8 },
          \gate, 1
        ]);
        monoNote = note;
        this.logEvent(\noteOn, note, vel, chan);
      }, chan: chanArg);

      MIDIdef.noteOff(offName, { |vel, note, chan|
        if(note == monoNote) {
          if(monoSynth.notNil) { monoSynth.set(\gate, 0) };
          monoSynth = nil;
        };
        this.logEvent(\noteOff, note, vel, chan);
      }, chan: chanArg);
    } {
      // Polyphonic mapping
      MIDIdef.noteOn(onName, { |vel, note, chan|
        noteState[note] = Synth(fullSynthName, [
          \freq, note.midicps,
          \amp, if(velocityToAmp) { vel.linlin(0, 127, 0, 0.8) } { 0.8 },
          \gate, 1
        ]);
        this.logEvent(\noteOn, note, vel, chan);
      }, chan: chanArg);

      MIDIdef.noteOff(offName, { |vel, note, chan|
        if(noteState[note].notNil) { noteState[note].set(\gate, 0) };
        noteState[note] = nil;
        this.logEvent(\noteOff, note, vel, chan);
      }, chan: chanArg);
    };

    noteMappings = noteMappings.add((
      onDef: onName,
      offDef: offName,
      synth: synthName,
      mono: mono
    ));

    ^true;
  }

  mapCC { |ccNum, busName, range=#[0, 1], curve=\lin, channel|
    var defName = ("cc_cc_" ++ busName).asSymbol;
    var bus = currentEnvironment[busName.asSymbol];
    var chanArg = channel ?? nil;

    // Create bus if needed
    if(bus.isNil) {
      bus = Bus.control(cc.server, 1);
      bus.set(range[0] + (range[1] - range[0]) / 2);  // midpoint
      currentEnvironment[busName.asSymbol] = bus;
    };

    MIDIdef.cc(defName, { |val, num, chan|
      var mapped = if(curve == \exp) {
        val.linexp(0, 127, range[0], range[1]);
      } {
        val.linlin(0, 127, range[0], range[1]);
      };
      bus.set(mapped);
      this.logEvent(\cc, num, val, chan);
    }, ccNum: ccNum, chan: chanArg);

    ccMappings[busName.asSymbol] = (
      cc: ccNum,
      bus: bus,
      range: range,
      curve: curve,
      def: defName
    );

    ^bus;
  }

  learn { |timeout=10, callback|
    var defName = \cc_learn;
    var result = nil;

    this.initMIDI;
    this.connectAll;

    MIDIdef.cc(defName, { |val, ccNum, chan|
      result = (cc: ccNum, channel: chan, value: val);
      MIDIdef(defName).free;
      callback.value(result);
    });

    // Timeout
    SystemClock.sched(timeout, {
      if(result.isNil) {
        MIDIdef(defName).free;
        callback.value(nil);
      };
      nil;
    });

    ^"Waiting for CC...";
  }

  send { |type, channel=0 ...args|
    if(output.isNil) {
      "CCMIDI: no output connected".warn;
      ^false;
    };

    switch(type,
      \noteOn, { output.noteOn(channel, args[0], args[1] ?? 64) },
      \noteOff, { output.noteOff(channel, args[0], args[1] ?? 0) },
      \cc, { output.control(channel, args[0], args[1]) },
      \program, { output.program(channel, args[0]) }
    );

    ^true;
  }

  enableLog { |enabled=true|
    logEnabled = enabled;
  }

  logEvent { |type, note, vel, chan|
    if(logEnabled) {
      eventLog.add((
        type: type,
        note: note,
        vel: vel,
        chan: chan,
        time: Main.elapsedTime
      ));
      if(eventLog.size > 200) { eventLog.removeAt(0) };
    };
  }

  getRecent { |count=20, type, since|
    var events = eventLog;
    var cutoff;

    if(type.notNil) {
      events = events.select { |e| e.type == type };
    };

    if(since.notNil) {
      cutoff = Main.elapsedTime - since;
      events = events.select { |e| e.time > cutoff };
    };

    ^events.keep(count.neg);
  }

  clearMappings {
    MIDIdef.freeAll;
    noteMappings = [];
    ccMappings.clear;
    noteState = Array.fill(128, { nil });
    defCounter = 0;
  }

  clearAll {
    this.clearMappings;
    eventLog.clear;
    inputs = [];
    output = nil;
  }

  status {
    ^(
      initialized: initialized,
      inputs: inputs.collect(_.device),
      output: output.notNil,
      noteMappings: noteMappings.size,
      ccMappings: ccMappings.keys.asArray
    );
  }
}
