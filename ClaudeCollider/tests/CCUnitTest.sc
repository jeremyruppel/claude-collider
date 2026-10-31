// CCUnitTest - UnitTest base class that keeps the suite running when a test throws.
// UnitTest runs setUp/test/tearDown unprotected, so one error kills the whole run
// and skips cleanup. Here an error is recorded as a failure and tearDown always runs.

CCUnitTest : UnitTest {

  runTestMethod { |method, report = true|
    this.class.forkIfNeeded {
      currentMethod = method;
      if(this.guard(method, "setUp") { this.setUp }) {
        this.guard(method, "test") { this.perform(method.name) };
      };
      this.guard(method, "tearDown") { this.tearDown };
      if(report) { this.class.report };
    }
  }

  // Run func; on error, record a failure and return false
  guard { |method, stage, func|
    var ok = true;
    func.try { |error|
      ok = false;
      this.failed(method, "% threw %: %".format(stage, error.class.name, error.errorString));
    };
    ^ok;
  }
}
