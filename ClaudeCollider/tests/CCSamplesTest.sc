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

  test_describe_formatsUnloadedSample {
    var result, pathsDict, key;
    key = "unloaded";
    pathsDict = Dictionary.new;
    pathsDict.put(key, "/path/to/unloaded.wav");
    samples.instVarPut(\paths, pathsDict);
    samples.instVarPut(\buffers, Dictionary.new);

    result = samples.describe;

    this.assertEquals(
      result,
      "unloaded (not loaded)",
      "describe should show not loaded for unloaded samples"
    );
  }

  test_describe_emptyReturnsNone {
    var result;
    samples.instVarPut(\paths, Dictionary.new);
    samples.instVarPut(\buffers, Dictionary.new);

    result = samples.describe;

    this.assertEquals(
      result,
      "(none)",
      "describe should return (none) when no samples"
    );
  }

  // ========== names tests ==========

  test_names_returnsPathKeys {
    var result, pathsDict, key1, key2;
    key1 = "kick";
    key2 = "snare";
    pathsDict = Dictionary.new;
    pathsDict.put(key1, "/a");
    pathsDict.put(key2, "/b");
    samples.instVarPut(\paths, pathsDict);
    result = samples.names;
    this.assertEquals(result.size, 2, "names should return 2 items");
    this.assert(result.any { |n| n == key1 }, "names should include kick");
    this.assert(result.any { |n| n == key2 }, "names should include snare");
  }

  test_names_emptyWhenNoPaths {
    var result;
    samples.instVarPut(\paths, Dictionary.new);
    result = samples.names;
    this.assertEquals(result.size, 0, "names should be empty when no paths");
  }

  // ========== list tests ==========

  test_list_returnsCommaSeparatedString {
    var result, pathsDict, key1, key2;
    key1 = "kick";
    key2 = "snare";
    pathsDict = Dictionary.new;
    pathsDict.put(key1, "/a");
    pathsDict.put(key2, "/b");
    samples.instVarPut(\paths, pathsDict);
    result = samples.list;
    this.assertEquals(result, "kick, snare", "list should return comma-separated names");
  }

  test_list_isSorted {
    var result, pathsDict, key1, key2, key3;
    key1 = "zebra";
    key2 = "alpha";
    key3 = "middle";
    pathsDict = Dictionary.new;
    pathsDict.put(key1, "/z");
    pathsDict.put(key2, "/a");
    pathsDict.put(key3, "/m");
    samples.instVarPut(\paths, pathsDict);
    result = samples.list;
    this.assertEquals(result, "alpha, middle, zebra", "list should be sorted alphabetically");
  }

  test_list_emptyReturnsNone {
    var result;
    samples.instVarPut(\paths, Dictionary.new);
    result = samples.list;
    this.assertEquals(result, "(none)", "list should return (none) when empty");
  }
}
