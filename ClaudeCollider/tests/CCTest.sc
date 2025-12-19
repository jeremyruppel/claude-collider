CCTest : UnitTest {
  var cc;

  setUp {
    this.bootServer;
    cc = CC(Server.default);
  }

  tearDown {
    cc.clear;
  }

  test_new_createsInstance {
    this.assert(cc.notNil, "CC.new should create instance");
    this.assert(cc.isKindOf(CC), "Instance should be CC");
  }

  test_new_initializesSubsystems {
    this.assert(cc.synths.isKindOf(CCSynths), "Should initialize synths");
    this.assert(cc.samples.isKindOf(CCSamples), "Should initialize samples");
    this.assert(cc.fx.isKindOf(CCFX), "Should initialize fx");
    this.assert(cc.midi.isKindOf(CCMIDI), "Should initialize midi");
    this.assert(cc.state.isKindOf(CCState), "Should initialize state");
    this.assert(cc.recorder.isKindOf(CCRecorder), "Should initialize recorder");
    this.assert(cc.formatter.isKindOf(CCFormatter), "Should initialize formatter");
  }

  test_new_setsServer {
    this.assertEquals(cc.server, Server.default, "Should use provided server");
  }

  test_tempo_get {
    TempoClock.default.tempo = 2; // 120 BPM
    this.assertEquals(cc.tempo, 120.0, "tempo should return BPM");
  }

  test_tempo_set {
    cc.tempo(140);
    this.assertEquals(TempoClock.default.tempo, 140/60, "tempo should set TempoClock");
  }

  test_tempo_setReturnsValue {
    this.assertEquals(cc.tempo(100), 100, "tempo setter should return BPM");
  }

  test_numOutputs_get {
    cc.server.options.numOutputBusChannels = 8;
    this.assertEquals(cc.numOutputs, 8, "numOutputs should return channel count");
  }

  test_numOutputs_set {
    cc.numOutputs(16);
    this.assertEquals(cc.server.options.numOutputBusChannels, 16, "numOutputs should set channels");
  }

  test_device_setNil {
    cc.device(nil);
    this.assertEquals(cc.server.options.device, nil, "device(nil) should set default device");
  }

  test_device_setName {
    cc.device("TestDevice");
    this.assertEquals(cc.server.options.device, "TestDevice", "device should set device name");
  }

  test_stop_stopsPdefs {
    Pdef(\testStop, Pbind(\dur, 1)).play;
    0.1.wait;
    cc.stop;
    this.assert(Pdef(\testStop).isPlaying.not, "stop should stop Pdefs");
    Pdef(\testStop).clear;
  }

  test_clear_clearsState {
    Pdef(\testClear, Pbind(\dur, 1));
    cc.clear;
    this.assertEquals(Pdef(\testClear).source, nil, "clear should clear Pdef sources");
  }
}
