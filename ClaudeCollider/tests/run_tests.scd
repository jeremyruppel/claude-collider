// ClaudeCollider Test Runner
// Usage: sclang ClaudeCollider/tests/run_tests.scd

(
fork {
  UnitTest.reportPasses = true;
  CCFormatterTest.run;
  CCFXTest.run;
  CCSynthsTest.run;
  CCSamplesTest.run;
  1.wait;
  0.exit;
};
)
