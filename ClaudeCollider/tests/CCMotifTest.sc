// CCMotifTest - Unit tests for CCMotif

CCMotifTest : UnitTest {

	// ========== constructor tests ==========

	test_new_storesDegrees {
		var m = CCMotif([0, 2, 4]);
		this.assertEquals(m.degrees, [0, 2, 4], "Should store degrees array");
	}

	test_new_preservesRests {
		var m = CCMotif([0, Rest(), 2]);
		this.assert(m.degrees[1].isRest, "Should preserve Rest in degrees");
	}

	// ========== storeArgs ==========

	test_storeArgs_returnsDegrees {
		var m = CCMotif([0, 2, 4]);
		this.assertEquals(m.storeArgs, [[0, 2, 4]], "Should return degrees in array");
	}

	// ========== embedInStream ==========

	test_embedInStream_yieldsAllDegrees {
		var m = CCMotif([0, 2, 4]);
		var stream = m.asStream;
		var result = stream.nextN(3, ());
		this.assertEquals(result, [0, 2, 4], "Should yield all degrees");
	}

	test_embedInStream_yieldsRests {
		var m = CCMotif([0, Rest(), 2]);
		var stream = m.asStream;
		var results = stream.nextN(3, ());
		this.assertEquals(results[0], 0, "First should be 0");
		this.assert(results[1].isRest, "Second should be Rest");
		this.assertEquals(results[2], 2, "Third should be 2");
	}

	test_embedInStream_finishesAfterDegrees {
		var m = CCMotif([0, 2]);
		var stream = m.asStream;
		stream.nextN(2, ());
		this.assertEquals(stream.next(()), nil, "Should return nil after all degrees");
	}

	test_embedInStream_worksInPseq {
		var m1 = CCMotif([0, 1]);
		var m2 = CCMotif([2, 3]);
		var stream = Pseq([m1, m2]).asStream;
		var result = stream.nextN(4, ());
		this.assertEquals(result, [0, 1, 2, 3], "Should sequence motifs in Pseq");
	}

	test_embedInStream_worksWithPn {
		var m = CCMotif([0, 1]);
		var stream = Pn(m, 2).asStream;
		var result = stream.nextN(4, ());
		this.assertEquals(result, [0, 1, 0, 1], "Should repeat motif with Pn");
	}

	// ========== transpose ==========

	test_transpose_shiftsDegrees {
		var m = CCMotif([0, 2, 4]);
		var t = m.transpose(3);
		this.assertEquals(t.degrees, [3, 5, 7], "Should shift all degrees by n");
	}

	test_transpose_preservesRests {
		var m = CCMotif([0, Rest(), 2]);
		var t = m.transpose(3);
		this.assertEquals(t.degrees[0], 3, "Should shift non-Rest");
		this.assert(t.degrees[1].isRest, "Should preserve Rest");
		this.assertEquals(t.degrees[2], 5, "Should shift non-Rest");
	}

	test_transpose_returnsNewMotif {
		var m = CCMotif([0, 2, 4]);
		var t = m.transpose(3);
		this.assertEquals(m.degrees, [0, 2, 4], "Original should be unchanged");
	}

	test_transpose_negativeShift {
		var m = CCMotif([4, 5, 6]);
		var t = m.transpose(-2);
		this.assertEquals(t.degrees, [2, 3, 4], "Should handle negative shift");
	}

	// ========== invert ==========

	test_invert_mirrorsAroundFirstNote {
		var m = CCMotif([2, 4, 6]);
		var inv = m.invert;
		// pivot=2: 2→2, 4→0, 6→-2
		this.assertEquals(inv.degrees, [2, 0, -2], "Should mirror around first note");
	}

	test_invert_preservesRests {
		var m = CCMotif([0, Rest(), 4]);
		var inv = m.invert;
		this.assertEquals(inv.degrees[0], 0, "First note unchanged (it's the pivot)");
		this.assert(inv.degrees[1].isRest, "Should preserve Rest");
		this.assertEquals(inv.degrees[2], -4, "Should invert non-Rest");
	}

	test_invert_pivotOnZero {
		var m = CCMotif([0, 2, 4]);
		var inv = m.invert;
		// pivot=0: 0→0, 2→-2, 4→-4
		this.assertEquals(inv.degrees, [0, -2, -4], "Should mirror around zero");
	}

	test_invert_allRests {
		var m = CCMotif([Rest(), Rest()]);
		var inv = m.invert;
		this.assert(inv.degrees[0].isRest, "Should preserve Rest");
		this.assert(inv.degrees[1].isRest, "Should preserve Rest");
	}

	test_invert_skipsLeadingRests {
		var m = CCMotif([Rest(), 3, 5]);
		var inv = m.invert;
		// pivot=3: Rest→Rest, 3→3, 5→1
		this.assert(inv.degrees[0].isRest, "Should preserve leading Rest");
		this.assertEquals(inv.degrees[1], 3, "Pivot should be unchanged");
		this.assertEquals(inv.degrees[2], 1, "Should invert around pivot");
	}

	// ========== retrograde ==========

	test_retrograde_reversesOrder {
		var m = CCMotif([0, 2, 4]);
		var r = m.retrograde;
		this.assertEquals(r.degrees, [4, 2, 0], "Should reverse degree order");
	}

	test_retrograde_preservesRests {
		var m = CCMotif([0, Rest(), 4]);
		var r = m.retrograde;
		this.assertEquals(r.degrees[0], 4, "First should be last original");
		this.assert(r.degrees[1].isRest, "Middle Rest stays in middle");
		this.assertEquals(r.degrees[2], 0, "Last should be first original");
	}

	test_retrograde_returnsNewMotif {
		var m = CCMotif([0, 2, 4]);
		m.retrograde;
		this.assertEquals(m.degrees, [0, 2, 4], "Original should be unchanged");
	}

	// ========== extend ==========

	test_extend_appendsDegrees {
		var m = CCMotif([0, 2]);
		var e = m.extend([4, 6]);
		this.assertEquals(e.degrees, [0, 2, 4, 6], "Should append extra degrees");
	}

	test_extend_preservesOriginal {
		var m = CCMotif([0, 2]);
		m.extend([4, 6]);
		this.assertEquals(m.degrees, [0, 2], "Original should be unchanged");
	}

	// ========== truncate ==========

	test_truncate_takesFirstN {
		var m = CCMotif([0, 2, 4, 6]);
		var t = m.truncate(2);
		this.assertEquals(t.degrees, [0, 2], "Should take first n degrees");
	}

	test_truncate_preservesOriginal {
		var m = CCMotif([0, 2, 4, 6]);
		m.truncate(2);
		this.assertEquals(m.degrees, [0, 2, 4, 6], "Original should be unchanged");
	}

	// ========== chaining ==========

	test_chaining_transposeAndInvert {
		var m = CCMotif([0, 2, 4]);
		var result = m.transpose(1).invert;
		// transpose(1): [1, 3, 5]
		// invert (pivot=1): 1→1, 3→-1, 5→-3
		this.assertEquals(result.degrees, [1, -1, -3], "Should chain transpose then invert");
	}

	test_chaining_retrogradeAndExtend {
		var m = CCMotif([0, 2, 4]);
		var result = m.retrograde.extend([7]);
		this.assertEquals(result.degrees, [4, 2, 0, 7], "Should chain retrograde then extend");
	}
}
