// CCOutputsTest - Unit tests for CCOutputs

CCOutputsTest : UnitTest {
  var outputs, mockCC;

  setUp {
    mockCC = this.createMockCC;
    outputs = CCOutputs(mockCC);
  }

  tearDown {
    // Clean up test outputs
  }

  createMockCC {
    ^(
      server: Server.default
    );
  }

  // ========== init tests ==========

  test_init_emptyOutputs {
    this.assertEquals(outputs.size, 0, "Should start with no outputs");
  }

  test_init_emptyRoutes {
    this.assertEquals(outputs.outputFor(\test), nil, "Should start with no routes");
  }

  // ========== at tests ==========

  test_at_notFound {
    this.assertEquals(outputs.at(\nonexistent), nil, "Should return nil for unknown key");
  }

  // ========== main tests ==========

  test_main_nil {
    this.assertEquals(outputs.main, nil, "Main should be nil before creation");
  }

  // ========== isMainPlaying tests ==========

  test_isMainPlaying_noMain {
    this.assert(outputs.isMainPlaying.not, "Should not be playing without main output");
  }

  // ========== keys tests ==========

  test_keys_empty {
    this.assertEquals(outputs.keys, [], "Keys should be empty initially");
  }

  // ========== size tests ==========

  test_size_empty {
    this.assertEquals(outputs.size, 0, "Size should be 0 initially");
  }

  // ========== outputFor tests ==========

  test_outputFor_notRouted {
    this.assertEquals(outputs.outputFor(\someSource), nil, "Should return nil for unrouted source");
  }

  // ========== status tests ==========

  test_status_empty {
    this.assertEquals(outputs.status, "  (none)", "Status should show none when empty");
  }

  // ========== keysValuesDo tests ==========

  test_keysValuesDo_empty {
    var count = 0;
    outputs.keysValuesDo { count = count + 1 };
    this.assertEquals(count, 0, "Should not iterate when empty");
  }
}
