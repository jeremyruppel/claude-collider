// CCPhraseTest - Unit tests for CCPhrase

CCPhraseTest : UnitTest {

	// ========== constructor tests ==========

	test_new_storesMotif {
		var m = CCMotif([0, 2, 4]);
		var p = CCPhrase(m, [\state]);
		this.assertEquals(p.motif, m, "Should store motif");
	}

	// ========== parsePlan ==========

	test_parsePlan_stateNoArg {
		var m = CCMotif([0]);
		var p = CCPhrase(m, [\state]);
		this.assertEquals(p.steps, [[\state, nil]], "Should parse state without arg");
	}

	test_parsePlan_transposeWithArg {
		var m = CCMotif([0]);
		var p = CCPhrase(m, [\transpose, 3]);
		this.assertEquals(p.steps, [[\transpose, 3]], "Should parse transpose with arg");
	}

	test_parsePlan_invertNoArg {
		var m = CCMotif([0]);
		var p = CCPhrase(m, [\invert]);
		this.assertEquals(p.steps, [[\invert, nil]], "Should parse invert without arg");
	}

	test_parsePlan_retrogradeNoArg {
		var m = CCMotif([0]);
		var p = CCPhrase(m, [\retrograde]);
		this.assertEquals(p.steps, [[\retrograde, nil]], "Should parse retrograde without arg");
	}

	test_parsePlan_extendWithArg {
		var m = CCMotif([0]);
		var p = CCPhrase(m, [\extend, [3, 4]]);
		this.assertEquals(p.steps[0][0], \extend, "Should parse extend op");
		this.assertEquals(p.steps[0][1], [3, 4], "Should parse extend arg");
	}

	test_parsePlan_mixedOps {
		var m = CCMotif([0]);
		var p = CCPhrase(m, [\state, \transpose, 2, \invert]);
		this.assertEquals(p.steps.size, 3, "Should parse all three ops");
		this.assertEquals(p.steps[0], [\state, nil], "First op should be state");
		this.assertEquals(p.steps[1], [\transpose, 2], "Second op should be transpose");
		this.assertEquals(p.steps[2], [\invert, nil], "Third op should be invert");
	}

	// ========== embedInStream ==========

	test_embedInStream_statePlaysOriginal {
		var m = CCMotif([0, 2, 4]);
		var p = CCPhrase(m, [\state], 1);
		var result = p.asStream.nextN(3, ());
		this.assertEquals(result, [0, 2, 4], "State should play original motif");
	}

	test_embedInStream_transposePlaysShifted {
		var m = CCMotif([0, 2, 4]);
		var p = CCPhrase(m, [\transpose, 3], 1);
		var result = p.asStream.nextN(3, ());
		this.assertEquals(result, [3, 5, 7], "Transpose should shift degrees");
	}

	test_embedInStream_multipleSteps {
		var m = CCMotif([0, 2]);
		var p = CCPhrase(m, [\state, \transpose, 3], 1);
		var result = p.asStream.nextN(4, ());
		this.assertEquals(result, [0, 2, 3, 5], "Should sequence state then transposed");
	}

	test_embedInStream_repeatsOnce {
		var m = CCMotif([0, 2]);
		var p = CCPhrase(m, [\state], 1);
		var stream = p.asStream;
		stream.nextN(2, ());
		this.assertEquals(stream.next(()), nil, "Should end after one repeat");
	}

	test_embedInStream_repeatsMultiple {
		var m = CCMotif([0, 2]);
		var p = CCPhrase(m, [\state], 2);
		var result = p.asStream.nextN(4, ());
		this.assertEquals(result, [0, 2, 0, 2], "Should repeat phrase twice");
	}

	test_embedInStream_invertPlaysInverted {
		var m = CCMotif([0, 2, 4]);
		var p = CCPhrase(m, [\invert], 1);
		var result = p.asStream.nextN(3, ());
		this.assertEquals(result, [0, -2, -4], "Invert should mirror around pivot");
	}

	test_embedInStream_retrogradePlaysReversed {
		var m = CCMotif([0, 2, 4]);
		var p = CCPhrase(m, [\retrograde], 1);
		var result = p.asStream.nextN(3, ());
		this.assertEquals(result, [4, 2, 0], "Retrograde should reverse");
	}

	// ========== development sequences ==========

	test_development_stateStateTranspose {
		var m = CCMotif([0, 2]);
		var p = CCPhrase(m, [\state, \state, \transpose, 2], 1);
		var result = p.asStream.nextN(6, ());
		this.assertEquals(result, [0, 2, 0, 2, 2, 4], "Should develop: state, state, transpose");
	}

	// ========== integration ==========

	test_worksInPseq {
		var m = CCMotif([0, 1]);
		var p1 = CCPhrase(m, [\state], 1);
		var p2 = CCPhrase(m, [\transpose, 5], 1);
		var result = Pseq([p1, p2]).asStream.nextN(4, ());
		this.assertEquals(result, [0, 1, 5, 6], "Should work inside Pseq");
	}
}
