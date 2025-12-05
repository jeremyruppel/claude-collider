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
  var <isBooted;

  *new { |server|
    ^super.new.init(server ?? Server.default);
  }

  *boot { |server, device, onComplete|
    instance = this.new(server);
    instance.boot(device, onComplete);
    ^instance;
  }

  init { |argServer|
    server = argServer;
    synths = CCSynths(this);
    samples = CCSamples(this);
    fx = CCFX(this);
    midi = CCMIDI(this);
    state = CCState(this);
    recorder = CCRecorder(this);
    isBooted = false;
  }

  boot { |device, onComplete|
    this.device(device);

    server.waitForBoot {
      this.loadSynthDefs;
      this.loadSamples;
      Pdef.defaultQuant = 4;
      isBooted = true;
      "*** ClaudeCollider ready ***".postln;
      onComplete.value(this);
    };
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

  reboot { |device, onComplete|
    this.stop;
    server.quit;
    this.boot(device, onComplete);
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
    var playingPdefs = Pdef.all.select(_.isPlaying).keys.asArray;
    var playingNdefs = Ndef.all.select(_.isPlaying).keys.asArray;
    var loadedSamples = samples.buffers.size;
    var totalSamples = samples.paths.size;
    var lines = [
      "Server: % | CPU: %% | Synths: %".format(
        if(server.serverRunning) { "running" } { "stopped" },
        server.avgCPU.round(0.1),
        server.numSynths
      ),
      "Tempo: % BPM | Device: %".format(
        this.tempo.round(0.1),
        server.options.device ?? "default"
      ),
      "Samples: %/% loaded".format(loadedSamples, totalSamples),
      "Pdefs playing: %".format(if(playingPdefs.isEmpty) { "none" } { playingPdefs.join(", ") }),
      "Ndefs playing: %".format(if(playingNdefs.isEmpty) { "none" } { playingNdefs.join(", ") })
    ];
    lines.join("\n").postln;
  }
}
