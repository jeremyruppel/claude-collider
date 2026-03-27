// CCMelodyTest - Unit tests for CCMelody

CCMelodyTest : UnitTest {

	// ========== callAndResponse ==========

	test_callAndResponse_alternates {
		var call = CCMotif([0, 2]);
		var resp = CCMotif([4, 3]);
		var p = CCMelody.callAndResponse(call, resp, 1);
		var result = p.asStream.nextN(4, ());
		this.assertEquals(result, [0, 2, 4, 3], "Should alternate call then response");
	}

	test_callAndResponse_repeats {
		var call = CCMotif([0, 2]);
		var resp = CCMotif([4, 3]);
		var p = CCMelody.callAndResponse(call, resp, 2);
		var result = p.asStream.nextN(8, ());
		this.assertEquals(result, [0, 2, 4, 3, 0, 2, 4, 3], "Should repeat call/response");
	}

	test_callAndResponse_finiteEnds {
		var call = CCMotif([0]);
		var resp = CCMotif([1]);
		var p = CCMelody.callAndResponse(call, resp, 1);
		var stream = p.asStream;
		stream.nextN(2, ());
		this.assertEquals(stream.next(()), nil, "Should end after finite repeats");
	}

	// ========== develop ==========

	test_develop_structure {
		var m = CCMotif([0, 2]);
		var p = CCMelody.develop(m, 1);
		var result = p.asStream.nextN(8, ());
		// state, state, transpose(2), state
		this.assertEquals(result, [0, 2, 0, 2, 2, 4, 0, 2], "Should develop: AA'A");
	}

	test_develop_repeats {
		var m = CCMotif([0]);
		var p = CCMelody.develop(m, 2);
		// Each cycle: state(0), state(0), transpose2(2), state(0) = 4 values
		var result = p.asStream.nextN(8, ());
		this.assertEquals(result, [0, 0, 2, 0, 0, 0, 2, 0], "Should repeat development");
	}

	test_develop_finiteEnds {
		var m = CCMotif([0]);
		var p = CCMelody.develop(m, 1);
		var stream = p.asStream;
		stream.nextN(4, ());
		this.assertEquals(stream.next(()), nil, "Should end after finite repeats");
	}
}
