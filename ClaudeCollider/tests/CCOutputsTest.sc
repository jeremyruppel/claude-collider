// CCOutputsTest - Unit tests for CCOutputs

CCOutputsTest : CCUnitTest {
  var outputs, mockCC;

  setUp {
    mockCC = this.createMockCC;
    outputs = CCOutputs(mockCC);
  }

  tearDown {
    outputs.clear;
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

  // ========== createMain tests ==========

  // Something at the head of the default group (like a sidechain passthrough)
  // resets the main bus every cycle. Sources added later, like effect
  // monitors, must still reach the main output.
  test_createMain_hearsSourcesAfterAnEarlyWriter {
    var server = Server.default;
    var meterBus, early, late, meter, level, cond = Condition.new;

    this.bootServer;
    outputs.createMain;

    early = { Out.ar(outputs.mainBus, DC.ar(0).dup) }.play(server.defaultGroup, addAction: \addToHead);
    late = { Out.ar(outputs.mainBus, DC.ar(0.5).dup) }.play(server.defaultGroup, addAction: \addToTail);
    meterBus = Bus.control(server, 1);
    meter = {
      Out.kr(meterBus, Amplitude.kr(InFeedback.ar(outputs.main.ndef.bus.index, 2).sum))
    }.play(outputs.group, addAction: \addToTail);

    0.5.wait;
    meterBus.get { |value| level = value; cond.unhang };
    cond.hang;

    [early, late, meter].do(_.free);
    meterBus.free;

    this.assert(level > 0.1, "Main output should hear sources that write after an early writer (level: %)".format(level));
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
