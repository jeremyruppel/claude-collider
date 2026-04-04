// CCSamples - Sample management for ClaudeCollider

CCSamples {
  var <cc;
  var <paths;    // name (String) -> file path (String)
  var <buffers;  // name (String) -> Buffer
  var <samplesDirs;  // Array of directory paths

  *new { |cc, samplesPath|
    ^super.new.init(cc, samplesPath);
  }

  init { |argCC, argSamplesPath|
    cc = argCC;
    paths = Dictionary.new;
    buffers = Dictionary.new;
    samplesDirs = if(argSamplesPath.notNil) {
      argSamplesPath.split($:).reject { |s| s.isEmpty };
    } {
      [];
    };
  }

  // Scan all sample directories and store file paths (fast, sync)
  // Buffers are loaded lazily on first access
  // First directory in path wins on name conflicts
  loadAll {
    samplesDirs.do { |dir|
      var path = PathName(dir);
      var audioFiles;

      if(path.isFolder) {
        audioFiles = path.files.select { |f|
          #["wav", "aiff", "aif", "mp3"].includesEqual(f.extension.toLower)
        };

        audioFiles.do { |file|
          var name = file.fileNameWithoutExtension;
          if(paths.at(name).isNil) {
            paths.put(name, file.fullPath);
          };
        };
      };
    };
  }

  // Get a buffer by name (nil if not yet loaded)
  at { |name|
    ^buffers.at(name.asString);
  }

  // Get sample names as array
  names {
    ^paths.keys.asArray.sort;
  }

  // Format buffer info string
  formatInfo { |name, buf|
    ^"% (%s, %ch, %Hz)".format(name, buf.duration.round(0.01), buf.numChannels, buf.sampleRate.asInteger);
  }

  // Explicitly load a sample buffer into memory
  load { |name|
    var key = name.asString;
    var path = paths.at(key);
    var buf = buffers.at(key);

    if(path.isNil) {
      "CCSamples: unknown sample '%'".format(name).warn;
      ^nil;
    };

    if(buf.notNil) { ^buf };

    Buffer.read(cc.server, path, action: { |loadedBuf|
      buffers.put(key, loadedBuf);
    });
    ^nil;
  }

  // Play a sample once (lazy loads if needed)
  play { |name, rate=1, amp=0.5|
    var key = name.asString;
    var path = paths.at(key);
    var buf = buffers.at(key);

    if(path.isNil) {
      "CCSamples: unknown sample '%'".format(name).warn;
      ^nil;
    };

    if(buf.notNil) {
      ^Synth(\cc_sampler, [\buf, buf, \rate, rate, \amp, amp]);
    };

    Buffer.read(cc.server, path, action: { |loadedBuf|
      buffers.put(key, loadedBuf);
      Synth(\cc_sampler, [\buf, loadedBuf, \rate, rate, \amp, amp]);
    });
    ^nil;
  }

  // Free a sample buffer (keeps path for reload)
  free { |name|
    var key = name.asString;
    var buf = buffers.removeAt(key);
    if(buf.notNil) {
      buf.free;
      ^true;
    };
    ^false;
  }

  // Free all sample buffers
  freeAll {
    buffers.do { |buf| buf.free };
    buffers.clear;
  }

  // Reload all sample directories (discovers new files, keeps loaded buffers)
  reload {
    var oldPaths = paths.copy;
    var newCount = 0;
    paths.clear;
    this.loadAll;
    paths.keysValuesDo { |name, path|
      if(oldPaths.at(name).isNil) { newCount = newCount + 1 };
    };
    "CCSamples: found % samples (% new)".format(paths.size, newCount).postln;
    ^paths.size;
  }

  list {
    if(this.names.isEmpty) { ^"(none)" };
    ^this.names.sort.join(", ");
  }

  describe {
    var lines = this.names.collect { |name|
      var buf = buffers.at(name);
      if(buf.notNil) {
        this.formatInfo(name, buf);
      } {
        "% (not loaded)".format(name);
      };
    };
    if(lines.isEmpty) { ^"(none)" };
    ^lines.join("\n");
  }
}
