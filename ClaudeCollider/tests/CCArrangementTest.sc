// CCArrangementTest - Unit tests for CCArrangement

CCArrangementTest : UnitTest {
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
    Pdef(\testStart, Pbind(\dur, 1));
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

  test_startElement_ndef_routesToMainOutput {
    this.bootServer;
    arr = CCArrangement([]);
    Ndef(\testNdefBus, { SinOsc.ar(440, 0, 0.0) });
    arr.startElement(\testNdefBus);
    0.1.wait;
    this.assertEquals(Ndef(\testNdefBus).monitor.out, 0, "Should route Ndef to bus 0");
    Ndef(\testNdefBus).stop;
  }

  // ========== stopElement tests ==========

  test_stopElement_pdef {
    this.bootServer;
    arr = CCArrangement([]);
    Pdef(\testStop, Pbind(\dur, 1)).play;
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
    this.assert(Ndef(\testNdefStop).isPlaying.not, "Should stop the Ndef");
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
    Pdef(\testActive, Pbind(\dur, 1));
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
