// CCMIDI - Facade for MIDI functionality in ClaudeCollider

CCMIDI {
  var <cc;
  var <input;      // CCMIDIInput
  var <synths;     // Dictionary: synthName -> CCMIDISynth

  *new { |cc|
    ^super.new.init(cc);
  }

  init { |argCC|
    cc = argCC;
    input = CCMIDIInput();
    synths = Dictionary[];
  }

  // Device connection (delegates to CCMIDIInput)
  listDevices {
    ^input.listDevices;
  }

  connect { |device, direction=\in|
    ^input.connect(device, direction);
  }

  connectAll {
    ^input.connectAll;
  }

  disconnect { |direction=\all|
    ^input.disconnect(direction);
  }

  // Play a synth via MIDI
  // Multiple synths can be active simultaneously
  // ccMappings: Dictionary of ccNum -> (param, range, curve) or just ccNum -> param
  play { |synthName, channel, mono=false, velToAmp=true, ccMappings|
    // Stop existing synth with this name if any
    if(synths[synthName].notNil) { synths[synthName].free };

    // Create new MIDI synth
    var newSynth = CCMIDISynth(synthName, cc.server);
    newSynth.channel = channel;
    newSynth.mono = mono;
    newSynth.velToAmp = velToAmp;

    // Apply CC mappings
    if(ccMappings.notNil) {
      ccMappings.keysValuesDo { |ccNum, mapping|
        if(mapping.isKindOf(Symbol) or: { mapping.isKindOf(String) }) {
          // Simple: ccNum -> param
          newSynth.mapCC(ccNum, mapping);
        } {
          // Full: ccNum -> (param, range, curve)
          newSynth.mapCC(
            ccNum,
            mapping[\param] ?? mapping[0],
            mapping[\range] ?? mapping[1],
            mapping[\curve] ?? mapping[2] ?? \lin
          );
        };
      };
    };

    newSynth.enable;
    synths[synthName] = newSynth;
    ^newSynth;
  }

  // Stop MIDI synth(s) - nil stops all, name stops one
  stop { |synthName|
    if(synthName.notNil) {
      if(synths[synthName].notNil) {
        synths[synthName].free;
        synths.removeAt(synthName);
      };
    } {
      synths.do(_.free);
      synths.clear;
    };
    ^true;
  }

  // Full cleanup
  clear {
    this.stop;
    input.disconnect;
  }

  // Status
  status {
    ^(
      input: input.status,
      synths: if(synths.size > 0) {
        synths.collect(_.status)
      } { nil }
    );
  }
}
