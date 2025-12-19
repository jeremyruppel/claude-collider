// CC - ClaudeCollider Main Facade Class
// The main entry point stored in ~cc by convention

CC {
  classvar <instance;

  var <server;
  var <synths;
  var <samples;
  var <fx;
  var <midi;
  var <state;
  var <recorder;
  var <formatter;
  var <isBooted;

  *new { |server, samplesDir, recordingsDir|
    ^super.new.init(server ?? Server.default, samplesDir, recordingsDir);
  }

  *boot { |server, device, numOutputs, onComplete, samplesDir, recordingsDir|
    instance = this.new(server, samplesDir, recordingsDir);
    instance.boot(device, numOutputs, onComplete);
    ^instance;
  }

  init { |argServer, argSamplesDir, argRecordingsDir|
    server = argServer;
    synths = CCSynths(this);
    samples = CCSamples(this, argSamplesDir);
    fx = CCFX(this);
    midi = CCMIDI(this);
    state = CCState(this);
    recorder = CCRecorder(this, argRecordingsDir);
    formatter = CCFormatter(this);
    isBooted = false;
  }

  boot { |device, numOutputs, onComplete|
    this.device(device);
    this.numOutputs(numOutputs);

    server.waitForBoot {
      this.loadSynthDefs;
      this.loadSamples;
      Pdef.defaultQuant = 4;
      isBooted = true;
      "*** ClaudeCollider ready ***".postln;
      onComplete.value(this);
    };
  }

  numOutputs { |num|
    if(num.notNil) {
      server.options.numOutputBusChannels = num;
      "CC: Using % output channels".format(num).postln;
    };
    ^server.options.numOutputBusChannels;
  }

  device { |name|
    if(name.isNil) {
      server.options.device = nil;
      "CC: Using default audio device".postln;
    } {
      server.options.device = name;
      "CC: Using audio device '%'".format(name).postln;
    };
    ^server.options.device;
  }

  reboot { |device, numOutputs, onComplete|
    this.stop;
    server.quit;
    this.boot(device, numOutputs, onComplete);
  }

  loadSynthDefs {
    synths.loadAll;
    fx.loadDefs;
  }

  loadSamples {
    var names = samples.loadAll;
    if(names.size > 0) {
      "SAMPLES_FOUND:%".format(names.join(",")).postln;
    };
    ^names;
  }

  tempo { |bpm|
    if(bpm.notNil) {
      TempoClock.default.tempo = bpm / 60;
      ^bpm;
    } {
      ^TempoClock.default.tempo * 60;
    };
  }

  stop {
    Pdef.all.do(_.stop);
    Ndef.all.do(_.stop);
  }

  clear {
    server.freeAll;
    this.stop;
    Pdef.all.do(_.clear);
    Ndef.all.do(_.clear);
    fx.clearAll;
    midi.clearAll;
    samples.freeAll;
    if(recorder.isRecording) { recorder.stop };
    state.clear;
  }

  status {
    ^formatter.format;
  }
}
