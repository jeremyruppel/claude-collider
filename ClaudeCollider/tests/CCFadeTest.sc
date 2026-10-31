// CCFadeTest - Unit tests for CCFade

CCFadeTest : CCUnitTest {

  // Stands in for a CCArrangement: 2 bars of 2 beats, recording each fade call
  mockArrangement { |levels|
    ^(
      sectionBars: 2,
      beatsPerBar: 2,
      fade: { |self, level, lag| levels.add(level) }
    );
  }

  test_in_goesFromZeroToOne {
    var fade = CCFade.in(\lin);
    this.assertEquals(fade.at(0), 0, "Fade in should start at 0");
    this.assertEquals(fade.at(1), 1, "Fade in should end at 1");
  }

  test_out_goesFromOneToZero {
    var fade = CCFade.out(\lin);
    this.assertEquals(fade.at(0), 1, "Fade out should start at 1");
    this.assertEquals(fade.at(1), 0, "Fade out should end at 0");
  }

  test_new_usesFromAndTo {
    this.assertFloatEquals(CCFade(1, 0.5, \lin).at(0.5), 0.75, "Should ease between from and to");
  }

  test_curve_shapesTheFade {
    this.assert(CCFade.in(4).at(0.5) < 0.5, "A positive curve should start slow");
  }

  test_value_reachesTargetOnLastBeat {
    var levels = List[];
    var arr = this.mockArrangement(levels);
    4.do { |beat| CCFade.in(\lin).value(beat, arr) };
    this.assertEquals(levels.asArray, [0.25, 0.5, 0.75, 1], "Should step through the section, ending at the target");
  }
}
