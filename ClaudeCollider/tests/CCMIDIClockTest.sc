// CCMIDIClockTest - Unit tests for CCMIDIClock

// Proper mock class — Event's stop method shadows doesNotUnderstand,
// so an Event-based mock won't dispatch stop to its function slot.
CCMIDIClockMockMidiOut {
  var messages;
  *new { |messages| ^super.new.init(messages) }
  init { |argMessages| messages = argMessages }
  start { messages.add(\start) }
  stop { messages.add(\stop) }
  midiClock { messages.add(\tick) }
}

CCMIDIClockTest : UnitTest {
  var mockMidiOut, messages;

  setUp {
    CCMIDIClock.disable;
    messages = List[];
    mockMidiOut = CCMIDIClockMockMidiOut(messages);
  }

  tearDown {
    CCMIDIClock.disable;
  }

  // ========== enable/disable tests ==========

  test_enable_setsCurrent {
    CCMIDIClock.enable(mockMidiOut);
    this.assert(CCMIDIClock.current.notNil, "Should set current");
  }

  test_enable_storesMidiOut {
    CCMIDIClock.enable(mockMidiOut);
    this.assertEquals(CCMIDIClock.current.midiOut, mockMidiOut, "Should store midiOut");
  }

  test_enable_notRunning {
    CCMIDIClock.enable(mockMidiOut);
    this.assert(CCMIDIClock.current.isRunning.not, "Should not be running after enable");
  }

  test_enable_stopsExisting {
    CCMIDIClock.enable(mockMidiOut);
    CCMIDIClock.current.start;
    CCMIDIClock.enable(mockMidiOut);
    this.assert(messages.includes(\stop), "Should stop previous clock on re-enable");
  }

  test_disable_clearsCurrent {
    CCMIDIClock.enable(mockMidiOut);
    CCMIDIClock.disable;
    this.assertEquals(CCMIDIClock.current, nil, "Should clear current");
  }

  test_disable_whenNil {
    CCMIDIClock.disable;
    this.assertEquals(CCMIDIClock.current, nil, "Should not error when no current");
  }

  // ========== class method start/stop tests ==========

  test_classStart_noop_whenNoCurrent {
    CCMIDIClock.start;
    this.assertEquals(messages.size, 0, "Should not send messages when no current");
  }

  test_classStop_noop_whenNoCurrent {
    CCMIDIClock.stop;
    this.assertEquals(messages.size, 0, "Should not send messages when no current");
  }

  test_classStart_delegatesToCurrent {
    CCMIDIClock.enable(mockMidiOut);
    CCMIDIClock.start;
    this.assert(messages.includes(\start), "Should send start via class method");
  }

  test_classStop_delegatesToCurrent {
    CCMIDIClock.enable(mockMidiOut);
    CCMIDIClock.current.start;
    messages.clear;
    CCMIDIClock.stop;
    this.assert(messages.includes(\stop), "Should send stop via class method");
  }

  // ========== instance start/stop tests ==========

  test_start_sendsStartMessage {
    CCMIDIClock.enable(mockMidiOut);
    CCMIDIClock.current.start;
    this.assertEquals(messages.first, \start, "Should send MIDI start");
  }

  test_start_setsRunning {
    CCMIDIClock.enable(mockMidiOut);
    CCMIDIClock.current.start;
    this.assert(CCMIDIClock.current.isRunning, "Should be running after start");
  }

  test_stop_sendsStopMessage {
    CCMIDIClock.enable(mockMidiOut);
    CCMIDIClock.current.start;
    messages.clear;
    CCMIDIClock.current.stop;
    this.assertEquals(messages.first, \stop, "Should send MIDI stop");
  }

  test_stop_clearsRunning {
    CCMIDIClock.enable(mockMidiOut);
    CCMIDIClock.current.start;
    CCMIDIClock.current.stop;
    this.assert(CCMIDIClock.current.isRunning.not, "Should not be running after stop");
  }

  test_start_stopsFirst_ifAlreadyRunning {
    CCMIDIClock.enable(mockMidiOut);
    CCMIDIClock.current.start;
    messages.clear;
    CCMIDIClock.current.start;
    this.assertEquals(messages[0], \stop, "Should stop before restarting");
    this.assertEquals(messages[1], \start, "Should start again");
  }

  // ========== tick scheduling tests ==========

  test_start_schedulesTicks {
    CCMIDIClock.enable(mockMidiOut);
    CCMIDIClock.current.start;
    // Wait one beat — should produce 24 ticks
    1.1.wait;
    CCMIDIClock.current.stop;
    this.assert(messages.includes(\tick), "Should have sent clock ticks");
    this.assert(
      messages.select({ |m| m == \tick }).size >= 20,
      "Should send approximately 24 ticks per beat"
    );
  }

  test_stop_haltsTicks {
    var countAtStop, countAfter;
    CCMIDIClock.enable(mockMidiOut);
    CCMIDIClock.current.start;
    0.2.wait;
    CCMIDIClock.current.stop;
    countAtStop = messages.select({ |m| m == \tick }).size;
    0.5.wait;
    countAfter = messages.select({ |m| m == \tick }).size;
    this.assertEquals(countAtStop, countAfter, "Should not send ticks after stop");
  }
}
