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

  test_init_emptySynths {
    this.assertEquals(midi.synths.size, 0, "Should start with no synths");
  }

  // ========== listDevices tests ==========

  test_listDevices_delegatesToInput {
    var result = midi.listDevices;
    this.assert(result[\inputs].notNil, "Should return inputs");
    this.assert(result[\outputs].notNil, "Should return outputs");
  }

  // ========== stop tests ==========

  test_stop_withNoSynths {
    var result = midi.stop;
    this.assertEquals(result, true, "Should return true even with no synths");
  }

  test_stop_clearsSynths {
    midi.stop;
    this.assertEquals(midi.synths.size, 0, "Should clear synths");
  }

  test_stop_byName_removesOnlyThat {
    midi.play(\cc_lead, 0);
    midi.play(\cc_bass, 1);
    midi.stop(\cc_lead);
    this.assertEquals(midi.synths.size, 1, "Should remove only named synth");
    this.assert(midi.synths[\cc_bass].notNil, "Should keep other synth");
  }

  test_stop_all_removesAll {
    midi.play(\cc_lead, 0);
    midi.play(\cc_bass, 1);
    midi.stop;
    this.assertEquals(midi.synths.size, 0, "Should remove all synths");
  }

  // ========== play tests ==========

  test_play_addsSynth {
    midi.play(\cc_lead, 0);
    this.assertEquals(midi.synths.size, 1, "Should add synth");
  }

  test_play_multipleSynths {
    midi.play(\cc_lead, 0);
    midi.play(\cc_bass, 1);
    this.assertEquals(midi.synths.size, 2, "Should support multiple synths");
  }

  test_play_replacesExistingSynth {
    midi.play(\cc_lead, 0);
    midi.play(\cc_lead, 1);
    this.assertEquals(midi.synths.size, 1, "Should replace, not duplicate");
  }

  // ========== clear tests ==========

  test_clear_clearsSynths {
    midi.play(\cc_lead, 0);
    midi.clear;
    this.assertEquals(midi.synths.size, 0, "Should clear synths");
  }

  // ========== status tests ==========

  test_status_containsInput {
    var status = midi.status;
    this.assert(status[\input].notNil, "Status should contain input");
  }

  test_status_containsSynths {
    var status = midi.status;
    // synths is nil when not playing
    this.assertEquals(status[\synths], nil, "Status should contain synths (nil when empty)");
  }

  test_status_inputHasInitialized {
    var status = midi.status;
    this.assert(status[\input][\initialized].notNil, "Input status should have initialized");
  }
}
