// CCMIDITest - Unit tests for CCMIDI facade

CCMIDITest : UnitTest {
  var midi, mockCC;

  setUp {
    mockCC = this.createMockCC;
    midi = CCMIDI(mockCC);
  }

  tearDown {
    midi.clear;
  }

  createMockCC {
    ^(
      server: Server.default
    );
  }

  // ========== init tests ==========

  test_init_hasInput {
    this.assert(midi.input.notNil, "Should have input");
  }

  test_init_inputIsCCMIDIInput {
    this.assert(midi.input.isKindOf(CCMIDIInput), "Input should be CCMIDIInput");
  }

  test_init_noSynth {
    this.assertEquals(midi.synth, nil, "Should start with no synth");
  }

  // ========== listDevices tests ==========

  test_listDevices_delegatesToInput {
    var result = midi.listDevices;
    this.assert(result[\inputs].notNil, "Should return inputs");
    this.assert(result[\outputs].notNil, "Should return outputs");
  }

  // ========== stop tests ==========

  test_stop_withNoSynth {
    var result = midi.stop;
    this.assertEquals(result, true, "Should return true even with no synth");
  }

  test_stop_clearsSynth {
    midi.stop;
    this.assertEquals(midi.synth, nil, "Should clear synth");
  }

  // ========== clear tests ==========

  test_clear_clearsSynth {
    midi.clear;
    this.assertEquals(midi.synth, nil, "Should clear synth");
  }

  // ========== status tests ==========

  test_status_containsInput {
    var status = midi.status;
    this.assert(status[\input].notNil, "Status should contain input");
  }

  test_status_containsSynth {
    var status = midi.status;
    // synth is nil when not playing
    this.assertEquals(status[\synth], nil, "Status should contain synth (nil)");
  }

  test_status_inputHasInitialized {
    var status = midi.status;
    this.assert(status[\input][\initialized].notNil, "Input status should have initialized");
  }
}
