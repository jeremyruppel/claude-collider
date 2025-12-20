// CCOutputTest - Unit tests for CCOutput

CCOutputTest : UnitTest {
  var output, mockCC, mockInBus;

  setUp {
    mockCC = this.createMockCC;
    mockInBus = this.createMockInBus;
  }

  tearDown {
    // Clean up test Ndefs and Pdefs
    [\testOutput1, \testOutput2, \testOutput3].do { |name|
      Ndef(name).clear;
    };
    [\testPdef1, \testPdef2].do { |name|
      if(Pdef.all[name].notNil) { Pdef(name).clear };
    };
  }

  createMockCC {
    ^(
      server: Server.default
    );
  }

  createMockInBus {
    ^(
      index: 16,
      free: {}
    );
  }

  createTestNdef { |name|
    ^Ndef(name, { Silent.ar(2) });
  }

  createEmptyNdef { |name|
    ^Ndef(name);
  }

  // ========== isMain tests ==========

  test_isMain_true {
    var ndef = this.createTestNdef(\testOutput1);
    output = CCOutput(mockCC, \out_main, ndef, nil, [1, 2], 0);
    this.assert(output.isMain, "Output with nil inBus should be main");
  }

  test_isMain_false {
    var ndef = this.createTestNdef(\testOutput1);
    output = CCOutput(mockCC, \out_3, ndef, mockInBus, 3, 2);
    this.assert(output.isMain.not, "Output with inBus should not be main");
  }

  // ========== numChannels tests ==========

  test_numChannels_stereo {
    var ndef = this.createTestNdef(\testOutput1);
    output = CCOutput(mockCC, \out_3_4, ndef, mockInBus, [3, 4], 2);
    this.assertEquals(output.numChannels, 2, "Stereo output should have 2 channels");
  }

  test_numChannels_mono {
    var ndef = this.createTestNdef(\testOutput1);
    output = CCOutput(mockCC, \out_3, ndef, mockInBus, 3, 2);
    this.assertEquals(output.numChannels, 1, "Mono output should have 1 channel");
  }

  // ========== channelString tests ==========

  test_channelString_stereo {
    var ndef = this.createTestNdef(\testOutput1);
    output = CCOutput(mockCC, \out_3_4, ndef, mockInBus, [3, 4], 2);
    this.assertEquals(output.channelString, "3-4", "Stereo should format as 3-4");
  }

  test_channelString_mono {
    var ndef = this.createTestNdef(\testOutput1);
    output = CCOutput(mockCC, \out_3, ndef, mockInBus, 3, 2);
    this.assertEquals(output.channelString, "3", "Mono should format as single number");
  }

  // ========== inputBusIndex tests ==========

  test_inputBusIndex_main {
    var ndef = this.createTestNdef(\testOutput1);
    output = CCOutput(mockCC, \out_main, ndef, nil, [1, 2], 0);
    this.assertEquals(output.inputBusIndex, 0, "Main output should use bus 0");
  }

  test_inputBusIndex_other {
    var ndef = this.createTestNdef(\testOutput1);
    output = CCOutput(mockCC, \out_3, ndef, mockInBus, 3, 2);
    this.assertEquals(output.inputBusIndex, 16, "Other output should use inBus index");
  }

  // ========== isPlaying tests ==========

  test_isPlaying_false {
    var ndef = this.createEmptyNdef(\testOutput1);
    output = CCOutput(mockCC, \out_main, ndef, nil, [1, 2], 0);
    this.assert(output.isPlaying.not, "Should report not playing when ndef is stopped");
  }

  // ========== statusString tests ==========

  test_statusString_stopped {
    var ndef = this.createEmptyNdef(\testOutput1);
    output = CCOutput(mockCC, \out_3, ndef, mockInBus, 3, 2);
    this.assertEquals(
      output.statusString,
      "out_3 -> hw 3 (stopped)",
      "Status string should show stopped state"
    );
  }

  // ========== status tests ==========

  test_status_returnsEvent {
    var ndef = this.createTestNdef(\testOutput1);
    output = CCOutput(mockCC, \out_main, ndef, nil, [1, 2], 0);
    this.assertEquals(output.status.key, \out_main, "Status should include key");
    this.assertEquals(output.status.channels, [1, 2], "Status should include channels");
    this.assertEquals(output.status.hwOut, 0, "Status should include hwOut");
    this.assert(output.status.isMain, "Status should include isMain");
  }

  // ========== sources tests ==========

  test_sources_empty {
    var ndef = this.createTestNdef(\testOutput1);
    output = CCOutput(mockCC, \out_main, ndef, nil, [1, 2], 0);
    this.assertEquals(output.sources, [], "Sources should be empty initially");
  }

  // ========== setHwOut tests ==========

  test_setHwOut_updatesHwOut {
    var ndef = this.createTestNdef(\testOutput1);
    output = CCOutput(mockCC, \out_main, ndef, nil, [1, 2], 0);
    output.setHwOut(6);
    this.assertEquals(output.hwOut, 6, "hwOut should be updated");
  }
}
