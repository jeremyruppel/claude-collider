// CCSamples - Sample management for ClaudeCollider

CCSamples {
  var <cc;
  var <paths;    // name (String) -> file path (String)
  var <buffers;  // name (String) -> Buffer
  var <samplesDir;

  *new { |cc, samplesDir|
    ^super.new.init(cc, samplesDir);
  }

  init { |argCC, argSamplesDir|
    cc = argCC;
    paths = Dictionary.new;
    buffers = Dictionary.new;
    samplesDir = argSamplesDir;
  }

  // Scan samples directory and store file paths (fast, sync)
  // Buffers are loaded lazily on first access
  loadAll {
    var path = PathName(samplesDir);
    var audioFiles;

    if(path.isFolder.not) {
      File.mkdir(samplesDir);
    };

    audioFiles = path.files.select { |f|
      #["wav", "aiff", "aif"].includesEqual(f.extension.toLower)
    };

    audioFiles.do { |file|
      paths.put(file.fileNameWithoutExtension, file.fullPath);
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

  // Play a sample once (lazy loads if needed)
  play { |name, rate=1, amp=0.5|
    var key = name.asString;
    var path = paths.at(key);
    var buf = buffers.at(key);

    if(path.isNil) {
      "CCSamples: unknown sample '%'".format(name).warn;
      ^nil;
    };

    // Already loaded - play immediately
    if(buf.notNil) {
      ^Synth(\cc_sampler, [\buf, buf, \rate, rate, \amp, amp]);
    };

    // Lazy load then play
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

  // Reload samples directory (discovers new files, keeps loaded buffers)
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

  // Describe samples for display
  describe {
    this.names.do { |name|
      var buf = buffers.at(name);
      if(buf.notNil) {
        "% (%.2fs, %ch, %Hz)".format(name, buf.duration, buf.numChannels, buf.sampleRate.asInteger).postln;
      } {
        "% (not loaded)".format(name).postln;
      };
    };
    ^nil;
  }
}
