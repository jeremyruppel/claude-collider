// CCBreakbeatTest - Unit tests for CCBreakbeat

CCBreakbeatTest : UnitTest {

  // ========== constructor tests ==========

  test_new_storesBuffer {
    var buf = (duration: 4.0);
    var b = CCBreakbeat(buf);
    this.assertEquals(b.buffer, buf, "Should store buffer");
  }

  test_new_storesNumSlices {
    var b = CCBreakbeat((duration: 4.0), 16);
    this.assertEquals(b.numSlices, 16, "Should store numSlices");
  }

  test_new_defaultsTo8Slices {
    var b = CCBreakbeat((duration: 4.0));
    this.assertEquals(b.numSlices, 8, "Should default to 8 slices");
  }

  // ========== slice position tests ==========

  test_sliceStart_firstSlice {
    var b = CCBreakbeat((duration: 4.0), 8);
    this.assertFloatEquals(b.sliceStart(0), 0, "First slice starts at 0");
  }

  test_sliceStart_middleSlice {
    var b = CCBreakbeat((duration: 4.0), 8);
    this.assertFloatEquals(b.sliceStart(4), 0.5, "Middle slice starts at 0.5");
  }

  test_sliceStart_lastSlice {
    var b = CCBreakbeat((duration: 4.0), 8);
    this.assertFloatEquals(b.sliceStart(7), 7/8, "Last slice starts at 7/8");
  }

  test_sliceEnd_firstSlice {
    var b = CCBreakbeat((duration: 4.0), 8);
    this.assertFloatEquals(b.sliceEnd(0), 1/8, "First slice ends at 1/8");
  }

  test_sliceEnd_lastSlice {
    var b = CCBreakbeat((duration: 4.0), 8);
    this.assertFloatEquals(b.sliceEnd(7), 1.0, "Last slice ends at 1.0");
  }

  test_slicePositions_16slices {
    var b = CCBreakbeat((duration: 4.0), 16);
    this.assertFloatEquals(b.sliceStart(0), 0, "16 slices: first starts at 0");
    this.assertFloatEquals(b.sliceEnd(0), 1/16, "16 slices: first ends at 1/16");
    this.assertFloatEquals(b.sliceStart(8), 0.5, "16 slices: middle starts at 0.5");
    this.assertFloatEquals(b.sliceEnd(15), 1.0, "16 slices: last ends at 1.0");
  }

  // ========== duration tests ==========

  test_bars_setsSliceDur {
    var b = CCBreakbeat((duration: 4.0), 8).bars(1);
    this.assertFloatEquals(b.sliceDur, 0.5, "1 bar / 8 slices = 0.5 beats each");
  }

  test_bars_twoBars {
    var b = CCBreakbeat((duration: 4.0), 8).bars(2);
    this.assertFloatEquals(b.sliceDur, 1.0, "2 bars / 8 slices = 1.0 beats each");
  }

  test_bars_customBeatsPerBar {
    var b = CCBreakbeat((duration: 4.0), 6).bars(1, 3);
    this.assertFloatEquals(b.sliceDur, 0.5, "1 bar of 3 beats / 6 slices = 0.5");
  }

  test_bars_returnsThis {
    var b = CCBreakbeat((duration: 4.0), 8);
    var result = b.bars(1);
    this.assert(result === b, "bars should return this for chaining");
  }

  test_dur_setsSliceDur {
    var b = CCBreakbeat((duration: 4.0), 8);
    b.dur = 0.25;
    this.assertFloatEquals(b.sliceDur, 0.25, "dur_ should set sliceDur directly");
  }

  // ========== pattern tests ==========

  test_pattern_returnsPbind {
    var b = CCBreakbeat((duration: 4.0), 4).bars(1);
    var pat = b.pattern([0, 1, 2, 3]);
    this.assert(pat.isKindOf(Pbind), "pattern should return a Pbind");
  }

  test_pattern_hasInstrumentKey {
    var b = CCBreakbeat((duration: 4.0), 4).bars(1);
    var pat = b.pattern([0, 1, 2, 3]);
    var stream = pat.asStream;
    var event = stream.next(());
    this.assertEquals(event[\instrument], \cc_breakbeat, "instrument should be cc_breakbeat");
  }

  test_pattern_hasBufferKey {
    var buf = "mockBuffer";
    var b = CCBreakbeat(buf, 4).bars(1);
    var pat = b.pattern([0, 1, 2, 3]);
    var stream = pat.asStream;
    var event = stream.next(());
    this.assertEquals(event[\buf], "mockBuffer", "buf should match constructor buffer");
  }

  test_pattern_hasDurKey {
    var b = CCBreakbeat((duration: 4.0), 4).bars(1);
    var pat = b.pattern([0, 1, 2, 3]);
    var stream = pat.asStream;
    var event = stream.next(());
    this.assertFloatEquals(event[\dur], 1.0, "dur should match sliceDur");
  }

  test_pattern_sliceStartPositions {
    var b = CCBreakbeat((duration: 4.0), 4).bars(1);
    var pat = b.pattern([0, 2, 1, 3]);
    var stream = pat.asStream;
    var events = 4.collect { stream.next(()) };
    this.assertFloatEquals(events[0][\start], 0, "Slice 0 starts at 0");
    this.assertFloatEquals(events[1][\start], 0.5, "Slice 2 starts at 0.5");
    this.assertFloatEquals(events[2][\start], 0.25, "Slice 1 starts at 0.25");
    this.assertFloatEquals(events[3][\start], 0.75, "Slice 3 starts at 0.75");
  }

  test_pattern_sliceEndPositions {
    var b = CCBreakbeat((duration: 4.0), 4).bars(1);
    var pat = b.pattern([0, 2, 1, 3]);
    var stream = pat.asStream;
    var events = 4.collect { stream.next(()) };
    this.assertFloatEquals(events[0][\end], 0.25, "Slice 0 ends at 0.25");
    this.assertFloatEquals(events[1][\end], 0.75, "Slice 2 ends at 0.75");
    this.assertFloatEquals(events[2][\end], 0.5, "Slice 1 ends at 0.5");
    this.assertFloatEquals(events[3][\end], 1.0, "Slice 3 ends at 1.0");
  }

  // ========== negative index tests ==========

  test_pattern_negativeIndex_usesAbsForPosition {
    var b = CCBreakbeat((duration: 4.0), 4).bars(1);
    var pat = b.pattern([0, -2]);
    var stream = pat.asStream;
    var events = 2.collect { stream.next(()) };
    this.assertFloatEquals(events[1][\start], 0.5, "Negative index should use abs for start position");
    this.assertFloatEquals(events[1][\end], 0.75, "Negative index should use abs for end position");
  }

  test_pattern_negativeIndex_reversesRate {
    var b = CCBreakbeat((duration: 4.0), 4).bars(1);
    var pat = b.pattern([0, -2]);
    var stream = pat.asStream;
    var events = 2.collect { stream.next(()) };
    this.assertFloatEquals(events[0][\rate], 1, "Positive index should have positive rate");
    this.assertFloatEquals(events[1][\rate], -1, "Negative index should have negative rate");
  }

  test_pattern_negativeIndex_withRateArray {
    var b = CCBreakbeat((duration: 4.0), 4).bars(1);
    var pat = b.pattern([0, -1], rate: [2, 1.5]);
    var stream = pat.asStream;
    var events = 2.collect { stream.next(()) };
    this.assertFloatEquals(events[0][\rate], 2, "Positive index keeps array rate");
    this.assertFloatEquals(events[1][\rate], -1.5, "Negative index negates array rate");
  }

  // ========== quant tests ==========

  test_quant_fullOrder {
    var b = CCBreakbeat((duration: 4.0), 8).bars(1);
    this.assertFloatEquals(b.quant((0..7)), 4.0, "8 slices * 0.5 dur = 4 beats");
  }

  test_quant_partialOrder {
    var b = CCBreakbeat((duration: 4.0), 8).bars(1);
    this.assertFloatEquals(b.quant([0, 1, 2, 3]), 2.0, "4 slices * 0.5 dur = 2 beats");
  }

  test_quant_twoBars {
    var b = CCBreakbeat((duration: 4.0), 16).bars(2);
    this.assertFloatEquals(b.quant((0..15)), 8.0, "16 slices * 0.5 dur = 8 beats");
  }

  // ========== pattern loops ==========

  test_pattern_loops {
    var b = CCBreakbeat((duration: 4.0), 4).bars(1);
    var pat = b.pattern([0, 1]);
    var stream = pat.asStream;
    var events = 4.collect { stream.next(()) };
    this.assertFloatEquals(events[2][\start], 0, "Pattern should loop: 3rd event = slice 0");
    this.assertFloatEquals(events[3][\start], 0.25, "Pattern should loop: 4th event = slice 1");
  }
}
