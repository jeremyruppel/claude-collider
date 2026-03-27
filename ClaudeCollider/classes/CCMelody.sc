// CCMelody - Factory methods for common melodic structures
// Utility class with class methods that return Patterns.

CCMelody {

	// Alternates two motifs: call, response, call, response...
	*callAndResponse { |call, response, repeats=inf|
		^Pseq([call, response], repeats);
	}

	// Standard motif development: state x2, transpose up, resolve
	*develop { |motif, repeats=inf|
		^CCPhrase(motif, [
			\state, \state,
			\transpose, 2,
			\state
		], repeats);
	}
}
