# Your Samples

Put your own audio samples here. Supported formats: WAV, AIFF, MP3.

Files in this folder are ignored by git, so they stay on your machine.

Each file's name (without the extension) becomes its sample name, so
`kick.wav` is loaded with `~cc.samples.load(\kick)`. Subfolders are not
scanned.

Samples here take priority over the bundled ones in `samples/` at the repo
root. If you add a file with the same name as a bundled sample, yours is
used.

After adding files while the server is running, rescan with
`~cc.samples.reload`.

To use a different folder, set the `CC_SAMPLES_PATH` environment variable.
It accepts several folders separated by `:`.
