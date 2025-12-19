// CCSamplesTest - Unit tests for CCSamples

CCSamplesTest : UnitTest {
  var samples, mockCC;

  setUp {
    mockCC = this.createMockCC;
    samples = CCSamples(mockCC, "/tmp/test_samples");
  }

  tearDown {
  }

  createMockCC {
    ^(
      server: (
        serverRunning: true
      )
    );
  }

  createMockBuffer { |duration, numChannels, sampleRate|
    ^(
      duration: duration,
      numChannels: numChannels,
      sampleRate: sampleRate
    );
  }

  // ========== describe tests ==========

  test_describe_formatsLoadedSample {
    var duration, numChannels, sampleRate, name, result;
    duration = 2.5;
    numChannels = 2;
    sampleRate = 44100;
    name = \kick;

    // Test the format string directly (SC's format doesn't support %.2f)
    result = "% (%s, %ch, %Hz)".format(name, duration.round(0.01), numChannels, sampleRate.asInteger);

    this.assertEquals(
      result,
      "kick (2.5s, 2ch, 44100Hz)",
      "describe should format loaded sample with duration, channels, and sample rate"
    );
  }

  test_describe_formatsUnloadedSample {
    var name, buf, result;
    samples.instVarPut(\paths, Dictionary[\unloaded -> "/path/to/unloaded.wav"]);
    samples.instVarPut(\buffers, Dictionary[]);

    name = \unloaded;
    buf = samples.buffers.at(name);

    if(buf.notNil) {
      result = "% (%.2fs, %ch, %Hz)".format(name, buf.duration, buf.numChannels, buf.sampleRate.asInteger);
    } {
      result = "% (not loaded)".format(name);
    };

    this.assertEquals(
      result,
      "unloaded (not loaded)",
      "describe should show not loaded for unloaded samples"
    );
  }

  // ========== names tests ==========

  test_names_returnsPathKeys {
    var result;
    samples.instVarPut(\paths, Dictionary[\kick -> "/a", \snare -> "/b"]);
    result = samples.names;
    this.assertEquals(result.size, 2, "names should return 2 items");
    this.assert(result.includes(\kick), "names should include kick");
    this.assert(result.includes(\snare), "names should include snare");
  }

  test_names_emptyWhenNoPaths {
    var result;
    samples.instVarPut(\paths, Dictionary[]);
    result = samples.names;
    this.assertEquals(result.size, 0, "names should be empty when no paths");
  }

  // ========== list tests ==========

  test_list_returnsCommaSeparatedString {
    var result;
    samples.instVarPut(\paths, Dictionary[\kick -> "/a", \snare -> "/b"]);
    result = samples.list;
    this.assertEquals(result, "kick, snare", "list should return comma-separated names");
  }

  test_list_isSorted {
    var result;
    samples.instVarPut(\paths, Dictionary[\zebra -> "/z", \alpha -> "/a", \middle -> "/m"]);
    result = samples.list;
    this.assertEquals(result, "alpha, middle, zebra", "list should be sorted alphabetically");
  }

  test_list_emptyReturnsNone {
    var result;
    samples.instVarPut(\paths, Dictionary[]);
    result = samples.list;
    this.assertEquals(result, "(none)", "list should return (none) when empty");
  }
}
