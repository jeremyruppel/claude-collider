// CCMIDIInputTest - Unit tests for CCMIDIInput

CCMIDIInputTest : UnitTest {
  var input;

  setUp {
    input = CCMIDIInput();
  }

  tearDown {
    input.disconnect;
  }

  // ========== init tests ==========

  test_init_notInitialized {
    this.assertEquals(input.initialized, false, "Should start uninitialized");
  }

  test_init_noInputs {
    this.assertEquals(input.inputs.size, 0, "Should start with no inputs");
  }

  test_init_noOutput {
    this.assertEquals(input.output, nil, "Should start with no output");
  }

  // ========== hasInputs tests ==========

  test_hasInputs_false {
    this.assertEquals(input.hasInputs, false, "Should return false when no inputs");
  }

  // ========== hasOutput tests ==========

  test_hasOutput_false {
    this.assertEquals(input.hasOutput, false, "Should return false when no output");
  }

  // ========== disconnect tests ==========

  test_disconnect_clearsInputs {
    input.disconnect(\in);
    this.assertEquals(input.inputs.size, 0, "Should clear inputs");
  }

  test_disconnect_clearsOutput {
    input.disconnect(\out);
    this.assertEquals(input.output, nil, "Should clear output");
  }

  test_disconnect_all {
    input.disconnect(\all);
    this.assertEquals(input.inputs.size, 0, "Should clear inputs");
    this.assertEquals(input.output, nil, "Should clear output");
  }

  // ========== status tests ==========

  test_status_containsInitialized {
    var status = input.status;
    this.assert(status[\initialized].notNil, "Status should contain initialized");
  }

  test_status_containsInputs {
    var status = input.status;
    this.assert(status[\inputs].notNil, "Status should contain inputs");
  }

  test_status_containsHasOutput {
    var status = input.status;
    this.assert(status[\hasOutput].notNil, "Status should contain hasOutput");
  }
}
