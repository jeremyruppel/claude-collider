// CCMIDIInput - MIDI device connection management for ClaudeCollider

CCMIDIInput {
  var <initialized;
  var <inputs;
  var <output;

  *new {
    ^super.new.init;
  }

  init {
    initialized = false;
    inputs = [];
    output = nil;
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
      "CCMIDIInput: device '%' not found".format(device).warn;
      ^false;
    };

    if(direction == \in) {
      MIDIIn.connect(inputs.size, endpoint);
      inputs = inputs.add(endpoint);
      "CCMIDIInput: connected input '%'".format(endpoint.device).postln;
    } {
      var deviceIndex = MIDIClient.destinations.indexOf(endpoint);
      output = MIDIOut(deviceIndex, endpoint.uid);
      "CCMIDIInput: connected output '%' (port %)".format(endpoint.device, deviceIndex).postln;
    };

    ^true;
  }

  connectAll {
    this.initMIDI;
    MIDIIn.connectAll;
    inputs = MIDIClient.sources.copy;
    "CCMIDIInput: connected all inputs".postln;
    ^true;
  }

  disconnect { |direction=\all|
    if(direction == \in or: { direction == \all }) {
      MIDIIn.disconnectAll;
      inputs = [];
    };

    if(direction == \out or: { direction == \all }) {
      output = nil;
    };

    ^true;
  }

  hasInputs {
    ^inputs.size > 0;
  }

  hasOutput {
    ^output.notNil;
  }

  status {
    ^(
      initialized: initialized,
      inputs: inputs.collect(_.device),
      hasOutput: output.notNil
    );
  }
}
