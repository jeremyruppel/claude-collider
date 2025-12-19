// ClaudeCollider Test Runner
// Usage: sclang ClaudeCollider/tests/run_tests.scd

(
fork {
  UnitTest.runAll;
  2.wait;
  if(UnitTest.failures.size > 0) {
    "FAILED: % test(s) failed".format(UnitTest.failures.size).postln;
    1.exit;
  } {
    "All tests passed".postln;
    0.exit;
  };
};
)
