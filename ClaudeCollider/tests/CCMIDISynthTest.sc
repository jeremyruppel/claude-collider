// CCMIDISynthTest - Unit tests for CCMIDISynth

CCMIDISynthTest : UnitTest {
  var midiSynth;

  setUp {
    midiSynth = CCMIDISynth(\testSynth);
  }

  tearDown {
    midiSynth.free;
  }

  // ========== init tests ==========

  test_init_synthName {
    this.assertEquals(midiSynth.synthName, \testSynth, "Should store synth name");
  }

  test_init_polyMode {
    this.assertEquals(midiSynth.mono, false, "Should default to poly mode");
  }

  test_init_allChannels {
    this.assertEquals(midiSynth.channel, nil, "Should default to all channels");
  }

  test_init_velToAmp {
    this.assertEquals(midiSynth.velToAmp, true, "Should default to velocity->amp");
  }

  test_init_disabled {
    this.assertEquals(midiSynth.enabled, false, "Should start disabled");
  }

  test_init_noCCMappings {
    this.assertEquals(midiSynth.ccMappings.size, 0, "Should start with no CC mappings");
  }

  // ========== synthName setter ==========

  test_synthName_setter {
    midiSynth.synthName = \newSynth;
    this.assertEquals(midiSynth.synthName, \newSynth, "Should update synth name");
  }

  // ========== mono setter ==========

  test_mono_setter {
    midiSynth.mono = true;
    this.assertEquals(midiSynth.mono, true, "Should update mono mode");
  }

  // ========== mapCC tests ==========

  test_mapCC_simple {
    midiSynth.mapCC(1, \cutoff);
    this.assertEquals(midiSynth.ccMappings.size, 1, "Should add CC mapping");
  }

  test_mapCC_storesParam {
    midiSynth.mapCC(1, \cutoff);
    this.assertEquals(midiSynth.ccMappings[1][\param], \cutoff, "Should store param name");
  }

  test_mapCC_defaultRange {
    midiSynth.mapCC(1, \cutoff);
    this.assertEquals(midiSynth.ccMappings[1][\range], [0, 1], "Should default to 0-1 range");
  }

  test_mapCC_customRange {
    midiSynth.mapCC(1, \cutoff, [200, 8000]);
    this.assertEquals(midiSynth.ccMappings[1][\range], [200, 8000], "Should store custom range");
  }

  test_mapCC_defaultCurve {
    midiSynth.mapCC(1, \cutoff);
    this.assertEquals(midiSynth.ccMappings[1][\curve], \lin, "Should default to linear curve");
  }

  test_mapCC_expCurve {
    midiSynth.mapCC(1, \cutoff, [200, 8000], \exp);
    this.assertEquals(midiSynth.ccMappings[1][\curve], \exp, "Should store exp curve");
  }

  test_mapCC_initializesCCValue {
    midiSynth.mapCC(1, \cutoff, [0, 100]);
    this.assertEquals(midiSynth.ccValues[\cutoff], 50, "Should init to midpoint");
  }

  // ========== unmapCC tests ==========

  test_unmapCC_removesMapping {
    midiSynth.mapCC(1, \cutoff);
    midiSynth.unmapCC(1);
    this.assertEquals(midiSynth.ccMappings[1], nil, "Should remove mapping");
  }

  // ========== clearCCMappings tests ==========

  test_clearCCMappings_clearsAll {
    midiSynth.mapCC(1, \cutoff);
    midiSynth.mapCC(74, \res);
    midiSynth.clearCCMappings;
    this.assertEquals(midiSynth.ccMappings.size, 0, "Should clear all mappings");
  }

  test_clearCCMappings_clearsCCValues {
    midiSynth.mapCC(1, \cutoff);
    midiSynth.clearCCMappings;
    this.assertEquals(midiSynth.ccValues.size, 0, "Should clear CC values");
  }

  // ========== enable/disable tests ==========

  test_enable_setsEnabled {
    midiSynth.enable;
    this.assertEquals(midiSynth.enabled, true, "Should set enabled to true");
  }

  test_disable_setsDisabled {
    midiSynth.enable;
    midiSynth.disable;
    this.assertEquals(midiSynth.enabled, false, "Should set enabled to false");
  }

  test_free_disables {
    midiSynth.enable;
    midiSynth.free;
    this.assertEquals(midiSynth.enabled, false, "Should disable on free");
  }

  test_free_clearsMappings {
    midiSynth.mapCC(1, \cutoff);
    midiSynth.free;
    this.assertEquals(midiSynth.ccMappings.size, 0, "Should clear mappings on free");
  }

  // ========== status tests ==========

  test_status_containsSynth {
    var status = midiSynth.status;
    this.assertEquals(status[\synth], \testSynth, "Status should contain synth name");
  }

  test_status_containsChannel {
    var status = midiSynth.status;
    this.assertEquals(status[\channel], "all", "Status should contain channel");
  }

  test_status_containsMono {
    var status = midiSynth.status;
    this.assertEquals(status[\mono], false, "Status should contain mono flag");
  }

  test_status_containsEnabled {
    var status = midiSynth.status;
    this.assertEquals(status[\enabled], false, "Status should contain enabled flag");
  }

  test_status_containsCCMappings {
    var status = midiSynth.status;
    this.assert(status[\ccMappings].notNil, "Status should contain ccMappings");
  }
}
