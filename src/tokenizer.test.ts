import { describe, it } from "node:test";
import assert from "node:assert";
import { tokenize, compress } from "./tokenizer.js";

describe("compress", () => {
  it("Basic - multiline SynthDef", () => {
    const input = `SynthDef(\\test, {
  Out.ar(0, SinOsc.ar(440));
}).add;`;
    const expected = `SynthDef(\\test, { Out.ar(0, SinOsc.ar(440)); }).add;`;
    assert.strictEqual(compress(input), expected);
  });

  it("With Comments - line and block", () => {
    const input = `// This is a synth
SynthDef(\\test, { |freq = 440|
  var sig = SinOsc.ar(freq); // oscillator
  /* stereo output */
  Out.ar(0, sig ! 2);
}).add;`;
    const expected = `SynthDef(\\test, { |freq = 440| var sig = SinOsc.ar(freq); Out.ar(0, sig ! 2); }).add;`;
    assert.strictEqual(compress(input), expected);
  });

  it("String with Newlines", () => {
    const input = `"hello
world".postln;`;
    const expected = `"hello\\nworld".postln;`;
    assert.strictEqual(compress(input), expected);
  });

  it("Character Literals", () => {
    const input = `$a.postln;
$\\n.postln;`;
    const expected = `$a.postln; $\\n.postln;`;
    assert.strictEqual(compress(input), expected);
  });

  it("Mixed Symbols", () => {
    const input = `[\\freq, 'symbol', "string"]`;
    const expected = `[\\freq, 'symbol', "string"]`;
    assert.strictEqual(compress(input), expected);
  });

  it("Edge Case: Division vs Comment", () => {
    const input = `x = 10 / 2; // division
y = 4 /* block */ / 2;`;
    const expected = `x = 10 / 2; y = 4 / 2;`;
    assert.strictEqual(compress(input), expected);
  });

  it("Empty string", () => {
    assert.strictEqual(compress(""), "");
  });

  it("String with escaped quote", () => {
    const input = `"say \\"hello\\"".postln;`;
    const expected = `"say \\"hello\\"".postln;`;
    assert.strictEqual(compress(input), expected);
  });

  it("Multiple consecutive spaces collapsed", () => {
    const input = `a    =    b;`;
    const expected = `a = b;`;
    assert.strictEqual(compress(input), expected);
  });

  it("Quoted symbol preserved", () => {
    const input = `'my symbol'.postln;`;
    const expected = `'my symbol'.postln;`;
    assert.strictEqual(compress(input), expected);
  });
});
