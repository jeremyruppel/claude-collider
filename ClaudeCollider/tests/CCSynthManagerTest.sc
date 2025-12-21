// CCSynthManagerTest - Unit tests for CCSynthManager

CCSynthManagerTest : UnitTest {
  var manager;

  setUp {
    manager = CCSynthManager(\testSynth);
  }

  tearDown {
    manager.releaseAll;
  }

  // ========== init tests ==========

  test_init_synthName {
    this.assertEquals(manager.synthName, \testSynth, "Should store synth name");
  }

  test_init_polyMode {
    this.assertEquals(manager.mono, false, "Should default to poly mode");
  }

  test_init_noActiveNotes {
    this.assertEquals(manager.activeNotes.size, 0, "Should start with no active notes");
  }

  // ========== synthName setter ==========

  test_synthName_setter {
    manager.synthName = \newSynth;
    this.assertEquals(manager.synthName, \newSynth, "Should update synth name");
  }

  // ========== mono setter ==========

  test_mono_setter {
    manager.mono = true;
    this.assertEquals(manager.mono, true, "Should update mono mode");
  }

  test_mono_switchReleasesNotes {
    // This would require a running server to fully test
    // Just test that the mode changes
    manager.mono = true;
    manager.mono = false;
    this.assertEquals(manager.mono, false, "Should toggle mono mode");
  }

  // ========== activeNotes tests ==========

  test_activeNotes_emptyPoly {
    this.assertEquals(manager.activeNotes, [], "Poly mode should return empty array");
  }

  test_activeNotes_emptyMono {
    manager.mono = true;
    this.assertEquals(manager.activeNotes, [], "Mono mode should return empty array");
  }

  // ========== hasActiveNotes tests ==========

  test_hasActiveNotes_false {
    this.assertEquals(manager.hasActiveNotes, false, "Should return false when no notes");
  }

  // ========== releaseAll tests ==========

  test_releaseAll_poly {
    manager.releaseAll;
    this.assertEquals(manager.activeNotes.size, 0, "Should have no active notes after releaseAll");
  }

  test_releaseAll_mono {
    manager.mono = true;
    manager.releaseAll;
    this.assertEquals(manager.activeNotes.size, 0, "Should have no active notes after releaseAll");
  }

  // ========== status tests ==========

  test_status_containsSynth {
    var status = manager.status;
    this.assertEquals(status[\synth], \testSynth, "Status should contain synth name");
  }

  test_status_containsMono {
    var status = manager.status;
    this.assertEquals(status[\mono], false, "Status should contain mono flag");
  }

  test_status_containsActiveNotes {
    var status = manager.status;
    this.assert(status[\activeNotes].notNil, "Status should contain activeNotes");
  }
}
