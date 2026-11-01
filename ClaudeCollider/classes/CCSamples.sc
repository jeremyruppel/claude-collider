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
    if(buf.numFrames.isNil) { ^"% (loading)".format(name) };
    ^"% (%s, %ch, %Hz)".format(name, buf.duration.round(0.01), buf.numChannels, buf.sampleRate.asInteger);
  }

  // Explicitly load a sample buffer into memory.
  // The Buffer is registered immediately (its bufnum is allocated up front),
  // so `at` works on the very next line; the server fills in the audio data
  // asynchronously, before any later-scheduled pattern events reach it.
  load { |name|
    var key = name.asString;
    var path = paths.at(key);
    var buf = buffers.at(key);

    if(path.isNil) {
      "CCSamples: unknown sample '%'".format(name).warn;
      ^nil;
    };

    if(buf.notNil) { ^buf };

    buf = this.readBuffer(path);
    buffers.put(key, buf);
    ^buf;
  }

  // Read a file into a buffer. cc_sampler and cc_breakbeat play 2-channel
  // buffers, so a mono file is read twice into a stereo buffer, one copy
  // per side. Otherwise it would play out of the left channel only.
  readBuffer { |path, action|
    var channels = this.readChannels(this.fileChannels(path));
    if(channels.notNil) {
      ^Buffer.readChannel(cc.server, path, channels: channels, action: action);
    };
    ^Buffer.read(cc.server, path, action: action);
  }

  // Channels to read for a file with numChannels channels: [0, 0] copies a
  // mono file to both sides; nil reads the file as is.
  readChannels { |numChannels|
    ^if(numChannels == 1) { [0, 0] } { nil };
  }

  // Number of channels in a sound file, or nil if it can't be opened
  fileChannels { |path|
    var file = SoundFile.openRead(path);
    var numChannels;
    if(file.isNil) { ^nil };
    numChannels = file.numChannels;
    file.close;
    ^numChannels;
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

    this.readBuffer(path, { |loadedBuf|
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
