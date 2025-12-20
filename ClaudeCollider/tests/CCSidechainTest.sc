// CCSidechainTest - Unit tests for CCSidechain

CCSidechainTest : UnitTest {
  var sidechain, mockCC;

  setUp {
    mockCC = this.createMockCC;
    sidechain = CCSidechain(mockCC);
  }

  tearDown {
    // Clean up
  }

  createMockCC {
    ^(
      server: Server.default
    );
  }

  // ========== init tests ==========

  test_init_emptySidechains {
    this.assertEquals(sidechain.size, 0, "Should start with no sidechains");
  }

  // ========== at tests ==========

  test_at_notFound {
    this.assertEquals(sidechain.at(\nonexistent), nil, "Should return nil for unknown sidechain");
  }

  // ========== size tests ==========

  test_size_empty {
    this.assertEquals(sidechain.size, 0, "Size should be 0 initially");
  }

  // ========== keys tests ==========

  test_keys_empty {
    this.assertEquals(sidechain.keys, [], "Keys should be empty initially");
  }

  // ========== status tests ==========

  test_status_empty {
    this.assertEquals(sidechain.status, "  (none)", "Status should show none when empty");
  }

  // ========== keysValuesDo tests ==========

  test_keysValuesDo_empty {
    var count = 0;
    sidechain.keysValuesDo { count = count + 1 };
    this.assertEquals(count, 0, "Should not iterate when empty");
  }
}
