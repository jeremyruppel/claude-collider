// CCRecorder - Audio recording for ClaudeCollider

CCRecorder {
  var <cc;
  var <recordingsDir;
  var <isRecording;
  var <currentPath;

  *new { |cc, recordingsDir| ^super.new.init(cc, recordingsDir) }

  init { |argCC, argRecordingsDir|
    cc = argCC;
    recordingsDir = argRecordingsDir;
    isRecording = false;
    currentPath = nil;
    this.ensureDir;
    this.configureFormat;
  }

  ensureDir {
    if(File.exists(recordingsDir).not) { File.mkdir(recordingsDir) };
  }

  configureFormat {
    cc.server.recHeaderFormat = "wav";
    cc.server.recSampleFormat = "int16";
  }

  start { |filename|
    var path;
    if(isRecording) { ^"Already recording: %".format(currentPath) };

    filename = filename ?? this.generateFilename;
    path = recordingsDir +/+ filename;
    if(path.endsWith(".wav").not) { path = path ++ ".wav" };

    cc.server.record(path);
    isRecording = true;
    currentPath = path;
    ^"Recording started: %".format(path);
  }

  stop {
    var path;
    if(isRecording.not) { ^"Not recording" };

    path = currentPath;
    cc.server.stopRecording;
    isRecording = false;
    currentPath = nil;
    ^"Recording saved: %".format(path);
  }

  status {
    if(isRecording) {
      ^"Recording: %".format(currentPath);
    } {
      ^"Not recording";
    };
  }

  generateFilename {
    var d = Date.getDate;
    ^"recording_%_%_%_%.wav".format(
      d.year,
      d.month.asString.padLeft(2, "0"),
      d.day.asString.padLeft(2, "0"),
      d.format("%H%M%S")
    );
  }
}
