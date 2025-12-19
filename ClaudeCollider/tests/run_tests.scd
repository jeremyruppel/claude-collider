// ClaudeCollider Test Runner
// Usage: sclang ClaudeCollider/tests/run_tests.scd

(
fork {
  UnitTest.reportPasses = true;
  CCFormatterTest.run;
  0.1.wait;
  0.exit;
};
)
