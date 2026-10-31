// CCSidechainTest - Unit tests for CCSidechain

CCSidechainTest : CCUnitTest {
  var sidechain, mockCC;

  setUp {
    mockCC = this.createMockCC;
    sidechain = CCSidechain(mockCC);
  }

  tearDown {
    // Clean up
  }

  createMockCC {
    ^(
      server: Server.default
    );
  }

  // ========== init tests ==========

  test_init_emptySidechains {
    this.assertEquals(sidechain.size, 0, "Should start with no sidechains");
  }

  // ========== at tests ==========

  test_at_notFound {
    this.assertEquals(sidechain.at(\nonexistent), nil, "Should return nil for unknown sidechain");
  }

  // ========== size tests ==========

  test_size_empty {
    this.assertEquals(sidechain.size, 0, "Size should be 0 initially");
  }

  // ========== keys tests ==========

  test_keys_empty {
    this.assertEquals(sidechain.keys, [], "Keys should be empty initially");
  }

  // ========== status tests ==========

  test_status_empty {
    this.assertEquals(sidechain.status, "  (none)", "Status should show none when empty");
  }

  // ========== output destination tests ==========

  test_mainOut_fallsBackToZeroWithoutOutputs {
    this.assertEquals(sidechain.mainOut, 0, "Should fall back to bus 0 when there is no output system");
  }

  test_currentOut_unroutedPdefGoesToMain {
    Pdef(\cc_test_sc_unrouted, Pbind());
    Pdef(\cc_test_sc_unrouted).envir = nil;
    this.assertEquals(sidechain.currentOut(\cc_test_sc_unrouted), sidechain.mainOut, "Unrouted Pdef should resolve to main");
    Pdef(\cc_test_sc_unrouted).clear;
  }

  test_currentOut_keepsExistingRoute {
    Pdef(\cc_test_sc_routed, Pbind());
    Pdef(\cc_test_sc_routed).set(\out, 42);
    this.assertEquals(sidechain.currentOut(\cc_test_sc_routed), 42, "Routed Pdef should keep its bus");
    Pdef(\cc_test_sc_routed).clear;
  }

  test_freeTriggerRoute_restoresPreviousOut {
    var routeInfo = (routeBus: Bus.audio(Server.default, 2), restoreOut: 42);
    Pdef(\cc_test_sc_restore, Pbind());
    Pdef(\cc_test_sc_restore).set(\out, routeInfo.routeBus);
    sidechain.freeTriggerRoute(\cc_test_sc_restore, routeInfo);
    this.assertEquals(Pdef(\cc_test_sc_restore).envir[\out], 42, "Should restore the route that existed before the trigger");
    Pdef(\cc_test_sc_restore).clear;
  }

  // ========== keysValuesDo tests ==========

  test_keysValuesDo_empty {
    var count = 0;
    sidechain.keysValuesDo { count = count + 1 };
    this.assertEquals(count, 0, "Should not iterate when empty");
  }
}
