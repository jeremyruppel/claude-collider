// CCMIDI - Facade for MIDI functionality in ClaudeCollider

CCMIDI {
  var <cc;
  var <input;      // CCMIDIInput
  var <synth;      // Current CCMIDISynth

  *new { |cc|
    ^super.new.init(cc);
  }

  init { |argCC|
    cc = argCC;
    input = CCMIDIInput();
    synth = nil;
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
  // ccMappings: Dictionary of ccNum -> (param, range, curve) or just ccNum -> param
  play { |synthName, channel, mono=false, velToAmp=true, ccMappings|
    // Stop existing synth if any
    if(synth.notNil) { synth.free };

    // Create new MIDI synth
    synth = CCMIDISynth(synthName, cc.server);
    synth.channel = channel;
    synth.mono = mono;
    synth.velToAmp = velToAmp;

    // Apply CC mappings
    if(ccMappings.notNil) {
      ccMappings.keysValuesDo { |ccNum, mapping|
        if(mapping.isKindOf(Symbol) or: { mapping.isKindOf(String) }) {
          // Simple: ccNum -> param
          synth.mapCC(ccNum, mapping);
        } {
          // Full: ccNum -> (param, range, curve)
          synth.mapCC(
            ccNum,
            mapping[\param] ?? mapping[0],
            mapping[\range] ?? mapping[1],
            mapping[\curve] ?? mapping[2] ?? \lin
          );
        };
      };
    };

    synth.enable;
    ^synth;
  }

  // Stop current MIDI synth
  stop {
    if(synth.notNil) {
      synth.free;
      synth = nil;
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
      synth: if(synth.notNil) { synth.status } { nil }
    );
  }
}
