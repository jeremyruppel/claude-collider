// CCRouterTest - Unit tests for CCRouter

CCRouterTest : CCUnitTest {
  var router, mockFX;

  setUp {
    mockFX = this.createMockFX;
    router = CCRouter(mockFX);
  }

  tearDown {
    // Clean up
  }

  createMockFX {
    ^(
      cc: (server: Server.default),
      loaded: Dictionary[],
      sidechains: (at: { |self, name| nil })
    );
  }

  // ========== init tests ==========

  test_init_emptyConnections {
    this.assertEquals(router.connections.size, 0, "Should start with no connections");
  }

  test_init_emptyChains {
    this.assertEquals(router.chains.size, 0, "Should start with no chains");
  }

  test_init_emptyRoutes {
    this.assertEquals(router.routes.size, 0, "Should start with no routes");
  }

  // ========== wouldCreateCycle tests ==========

  test_wouldCreateCycle_noConnections {
    this.assert(router.wouldCreateCycle(\a, \b).not, "No cycle without connections");
  }

  // ========== registerChain tests ==========

  test_registerChain_addsChain {
    router.registerChain(\myChain, [\fx_a, \fx_b, \fx_c]);
    this.assertEquals(router.chains[\myChain], [\fx_a, \fx_b, \fx_c], "Should store chain slots");
  }

  // ========== unregisterChain tests ==========

  test_unregisterChain_removesChain {
    router.registerChain(\myChain, [\fx_a, \fx_b]);
    router.unregisterChain(\myChain);
    this.assertEquals(router.chains[\myChain], nil, "Should remove chain");
  }

  // ========== chainSlots tests ==========

  test_chainSlots_returnsSlots {
    router.registerChain(\myChain, [\fx_a, \fx_b]);
    this.assertEquals(router.chainSlots(\myChain), [\fx_a, \fx_b], "Should return chain slots");
  }

  test_chainSlots_nil {
    this.assertEquals(router.chainSlots(\nonexistent), nil, "Should return nil for unknown chain");
  }

  // ========== connect validation tests ==========

  test_connect_sourceNotFound {
    var result = router.connect(\nonexistent, \other);
    this.assertEquals(result, nil, "Should return nil when source not found");
  }

  test_connect_destNotFound {
    mockFX.loaded[\fx_a] = (ndef: (bus: (index: 10)), inBus: (index: 12));
    this.assertEquals(router.connect(\fx_a, \nonexistent), nil, "Should return nil when dest not found");
  }

  test_connect_selfConnection {
    mockFX.loaded[\fx_a] = (ndef: (bus: (index: 10)), inBus: (index: 12));
    this.assertEquals(router.connect(\fx_a, \fx_a), nil, "Should return nil for self-connection");
  }

  // ========== disconnect tests ==========

  test_disconnect_notConnected {
    this.assert(router.disconnect(\nonexistent).not, "Should return false when not connected");
  }

  // ========== route validation tests ==========

  test_route_targetNotFound {
    var result = router.route(\source, \nonexistent);
    this.assert(result.not, "Should return false when target not found");
  }

  test_route_findsNdefSource {
    var result;
    mockFX.loaded[\fx_test] = (inBus: (index: 16));
    Ndef(\cc_test_router_ndef).ar(2);
    result = router.route(\cc_test_router_ndef, \fx_test);
    this.assert(result, "Should find an existing Ndef and route it");
    this.assertEquals((router.routes[\cc_test_router_ndef] !? { |r| r[\target] }), \fx_test, "Should record the Ndef route");
    router.unroute(\cc_test_router_ndef);
    Ndef(\cc_test_router_ndef).clear;
  }

  test_route_chainNameRoutesToFirstSlot {
    var result;
    mockFX.loaded[\chain_0_lpf] = (inBus: (index: 18));
    mockFX.loaded[\chain_1_reverb] = (inBus: (index: 22));
    router.registerChain(\chain, [\chain_0_lpf, \chain_1_reverb]);
    Pdef(\cc_test_router_chain, Pbind());
    result = router.route(\cc_test_router_chain, \chain);
    this.assert(result, "Should route to a chain by name");
    this.assertEquals(Pdef(\cc_test_router_chain).get(\out), 18, "Should send to the chain's first effect");
    this.assertEquals((router.routes[\cc_test_router_chain] !? { |r| r[\target] }), \chain_0_lpf, "Should record the first slot as the target");
    Pdef(\cc_test_router_chain).clear;
  }

  test_route_movesNdefOutputInsteadOfCopying {
    mockFX.loaded[\fx_test] = (inBus: (index: 16));
    Ndef(\cc_test_router_move).ar(2);
    router.route(\cc_test_router_move, \fx_test);
    this.assertEquals(Ndef(\cc_test_router_move).monitor.out, 16, "Routed Ndef should play only to the effect bus");
    router.unroute(\cc_test_router_move);
    this.assertEquals(Ndef(\cc_test_router_move).monitor.out, router.mainOut, "Unrouted Ndef should go back to main");
    Ndef(\cc_test_router_move).clear;
  }

  // ========== unroute tests ==========

  test_mainOut_fallsBackToZeroWithoutOutputs {
    this.assertEquals(router.mainOut, 0, "Should fall back to bus 0 when there is no output system");
  }

  test_unroute_sendsPdefToMain {
    Pdef(\cc_test_router_unroute, Pbind());
    Pdef(\cc_test_router_unroute).set(\out, 42);
    router.routes[\cc_test_router_unroute] = (target: \fx_test);
    router.unroute(\cc_test_router_unroute);
    this.assertEquals(Pdef(\cc_test_router_unroute).envir[\out], router.mainOut, "Unrouted Pdef should go to main");
    Pdef(\cc_test_router_unroute).clear;
  }

  test_unroute_notRouted {
    this.assert(router.unroute(\nonexistent).not, "Should return false when not routed");
  }

  // ========== status tests ==========

  test_status_empty {
    this.assertEquals(router.status, "  (none)", "Status should show none when empty");
  }

  test_status_withChain {
    var result;
    router.registerChain(\myChain, [\fx_a, \fx_b]);
    result = router.status;
    this.assert(result.contains("Chains:"), "Status should show chains section");
    this.assert(result.contains("myChain"), "Status should show chain name");
  }

  // ========== clear tests ==========

  test_clear_clearsAll {
    router.registerChain(\myChain, [\fx_a, \fx_b]);
    router.clear;
    this.assertEquals(router.connections.size, 0, "Should clear connections");
    this.assertEquals(router.chains.size, 0, "Should clear chains");
    this.assertEquals(router.routes.size, 0, "Should clear routes");
  }
}
