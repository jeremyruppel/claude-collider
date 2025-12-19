// CCSynthsTest - Unit tests for CCSynths

CCSynthsTest : UnitTest {
  var synths, mockCC;

  setUp {
    mockCC = this.createMockCC;
    synths = CCSynths(mockCC);
  }

  tearDown {
  }

  createMockCC {
    ^(
      server: (
        serverRunning: true
      )
    );
  }

  // ========== describe tests ==========

  test_describe_formatsSynthWithParams {
    var result;
    synths.instVarPut(\defs, Dictionary[
      \testsynth -> (
        description: "A test synth",
        def: { SynthDef(\cc_testsynth, { |out=0, freq=440, amp=0.5| }) }
      )
    ]);
    result = synths.describe;

    this.assertEquals(
      result,
      "testsynth - A test synth\n  params: out, freq, amp",
      "describe should format synth with name, description, and params"
    );
  }

  test_describe_sortsSynthsAlphabetically {
    var result;
    synths.instVarPut(\defs, Dictionary[
      \zebra -> (
        description: "Z synth",
        def: { SynthDef(\cc_zebra, { |out=0| }) }
      ),
      \alpha -> (
        description: "A synth",
        def: { SynthDef(\cc_alpha, { |out=0| }) }
      )
    ]);
    result = synths.describe;

    this.assertEquals(
      result,
      "alpha - A synth\n  params: out\nzebra - Z synth\n  params: out",
      "describe should sort synths alphabetically"
    );
  }

  test_describe_eachDefHasDescription {
    synths.defs.keysValuesDo { |name, entry|
      this.assert(
        entry[\description].isString,
        "% should have a string description".format(name)
      );
      this.assert(
        entry[\description].size > 0,
        "% description should not be empty".format(name)
      );
    };
  }

  test_describe_eachDefHasSynthDefFunction {
    synths.defs.keysValuesDo { |name, entry|
      this.assert(
        entry[\def].isFunction,
        "% should have a function that creates SynthDef".format(name)
      );
    };
  }

  // ========== list tests ==========

  test_list_returnsCommaSeparatedString {
    var result;
    synths.instVarPut(\defs, Dictionary[
      \kick -> (description: "Kick drum"),
      \snare -> (description: "Snare drum")
    ]);
    result = synths.list;
    this.assertEquals(result, "kick, snare", "list should return comma-separated names");
  }

  test_list_isSorted {
    var result;
    synths.instVarPut(\defs, Dictionary[
      \zebra -> (description: "Z synth"),
      \alpha -> (description: "A synth"),
      \middle -> (description: "M synth")
    ]);
    result = synths.list;
    this.assertEquals(result, "alpha, middle, zebra", "list should be sorted alphabetically");
  }

  test_list_emptyReturnsNone {
    var result;
    synths.instVarPut(\defs, Dictionary[]);
    result = synths.list;
    this.assertEquals(result, "(none)", "list should return (none) when empty");
  }
}
