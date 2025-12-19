// CCFXTest - Unit tests for CCFX

CCFXTest : UnitTest {
  var fx, mockCC;

  setUp {
    mockCC = this.createMockCC;
    fx = CCFX(mockCC);
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

  test_describe_formatsEffectWithParams {
    var result;
    fx.instVarPut(\defs, Dictionary[
      \testfx -> (
        description: "A test effect",
        params: [\mix, 0.5, \rate, 1.0]
      )
    ]);
    result = fx.describe;

    this.assertEquals(
      result,
      "testfx - A test effect\n  params: mix (default: 0.5), rate (default: 1.0)",
      "describe should format effect with name, description, and params"
    );
  }

  test_describe_sortsEffectsAlphabetically {
    var result;
    fx.instVarPut(\defs, Dictionary[
      \zebra -> (description: "Z effect", params: [\mix, 0.5]),
      \alpha -> (description: "A effect", params: [\dry, 1.0])
    ]);
    result = fx.describe;

    this.assertEquals(
      result,
      "alpha - A effect\n  params: dry (default: 1.0)\nzebra - Z effect\n  params: mix (default: 0.5)",
      "describe should sort effects alphabetically"
    );
  }

  // ========== list tests ==========

  test_list_returnsCommaSeparatedString {
    var result;
    fx.instVarPut(\defs, Dictionary[
      \reverb -> (description: "Reverb effect", params: []),
      \delay -> (description: "Delay effect", params: [])
    ]);
    result = fx.list;
    this.assertEquals(result, "delay, reverb", "list should return comma-separated names");
  }

  test_list_isSorted {
    var result;
    fx.instVarPut(\defs, Dictionary[
      \zebra -> (description: "Z effect", params: []),
      \alpha -> (description: "A effect", params: []),
      \middle -> (description: "M effect", params: [])
    ]);
    result = fx.list;
    this.assertEquals(result, "alpha, middle, zebra", "list should be sorted alphabetically");
  }

  test_list_emptyReturnsNone {
    var result;
    fx.instVarPut(\defs, Dictionary[]);
    result = fx.list;
    this.assertEquals(result, "(none)", "list should return (none) when empty");
  }
}
