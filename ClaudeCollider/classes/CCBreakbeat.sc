// CCBreakbeat - Breakbeat slice sequencer
// Wraps a buffer and divides it into equal slices for pattern-based sequencing.
// Negative indices in the order array reverse that slice's playback.

CCBreakbeat {
  var <buffer;
  var <numSlices;
  var <sliceDur;

  *new { |buffer, numSlices=8|
    ^super.new.init(buffer, numSlices);
  }

  init { |argBuffer, argNumSlices|
    buffer = argBuffer;
    numSlices = argNumSlices;
  }

  // Set slice duration from bar count (chainable)
  bars { |numBars=1, beatsPerBar=4|
    sliceDur = (numBars * beatsPerBar) / numSlices;
    ^this;
  }

  // Set explicit slice duration in beats
  dur_ { |beats|
    sliceDur = beats;
  }

  // Normalized start position for a slice index
  sliceStart { |index|
    ^index / numSlices;
  }

  // Normalized end position for a slice index
  sliceEnd { |index|
    ^(index + 1) / numSlices;
  }

  // Returns a Pbind for use with Pdef
  // order: Array of slice indices (negative = reverse playback)
  // rate: Number, Array, or Pattern for playback rate
  // amp: Number or Pattern for amplitude
  pattern { |order, rate=1, amp=0.5|
    var starts, ends, rates, dur;

    dur = sliceDur ?? { this.computeSliceDur };

    starts = order.collect { |i| this.sliceStart(i.abs) };
    ends = order.collect { |i| this.sliceEnd(i.abs) };

    rates = case
      { rate.isKindOf(Pattern) } { rate }
      { rate.isKindOf(Array) } {
        Pseq(order.collect { |i, j|
          if(i < 0) { rate.wrapAt(j).neg } { rate.wrapAt(j) }
        }, inf)
      }
      {
        Pseq(order.collect { |i|
          if(i < 0) { rate.neg } { rate }
        }, inf)
      };

    ^Pbind(
      \instrument, \cc_breakbeat,
      \buf, buffer,
      \start, Pseq(starts, inf),
      \end, Pseq(ends, inf),
      \rate, rates,
      \amp, amp,
      \dur, dur
    );
  }

  // Quick-play: wraps pattern in a Pdef
  play { |order, name=\break|
    order = order ?? { (0..numSlices-1) };
    Pdef(name, this.pattern(order)).play;
    ^Pdef(name);
  }

  stop { |name=\break|
    Pdef(name).stop;
  }

  // Compute slice duration in beats from buffer duration
  computeSliceDur {
    var sliceSecs = buffer.duration / numSlices;
    ^sliceSecs * TempoClock.default.tempo;
  }
}
