// CCArrangementTest - Unit tests for CCArrangement

CCArrangementTest : CCUnitTest {
  var arr;

  setUp {
    if(CCArrangement.current.notNil) { CCArrangement.current.stop };
    Pdef.all.do(_.clear);
  }

  tearDown {
    if(CCArrangement.current.notNil) { CCArrangement.current.stop };
    Pdef.all.do(_.clear);
    Ndef.all[Server.default.name] !? { |space| space.do(_.clear) };
  }

  // ========== constructor tests ==========

  test_new_storesSections {
    var sections = [[\intro, 4, [\kick]], [\drop, 8, [\kick, \bass]]];
    arr = CCArrangement(sections);
    this.assertEquals(arr.sections, sections, "Should store sections array");
  }

  test_new_defaultBeatsPerBar {
    arr = CCArrangement([]);
    this.assertEquals(arr.beatsPerBar, 4, "Should default to 4 beats per bar");
  }

  test_new_customBeatsPerBar {
    arr = CCArrangement([], beatsPerBar: 3);
    this.assertEquals(arr.beatsPerBar, 3, "Should accept custom beatsPerBar");
  }

  test_new_notPlaying {
    arr = CCArrangement([]);
    this.assert(arr.isPlaying.not, "Should not be playing on creation");
  }

  test_new_emptyActiveElements {
    arr = CCArrangement([]);
    this.assertEquals(arr.activeElements.size, 0, "Should have no active elements");
  }

  // ========== diffElements tests ==========

  test_diffElements_startsNew {
    arr = CCArrangement([]);
    this.assertEquals(
      arr.diffElements(Set[], Set[\kick, \hat])[\start],
      Set[\kick, \hat],
      "Should start all new elements"
    );
  }

  test_diffElements_startsNew_stopsNone {
    arr = CCArrangement([]);
    this.assertEquals(
      arr.diffElements(Set[], Set[\kick, \hat])[\stop].size,
      0,
      "Should stop nothing when all are new"
    );
  }

  test_diffElements_stopsMissing {
    arr = CCArrangement([]);
    this.assertEquals(
      arr.diffElements(Set[\kick, \hat], Set[])[\stop],
      Set[\kick, \hat],
      "Should stop all removed elements"
    );
  }

  test_diffElements_stopsMissing_startsNone {
    arr = CCArrangement([]);
    this.assertEquals(
      arr.diffElements(Set[\kick, \hat], Set[])[\start].size,
      0,
      "Should start nothing when all are removed"
    );
  }

  test_diffElements_keepsCommon {
    var diff;
    arr = CCArrangement([]);
    diff = arr.diffElements(Set[\kick, \hat], Set[\kick, \bass]);
    this.assertEquals(diff[\start], Set[\bass], "Should start only new elements");
    this.assertEquals(diff[\stop], Set[\hat], "Should stop only removed elements");
  }

  // ========== status tests ==========

  test_status_notPlaying {
    arr = CCArrangement([]);
    this.assertEquals(arr.status, "Arrangement: stopped", "Should show stopped");
  }

  // ========== startElement tests ==========

  test_startElement_pdef {
    this.bootServer;
    arr = CCArrangement([]);
    Pdef(\testStart, Pbind(\type, \rest, \dur, 1));
    arr.startElement(\testStart);
    0.1.wait;
    this.assert(Pdef(\testStart).isPlaying, "Should start the Pdef");
    Pdef(\testStart).stop;
  }

  test_startElement_warnsMissing {
    arr = CCArrangement([]);
    // Should not throw, just warn
    arr.startElement(\nonexistent_element_xyz);
    this.assert(true, "Should warn but not throw for missing element");
  }

  // ========== startElement Ndef tests ==========

  test_startElement_ndef {
    this.bootServer;
    arr = CCArrangement([]);
    Ndef(\testNdefStart, { SinOsc.ar(440, 0, 0.0) });
    arr.startElement(\testNdefStart);
    0.1.wait;
    this.assert(Ndef(\testNdefStart).isPlaying, "Should play the Ndef");
    Ndef(\testNdefStart).stop;
  }

  test_startElement_ndef_playsToMainOut {
    this.bootServer;
    arr = CCArrangement([]);
    // Silent.ar, not SinOsc * 0: a zero-mul SinOsc folds to a control-rate constant, which can't be monitored
    Ndef(\testNdefBus, { Silent.ar(2) });
    arr.startElement(\testNdefBus);
    0.1.wait;
    this.assertEquals(Ndef(\testNdefBus).monitor.out, arr.mainOut, "Unrouted Ndef should play to main");
    Ndef(\testNdefBus).stop;
  }

  test_startElement_ndef_keepsRoutedOutput {
    this.bootServer;
    arr = CCArrangement([]);
    Ndef(\testNdefRouted, { Silent.ar(2) });
    Ndef(\testNdefRouted).initMonitor.out = 20;
    arr.startElement(\testNdefRouted);
    0.1.wait;
    this.assertEquals(Ndef(\testNdefRouted).monitor.out, 20, "Routed Ndef should keep its output");
    Ndef(\testNdefRouted).stop;
  }

  // ========== stopElement tests ==========

  test_stopElement_pdef {
    this.bootServer;
    arr = CCArrangement([]);
    Pdef(\testStop, Pbind(\type, \rest, \dur, 1)).play;
    0.1.wait;
    arr.stopElement(\testStop);
    this.assert(Pdef(\testStop).isPlaying.not, "Should stop the Pdef");
  }

  test_stopElement_ndef {
    this.bootServer;
    arr = CCArrangement([]);
    Ndef(\testNdefStop, { SinOsc.ar(440, 0, 0.0) }).play;
    0.1.wait;
    arr.stopElement(\testNdefStop);
    this.assert(Ndef(\testNdefStop).monitor.isPlaying.not, "Should stop the Ndef");
  }

  // ========== play/stop/current tests ==========

  test_play_setsCurrent {
    this.bootServer;
    arr = CCArrangement([[\intro, 1, []]]);
    arr.play;
    0.1.wait;
    this.assertEquals(CCArrangement.current, arr, "play should set current");
    arr.stop;
  }

  test_stop_clearsCurrent {
    this.bootServer;
    arr = CCArrangement([[\intro, 1, []]]);
    arr.play;
    0.1.wait;
    arr.stop;
    this.assertEquals(CCArrangement.current, nil, "stop should clear current");
  }

  test_stop_stopsActiveElements {
    this.bootServer;
    arr = CCArrangement([[\intro, 4, [\testActive]]]);
    Pdef(\testActive, Pbind(\type, \rest, \dur, 1));
    arr.play;
    0.5.wait;
    arr.stop;
    this.assert(Pdef(\testActive).isPlaying.not, "stop should stop active elements");
  }

  test_play_stopsExistingArrangement {
    var arr1, arr2;
    this.bootServer;
    arr1 = CCArrangement([[\sec, 8, []]]);
    arr2 = CCArrangement([[\sec, 8, []]]);
    arr1.play;
    0.1.wait;
    arr2.play;
    0.1.wait;
    this.assert(arr1.isPlaying.not, "First arrangement should be stopped");
    this.assertEquals(CCArrangement.current, arr2, "Current should be second arrangement");
    arr2.stop;
  }

  // ========== action tests ==========
  // Run the clock fast so a few beats pass quickly

  withFastClock { |func|
    var tempo = TempoClock.default.tempo;
    TempoClock.default.tempo = 20;
    protect(func) { TempoClock.default.tempo = tempo };
  }

  // Waits in seconds even when the test runs on the sped-up TempoClock.default
  waitSeconds { |seconds|
    if(thisThread.clock === TempoClock.default) {
      (seconds * TempoClock.default.tempo).wait;
    } {
      seconds.wait;
    };
  }

  test_action_runsEveryBeat {
    var beats = List[];
    this.withFastClock {
      arr = CCArrangement([[\a, 3, [], { |beat| beats.add(beat) }]], beatsPerBar: 1);
      arr.play;
      this.waitSeconds(0.5);
    };
    this.assertEquals(beats.asArray, [0, 1, 2], "Action should run on each beat, counting from 0");
  }

  test_action_beatResetsEachSection {
    var calls = List[];
    this.withFastClock {
      arr = CCArrangement([
        [\a, 2, [], { |beat| calls.add([\a, beat]) }],
        [\b, 2, [], { |beat| calls.add([\b, beat]) }]
      ], beatsPerBar: 1);
      arr.play;
      this.waitSeconds(0.5);
    };
    this.assertEquals(calls.asArray, [[\a, 0], [\a, 1], [\b, 0], [\b, 1]], "Beat should restart at 0 in each section");
  }

  test_action_receivesArrangement {
    var received;
    this.withFastClock {
      arr = CCArrangement([[\a, 1, [], { |beat, a| received = a }]], beatsPerBar: 1);
      arr.play;
      this.waitSeconds(0.3);
    };
    this.assert(received === arr, "Action should receive the arrangement");
  }

  test_finish_afterLastSection {
    this.withFastClock {
      arr = CCArrangement([[\a, 2, []]], beatsPerBar: 1);
      arr.play;
      this.waitSeconds(0.5);
    };
    this.assert(arr.isPlaying.not, "Should stop playing after the last section");
  }

  test_goto_stopsOldTimeline {
    var calls = List[];
    this.withFastClock {
      arr = CCArrangement([
        [\a, 8, [], { |beat| calls.add(\a) }],
        [\b, 8, [], { |beat| calls.add(\b) }]
      ], beatsPerBar: 1);
      arr.play;
      this.waitSeconds(0.15);
      arr.goto(\b);
      calls.clear;
      this.waitSeconds(0.3);
      arr.stop;
    };
    this.assert(calls.includes(\a).not, "After goto, the old section's action should stop running");
    this.assert(calls.includes(\b), "After goto, the new section's action should run");
  }

  // ========== volume tests ==========

  withVolume { |db, func|
    var vol = Server.default.volume;
    vol.volume = db;
    protect(func) { vol.lag = 0.1; vol.volume = 0 };
  }

  test_fade_isRelativeToStartVolume {
    this.bootServer;
    this.withVolume(-6) {
      arr = CCArrangement([[\a, 8, []]]);
      arr.play;
      arr.fade(0.5);
      this.assertFloatEquals(Server.default.volume.volume, -6 + 0.5.ampdb, "Fade should be relative to the starting volume", 0.01);
      arr.stop;
    };
  }

  test_stop_restoresVolume {
    this.bootServer;
    this.withVolume(-6) {
      arr = CCArrangement([[\a, 8, []]]);
      arr.play;
      arr.fade(0.1);
      arr.stop;
      this.assertFloatEquals(Server.default.volume.volume, -6, "stop should put the volume back", 0.01);
    };
  }

  test_finish_restoresVolume {
    this.bootServer;
    this.withVolume(-6) {
      this.withFastClock {
        arr = CCArrangement([[\a, 2, [], CCFade.out]], beatsPerBar: 1);
        arr.play;
        this.waitSeconds(0.5);
      };
      this.assertFloatEquals(Server.default.volume.volume, -6, "Finishing should put the volume back after a fade out", 0.01);
    };
  }

  test_goto_afterFinish_startsUp {
    this.bootServer;
    this.withVolume(-6) {
      this.withFastClock {
        arr = CCArrangement([[\a, 1, []], [\b, 8, [], CCFade.out]], beatsPerBar: 1);
        arr.play;
        this.waitSeconds(0.1);
        arr.stop;
        arr.goto(\b);
        this.waitSeconds(0.3);
        this.assertEquals(CCArrangement.current, arr, "goto on a stopped arrangement should make it current");
        this.assert(Server.default.volume.volume < -6.5, "goto on a stopped arrangement should still fade");
        arr.stop;
      };
      this.assertFloatEquals(Server.default.volume.volume, -6, "stop should put the volume back", 0.01);
    };
  }

  test_goto_undoesFade {
    this.bootServer;
    this.withVolume(-6) {
      arr = CCArrangement([[\a, 8, []], [\b, 8, []]]);
      arr.play;
      arr.fade(0.2);
      arr.goto(\b);
      this.assertFloatEquals(Server.default.volume.volume, -6, "goto should undo a fade in progress", 0.01);
      arr.stop;
    };
  }

  // ========== goto tests ==========

  test_goto_invalidSection {
    this.bootServer;
    arr = CCArrangement([[\intro, 4, []]]);
    arr.play;
    0.1.wait;
    arr.goto(\nonexistent);
    this.assert(arr.isPlaying, "goto invalid section should not stop arrangement");
    arr.stop;
  }
}
