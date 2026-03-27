// CCMotif - Melodic motif as a Pattern with transformations
// Wraps a short degree array and provides transpose, invert, retrograde, etc.
// Each transformation returns a new CCMotif (immutable/chainable).
// Subclasses Pattern so it embeds naturally in Pseq and works with Pn.

CCMotif : Pattern {
	var <degrees;

	*new { |degrees|
		^super.new.init(degrees);
	}

	init { |argDegrees|
		degrees = argDegrees;
	}

	storeArgs { ^[degrees] }

	embedInStream { |inval|
		degrees.do { |item|
			inval = item.embedInStream(inval);
		};
		^inval;
	}

	// === Transformations (all return new CCMotif) ===

	transpose { |n|
		^CCMotif(degrees.collect { |item|
			if(item.isRest) { item } { item + n }
		});
	}

	invert {
		var pivot = degrees.detect { |item| item.isRest.not };
		if(pivot.isNil) { ^CCMotif(degrees.copy) };
		^CCMotif(degrees.collect { |item|
			if(item.isRest) { item } { (2 * pivot) - item }
		});
	}

	retrograde {
		^CCMotif(degrees.reverse);
	}

	extend { |extraDegrees|
		^CCMotif(degrees ++ extraDegrees);
	}

	truncate { |n|
		^CCMotif(degrees.keep(n));
	}
}
