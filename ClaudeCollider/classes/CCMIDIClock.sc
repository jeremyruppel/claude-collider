// CCMIDIClock - MIDI clock output synchronized to TempoClock
// Sends 24 ppqn ticks, start, and stop messages via MIDIOut.
// Enable once; CCArrangement auto-starts/stops clock on play/stop.

CCMIDIClock {
  classvar <current;

  var <midiOut;
  var <isRunning;

  *enable { |midiOut|
    if(current.notNil) { current.stop };
    current = this.new(midiOut);
  }

  *start {
    if(current.notNil) { current.start };
  }

  *stop {
    if(current.notNil) { current.stop };
  }

  *disable {
    this.stop;
    current = nil;
  }

  *new { |midiOut|
    ^super.new.init(midiOut);
  }

  init { |argMidiOut|
    midiOut = argMidiOut;
    isRunning = false;
  }

  start {
    if(isRunning) { this.stop };
    isRunning = true;
    midiOut.start;
    this.scheduleTick(TempoClock.default.nextTimeOnGrid(1));
  }

  stop {
    isRunning = false;
    midiOut.stop;
  }

  scheduleTick { |beat|
    TempoClock.default.schedAbs(beat, {
      if(isRunning) {
        midiOut.midiClock;
        this.scheduleTick(beat + (1/24));
      };
      nil;
    });
  }
}
