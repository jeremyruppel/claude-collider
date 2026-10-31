// CCMIDIClockTest - Unit tests for CCMIDIClock

// Proper mock class — Event's stop method shadows doesNotUnderstand,
// so an Event-based mock won't dispatch stop to its function slot.
// Records each call as [type, ...args]; a tick carries the clock beat it was
// scheduled on.
CCMIDIClockMockMidiOut {
  var messages;
  *new { |messages| ^super.new.init(messages) }
  init { |argMessages| messages = argMessages }
  start { messages.add([\start]) }
  stop { messages.add([\stop]) }
  midiClock { messages.add([\tick, TempoClock.default.beats]) }
}

CCMIDIClockTest : CCUnitTest {
  var mockMidiOut, messages;

  setUp {
    CCMIDIClock.disable;
    messages = List[];
    mockMidiOut = CCMIDIClockMockMidiOut(messages);
  }

  tearDown {
    CCMIDIClock.disable;
  }

  types {
    ^messages.collect(_.first);
  }

  tickBeats {
    ^messages.select { |m| m.first == \tick }.collect(_[1]);
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
    this.assert(this.types.includes(\stop), "Should stop previous clock on re-enable");
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
    this.assert(this.types.includes(\start), "Should send start via class method");
  }

  test_classStop_delegatesToCurrent {
    CCMIDIClock.enable(mockMidiOut);
    CCMIDIClock.current.start;
    messages.clear;
    CCMIDIClock.stop;
    this.assert(this.types.includes(\stop), "Should send stop via class method");
  }

  // ========== instance start/stop tests ==========

  test_start_sendsStartMessage {
    CCMIDIClock.enable(mockMidiOut);
    CCMIDIClock.current.start;
    this.assertEquals(this.types.first, \start, "Should send MIDI start");
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
    this.assertEquals(this.types.first, \stop, "Should send MIDI stop");
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
    this.assertEquals(this.types[0], \stop, "Should stop before restarting");
    this.assertEquals(this.types[1], \start, "Should start again");
  }

  // ========== tick scheduling tests ==========

  // Checks the beats ticks are scheduled on rather than counting ticks over a
  // wait, so the result doesn't depend on where in the beat the test starts
  test_start_ticksAt24PerBeat {
    var firstTick = TempoClock.default.nextTimeOnGrid(1);
    var beats;
    CCMIDIClock.enable(mockMidiOut);
    CCMIDIClock.current.start;
    // Wait until a few ticks have landed
    (firstTick - TempoClock.default.beats + (3.5 / 24)).wait;
    CCMIDIClock.current.stop;
    beats = this.tickBeats;

    this.assert(beats.size >= 3, "Should have sent several ticks (got %)".format(beats.size));
    this.assertFloatEquals(beats.first, firstTick, "First tick should land on the next whole beat");
    beats.differentiate.drop(1).do { |gap|
      this.assertFloatEquals(gap, 1/24, "Ticks should be 1/24 beat apart (24 ppqn)");
    };
  }

  // Stops after ticks have started, so it can't pass by stopping before the
  // first tick
  test_stop_haltsTicks {
    var firstTick = TempoClock.default.nextTimeOnGrid(1);
    var countAtStop, countAfter;
    CCMIDIClock.enable(mockMidiOut);
    CCMIDIClock.current.start;
    (firstTick - TempoClock.default.beats + (2.5 / 24)).wait;
    CCMIDIClock.current.stop;
    countAtStop = this.tickBeats.size;
    // Several tick intervals: any tick still scheduled would have landed
    (4 / 24).wait;
    countAfter = this.tickBeats.size;
    this.assert(countAtStop > 0, "Should have sent ticks before stop (got %)".format(countAtStop));
    this.assertEquals(countAfter, countAtStop, "Should not send ticks after stop");
  }
}
