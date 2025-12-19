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
        chains: Dictionary[],
        connections: Dictionary[],
        routes: Dictionary[],
        sidechains: Dictionary[]
      )
    );
  }

  createMockServer {
    ^(
      serverRunning: true,
      avgCPU: 5.5,
      numSynths: 10,
      options: (device: "Test Device")
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
    this.assert(
      result.contains("12.8%"),
      "formatServer should round CPU to 1 decimal place"
    );
  }

  // ========== formatTempo tests ==========

  test_formatTempo {
    var result = formatter.formatTempo;
    this.assertEquals(
      result,
      "Tempo: 120.0 BPM | Device: Test Device",
      "formatTempo should format tempo and device"
    );
  }

  test_formatTempo_defaultDevice {
    var result;
    mockCC.server.options.device = nil;
    result = formatter.formatTempo;
    this.assertEquals(
      result,
      "Tempo: 120.0 BPM | Device: default",
      "formatTempo should show 'default' when device is nil"
    );
  }

  test_formatTempo_roundsBPM {
    var result;
    mockCC.tempo = 128.567;
    result = formatter.formatTempo;
    this.assert(
      result.contains("128.6 BPM"),
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
    var lines = result.split($\n);
    this.assertEquals(lines.size, 5, "format should have 5 lines");
    this.assert(lines[0].contains("Server:"), "Line 1 should be server info");
    this.assert(lines[1].contains("Tempo:"), "Line 2 should be tempo info");
    this.assert(lines[2].contains("Samples:"), "Line 3 should be samples info");
    this.assert(lines[3].contains("Pdefs"), "Line 4 should be Pdefs info");
    this.assert(lines[4].contains("Ndefs"), "Line 5 should be Ndefs info");
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
    mockCC.fx.chains = Dictionary[
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
    mockCC.fx.chains = Dictionary[\myChain -> [\fx_a, \fx_b, \fx_c]];
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
    mockCC.fx.chains = Dictionary[\myChain -> [\fx_a, \fx_b, \fx_c]];
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
}
