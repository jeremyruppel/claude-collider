// CCPhrase - Structured melodic phrase from motif + development plan
// Sequences a motif through transformations to create composed phrases.
// Parses a flat operation array at init time for efficient streaming.

CCPhrase : Pattern {
	var <motif;
	var <steps;
	var <repeats;

	*new { |motif, plan, repeats=inf|
		^super.new.init(motif, plan, repeats);
	}

	init { |argMotif, argPlan, argRepeats|
		motif = argMotif;
		steps = this.parsePlan(argPlan);
		repeats = argRepeats;
	}

	storeArgs { ^[motif, steps, repeats] }

	embedInStream { |inval|
		repeats.value(this).do {
			steps.do { |step|
				inval = this.applyOp(motif, step[0], step[1]).embedInStream(inval);
			};
		};
		^inval;
	}

	// Parse flat plan array into [op, arg] tuples
	parsePlan { |plan|
		var result, i, op, opsWithArgs;
		result = [];
		i = 0;
		opsWithArgs = [\transpose, \extend, \truncate];
		while { i < plan.size } {
			op = plan[i];
			if(opsWithArgs.includes(op)) {
				result = result.add([op, plan[i+1]]);
				i = i + 2;
			} {
				if([\state, \invert, \retrograde].includes(op)) {
					result = result.add([op, nil]);
				} {
					"CCPhrase: unknown operation '%'".format(op).warn;
				};
				i = i + 1;
			};
		};
		^result;
	}

	applyOp { |aMotif, op, val|
		^case
		{ op == \state } { aMotif }
		{ op == \transpose } { aMotif.transpose(val) }
		{ op == \invert } { aMotif.invert }
		{ op == \retrograde } { aMotif.retrograde }
		{ op == \extend } { aMotif.extend(val) }
		{ op == \truncate } { aMotif.truncate(val) }
		{ aMotif };
	}
}
