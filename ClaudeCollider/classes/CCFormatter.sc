// CCFormatter - Status formatting for ClaudeCollider

CCFormatter {
  var <cc;

  *new { |cc|
    ^super.new.init(cc);
  }

  init { |argCC|
    cc = argCC;
  }

  playingPdefs {
    ^Pdef.all.select(_.isPlaying).keys.asArray;
  }

  playingNdefs {
    var result = [];
    var proxySpace = Ndef.all[cc.server];
    if(proxySpace.notNil) {
      proxySpace.keysValuesDo { |key, proxy|
        if(proxy.isPlaying) { result = result.add(key) };
      };
    };
    ^result;
  }

  format {
    var lines = [
      this.formatServer,
      this.formatTempo,
      this.formatSamples,
      this.formatPdefs,
      this.formatNdefs
    ];
    ^lines.join("\n");
  }

  print {
    this.format.postln;
  }

  formatServer {
    ^"Server: % | CPU: %% | Synths: %".format(
      if(cc.server.serverRunning) { "running" } { "stopped" },
      cc.server.avgCPU.round(0.1),
      cc.server.numSynths
    );
  }

  formatTempo {
    ^"Tempo: % BPM | Device: %".format(
      cc.tempo.round(0.1),
      cc.server.options.device ?? "default"
    );
  }

  formatSamples {
    ^"Samples: %/% loaded".format(
      cc.samples.buffers.size,
      cc.samples.paths.size
    );
  }

  formatPdefs {
    var playing = this.playingPdefs;
    ^"Pdefs playing: %".format(
      if(playing.isEmpty) { "none" } { playing.join(", ") }
    );
  }

  formatNdefs {
    var playing = this.playingNdefs;
    ^"Ndefs playing: %".format(
      if(playing.isEmpty) { "none" } { playing.join(", ") }
    );
  }
}
