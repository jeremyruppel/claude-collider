// CCFormatterTest - Unit tests for CCFormatter

CCFormatterTest : UnitTest {
  var formatter, mockCC;

  setUp {
    mockCC = this.createMockCC;
    formatter = CCFormatter(mockCC);
  }

  tearDown {
    // Clean up any Pdefs/Ndefs we created
    [\testPdef1, \testPdef2, \testPdef3].do { |name|
      if(Pdef.all[name].notNil) { Pdef(name).clear };
    };
    [\testNdef1, \testNdef2].do { |name|
      Ndef(name).clear;
    };
  }

  createMockCC {
    ^(
      server: this.createMockServer,
      tempo: 120,
      samples: (
        buffers: Dictionary[],
        paths: Dictionary[]
      ),
      fx: (
        loaded: Dictionary[],
        router: this.createMockRouter,
        sidechains: this.createMockSidechains,
        outputs: this.createMockOutputs
      )
    );
  }

  createMockRouter {
    ^(
      chains: Dictionary[],
      connections: Dictionary[],
      routes: Dictionary[]
    );
  }

  createMockSidechains {
    ^(
      size: 0,
      sidechains: Dictionary[]
    );
  }

  createMockOutputs {
    ^(
      main: nil,
      outputs: Dictionary[]
    );
  }

  createMockServer {
    ^(
      serverRunning: true,
      avgCPU: 5.5,
      numSynths: 10,
      options: (device: "Test Device", numOutputBusChannels: 2)
    );
  }

  // ========== formatServer tests ==========

  test_formatServer_running {
    var result = formatter.formatServer;
    this.assertEquals(
      result,
      "Server: running | CPU: 5.5% | Synths: 10",
      "formatServer should format running server correctly"
    );
  }

  test_formatServer_stopped {
    var result;
    mockCC.server.serverRunning = false;
    result = formatter.formatServer;
    this.assertEquals(
      result,
      "Server: stopped | CPU: 5.5% | Synths: 10",
      "formatServer should show stopped when server not running"
    );
  }

  test_formatServer_roundsCPU {
    var result;
    mockCC.server.avgCPU = 12.789;
    result = formatter.formatServer;
    this.assertEquals(
      result,
      "Server: running | CPU: 12.8% | Synths: 10",
      "formatServer should round CPU to 1 decimal place"
    );
  }

  // ========== formatTempo tests ==========

  test_formatTempo {
    var result = formatter.formatTempo;
    this.assertEquals(
      result,
      "Tempo: 120.0 BPM | Device: Test Device (2 out)",
      "formatTempo should format tempo and device"
    );
  }

  test_formatTempo_defaultDevice {
    var result;
    mockCC.server.options.device = nil;
    result = formatter.formatTempo;
    this.assertEquals(
      result,
      "Tempo: 120.0 BPM | Device: default (2 out)",
      "formatTempo should show 'default' when device is nil"
    );
  }

  test_formatTempo_roundsBPM {
    var result;
    mockCC.tempo = 128.567;
    result = formatter.formatTempo;
    this.assertEquals(
      result,
      "Tempo: 128.6 BPM | Device: Test Device (2 out)",
      "formatTempo should round BPM to 1 decimal place"
    );
  }

  // ========== formatSamples tests ==========

  test_formatSamples_empty {
    var result = formatter.formatSamples;
    this.assertEquals(
      result,
      "Samples: 0/0 loaded",
      "formatSamples should show 0/0 when no samples"
    );
  }

  test_formatSamples_withSamples {
    var result;
    mockCC.samples.buffers = Dictionary[\kick -> 1, \snare -> 2];
    mockCC.samples.paths = Dictionary[\kick -> "a", \snare -> "b", \hat -> "c"];
    result = formatter.formatSamples;
    this.assertEquals(
      result,
      "Samples: 2/3 loaded",
      "formatSamples should show loaded/total counts"
    );
  }

  // ========== formatPdefs tests ==========

  test_formatPdefs_none {
    var result = formatter.formatPdefs;
    this.assertEquals(
      result,
      "Pdefs playing: none",
      "formatPdefs should show 'none' when no Pdefs playing"
    );
  }

  // ========== formatNdefs tests ==========

  test_formatNdefs_none {
    var result = formatter.formatNdefs;
    this.assertEquals(
      result,
      "Ndefs playing: none",
      "formatNdefs should show 'none' when no Ndefs playing"
    );
  }

  // ========== format (integration) tests ==========

  test_format_combinesAll {
    var result = formatter.format;
    var expected = [
      "Server: running | CPU: 5.5% | Synths: 10",
      "Tempo: 120.0 BPM | Device: Test Device (2 out)",
      "Outputs: none",
      "Samples: 0/0 loaded",
      "Pdefs playing: none",
      "Ndefs playing: none"
    ].join("\n");
    this.assertEquals(result, expected, "format should combine all sections");
  }

  test_format_withMainOutput {
    var result;
    var mockOutput = (
      hwOut: 0,
      channels: [1, 2],
      isPlaying: false
    );
    var expected;
    mockCC.fx.outputs = (
      main: mockOutput,
      outputs: Dictionary[\out_main -> mockOutput]
    );
    result = formatter.formatOutputs;
    expected = "Outputs: main 1-2 (limiter off)";
    this.assertEquals(result, expected, "formatOutputs should show main output when configured");
  }

  // ========== playingPdefs tests ==========

  test_playingPdefs_empty {
    var result = formatter.playingPdefs;
    this.assertEquals(result, [], "playingPdefs should return empty array when none playing");
  }

  // ========== playingNdefs tests ==========

  test_playingNdefs_empty {
    var result = formatter.playingNdefs;
    this.assertEquals(result, [], "playingNdefs should return empty array when none playing");
  }

  // ========== collectChainSlots tests ==========

  test_collectChainSlots_empty {
    var result = formatter.collectChainSlots;
    this.assertEquals(result, Set[], "collectChainSlots should return empty set when no chains");
  }

  test_collectChainSlots_withChains {
    var result;
    mockCC.fx.router.chains = Dictionary[
      \myChain -> [\fx_distortion, \fx_reverb],
      \otherChain -> [\fx_delay]
    ];
    result = formatter.collectChainSlots;
    this.assertEquals(result.size, 3, "collectChainSlots should collect all slots from all chains");
    this.assert(result.includes(\fx_distortion), "Should include fx_distortion");
    this.assert(result.includes(\fx_reverb), "Should include fx_reverb");
    this.assert(result.includes(\fx_delay), "Should include fx_delay");
  }

  // ========== isChainConnection tests ==========

  test_isChainConnection_true {
    mockCC.fx.router.chains = Dictionary[\myChain -> [\fx_a, \fx_b, \fx_c]];
    this.assert(
      formatter.isChainConnection(\fx_a, \fx_b),
      "Should detect connection within chain"
    );
    this.assert(
      formatter.isChainConnection(\fx_b, \fx_c),
      "Should detect second connection within chain"
    );
  }

  test_isChainConnection_false {
    mockCC.fx.router.chains = Dictionary[\myChain -> [\fx_a, \fx_b, \fx_c]];
    this.assert(
      formatter.isChainConnection(\fx_a, \fx_c).not,
      "Should not detect non-adjacent slots as chain connection"
    );
    this.assert(
      formatter.isChainConnection(\fx_x, \fx_y).not,
      "Should not detect connection outside chain"
    );
  }

  // ========== getPdefOutBus tests ==========

  test_getPdefOutBus_nil {
    var result = formatter.getPdefOutBus(nil);
    this.assertEquals(result, nil, "getPdefOutBus should return nil for nil pdef");
  }

  test_getPdefOutBus_noSource {
    var pdef = Pdef(\testPdef1);
    var result = formatter.getPdefOutBus(pdef);
    this.assertEquals(result, nil, "getPdefOutBus should return nil when pdef has no source");
  }

  test_getPdefOutBus_pbindWithOut {
    var pdef = Pdef(\testPdef2, Pbind(\out, 4, \dur, 1));
    var result = formatter.getPdefOutBus(pdef);
    this.assertEquals(result, 4, "getPdefOutBus should extract out from Pbind");
  }

  test_getPdefOutBus_eventWithOut {
    var pdef = Pdef(\testPdef3, (out: 8, dur: 1));
    var result = formatter.getPdefOutBus(pdef);
    this.assertEquals(result, 8, "getPdefOutBus should extract out from Event source");
  }

  // ========== getChainSlotOutBus tests ==========

  test_getChainSlotOutBus_lastSlot {
    var result = formatter.getChainSlotOutBus([\a, \b, \c], 2);
    this.assertEquals(result, 0, "Last slot in chain should output to bus 0 (main out)");
  }

  // ========== formatEffectParams tests ==========

  test_formatEffectParams {
    var mockNdef = (get: { |self, key| if(key == \mix) { 0.5 } { nil } });
    var info = (
      params: [\mix, 0.33, \room, 0.8],
      ndef: mockNdef
    );
    var result = formatter.formatEffectParams(info);
    this.assertEquals(result, "mix=0.5, room=0.8", "formatEffectParams should format params with current values");
  }

  test_formatEffectParams_usesDefaultWhenNil {
    var mockNdef = (get: { |self, key| nil });
    var info = (
      params: [\mix, 0.5, \room, 0.8],
      ndef: mockNdef
    );
    var result = formatter.formatEffectParams(info);
    this.assertEquals(result, "mix=0.5, room=0.8", "formatEffectParams should use default values when get returns nil");
  }

  // ========== collectChainInputBuses tests ==========

  test_collectChainInputBuses_empty {
    var result = formatter.collectChainInputBuses;
    this.assertEquals(result, Dictionary[], "collectChainInputBuses should return empty dict when no chains");
  }

  test_collectChainInputBuses_withChain {
    var result;
    var mockInBus = (index: 16);
    mockCC.fx.router.chains = Dictionary[\myChain -> [\fx_first, \fx_second]];
    mockCC.fx.loaded = Dictionary[\fx_first -> (inBus: mockInBus)];
    result = formatter.collectChainInputBuses;
    this.assertEquals(result[\myChain], 16, "collectChainInputBuses should map chain name to first slot's inBus index");
  }

  // ========== getChainSlotOutBus tests ==========

  test_getChainSlotOutBus_midSlot {
    var mockInBus = (index: 20);
    mockCC.fx.loaded = Dictionary[\fx_b -> (inBus: mockInBus)];
    this.assertEquals(
      formatter.getChainSlotOutBus([\fx_a, \fx_b, \fx_c], 0),
      20,
      "Mid slot should output to next slot's inBus"
    );
  }

  test_getChainSlotOutBus_nextSlotNotLoaded {
    mockCC.fx.loaded = Dictionary[];
    this.assertEquals(
      formatter.getChainSlotOutBus([\fx_a, \fx_b], 0),
      0,
      "Should return 0 when next slot is not loaded"
    );
  }

  // ========== getExpectedBus tests ==========

  test_getExpectedBus_fromLoadedEffect {
    var mockInBus = (index: 12);
    mockCC.fx.loaded = Dictionary[\fx_reverb -> (inBus: mockInBus)];
    this.assertEquals(
      formatter.getExpectedBus(\fx_reverb, Dictionary[]),
      12,
      "getExpectedBus should return inBus index from loaded effect"
    );
  }

  test_getExpectedBus_fromChainInputBuses {
    mockCC.fx.loaded = Dictionary[];
    this.assertEquals(
      formatter.getExpectedBus(\myChain, Dictionary[\myChain -> 24]),
      24,
      "getExpectedBus should fall back to chainInputBuses"
    );
  }

  test_getExpectedBus_notFound {
    mockCC.fx.loaded = Dictionary[];
    this.assertEquals(
      formatter.getExpectedBus(\unknown, Dictionary[]),
      nil,
      "getExpectedBus should return nil when target not found"
    );
  }

  // ========== formatSourceRoute tests ==========

  test_formatSourceRoute_noActualOut {
    var result;
    Pdef(\testPdef1).clear;
    mockCC.fx.loaded = Dictionary[];
    result = formatter.formatSourceRoute(\testPdef1, \fx_reverb, Dictionary[]);
    this.assertEquals(result[\line], "  testPdef1 → fx_reverb", "formatSourceRoute should format basic route without bus info");
    this.assertEquals(result[\warning], nil, "formatSourceRoute should have no warning when no bus info");
  }

  test_formatSourceRoute_matchingBus {
    var result;
    var mockInBus = (index: 4);
    Pdef(\testPdef2, Pbind(\out, 4, \dur, 1));
    mockCC.fx.loaded = Dictionary[\fx_reverb -> (inBus: mockInBus)];
    result = formatter.formatSourceRoute(\testPdef2, \fx_reverb, Dictionary[]);
    this.assertEquals(result[\line], "  testPdef2 → fx_reverb (out: 4) ✓", "formatSourceRoute should show checkmark for matching bus");
    this.assertEquals(result[\warning], nil, "formatSourceRoute should have no warning when buses match");
  }

  test_formatSourceRoute_mismatchedBus {
    var result;
    var mockInBus = (index: 8);
    Pdef(\testPdef3, Pbind(\out, 4, \dur, 1));
    mockCC.fx.loaded = Dictionary[\fx_reverb -> (inBus: mockInBus)];
    result = formatter.formatSourceRoute(\testPdef3, \fx_reverb, Dictionary[]);
    this.assertEquals(result[\line], "  testPdef3 → fx_reverb (out: 4) ✗ MISMATCH", "formatSourceRoute should show MISMATCH for wrong bus");
    this.assertEquals(result[\warning], "⚠ testPdef3: sending to bus 4, but fx_reverb expects bus 8", "formatSourceRoute should return warning for mismatch");
  }

  // ========== appendDebugWarnings tests ==========

  test_appendDebugWarnings_empty {
    var result = formatter.appendDebugWarnings(["existing line"], []);
    this.assertEquals(result, ["existing line"], "appendDebugWarnings should return unchanged lines when no warnings");
  }

  test_appendDebugWarnings_withWarnings {
    var result = formatter.appendDebugWarnings(["existing"], ["warning 1", "warning 2"]);
    var expected = ["existing", "", "Warnings:", "  warning 1", "  warning 2"];
    this.assertEquals(result, expected, "appendDebugWarnings should format warnings section");
  }

  // ========== appendDebugConnections tests ==========

  test_appendDebugConnections_empty {
    mockCC.fx.router.connections = Dictionary[];
    this.assertEquals(
      formatter.appendDebugConnections(["line"]),
      ["line"],
      "appendDebugConnections should return unchanged lines when no connections"
    );
  }

  test_appendDebugConnections_withNonChainConnection {
    var result;
    mockCC.fx.router.connections = Dictionary[\fx_dist -> (to: \fx_reverb)];
    mockCC.fx.router.chains = Dictionary[];
    result = formatter.appendDebugConnections(["a", "b", "c"]);
    this.assertEquals(
      result,
      ["a", "b", "c", "", "Connections:", "  fx_dist → fx_reverb"],
      "appendDebugConnections should format non-chain connections"
    );
  }

  test_appendDebugConnections_filtersChainConnections {
    var result;
    mockCC.fx.router.connections = Dictionary[\fx_a -> (to: \fx_b)];
    mockCC.fx.router.chains = Dictionary[\myChain -> [\fx_a, \fx_b]];
    result = formatter.appendDebugConnections(["line"]);
    this.assertEquals(result, ["line"], "appendDebugConnections should filter out chain connections");
  }

  // ========== appendDebugSources tests ==========

  test_appendDebugSources_empty {
    var warnings = [];
    mockCC.fx.router.routes = Dictionary[];
    this.assertEquals(
      formatter.appendDebugSources(["line"], Dictionary[], warnings),
      ["line"],
      "appendDebugSources should return unchanged lines when no routes"
    );
  }

  test_appendDebugSources_withRoutes {
    var warnings = [];
    var result;
    var routeInfo = (target: \fx_reverb);
    Pdef(\testPdef1).clear;
    mockCC.fx.router.routes[\testPdef1] = routeInfo;
    mockCC.fx.loaded = Dictionary[];
    result = formatter.appendDebugSources(["a", "b", "c"], Dictionary[], warnings);
    this.assertEquals(
      result,
      ["a", "b", "c", "", "Sources:", "  testPdef1 → fx_reverb"],
      "appendDebugSources should format source routes"
    );
  }

  // ========== appendDebugSidechains tests ==========

  test_appendDebugSidechains_empty {
    mockCC.fx.sidechains = (
      size: 0,
      sidechains: Dictionary[]
    );
    this.assertEquals(
      formatter.appendDebugSidechains(["line"]),
      ["line"],
      "appendDebugSidechains should return unchanged lines when no sidechains"
    );
  }
}
