// ClaudeCollider Test Runner
// Usage: sclang ClaudeCollider/tests/run_tests.scd

(
// Backstop: exit with failure if the suite hangs (e.g. a server that never answers)
~testTimeout = 180;
AppClock.sched(~testTimeout, {
  "FAILED: test run timed out after % seconds".format(~testTimeout).postln;
  1.exit;
  nil
});

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
