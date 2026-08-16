# Third-party licenses

Custom Jukebox Discs itself is MIT licensed; see `LICENSE`. The libraries below
are bundled in the published jar and keep their own terms.

## mp3agic 0.9.1

- Project: https://github.com/mpatric/mp3agic
- License: MIT
- Copyright: Michael Patricios and mp3agic contributors

mp3agic is bundled for bounded MP3 frame and duration inspection. Its MIT
license text is packaged at
`META-INF/licenses/customjukeboxdiscs/mp3agic-MIT.txt`.

## VorbisJava Core 0.8

- Project: https://github.com/Gagravarr/VorbisJava
- License: Apache License 2.0
- Copyright: Nick Burch and VorbisJava contributors

VorbisJava Core is bundled for bounded OGG/Vorbis container inspection. Its
Apache 2.0 license text is packaged at
`META-INF/licenses/customjukeboxdiscs/vorbis-java-Apache-2.0.txt`.

## JLayer 1.0.1

- Project: http://www.javazoom.net/javalayer/javalayer.html
- License: GNU Lesser General Public License 2.1
- Copyright: JavaZOOM

JLayer is bundled for client-side MP3 decoding. The mod does not modify JLayer,
and it is packaged as its own unmodified jar under `META-INF/jarjar/`, so it can
be replaced with another build of the same library. Its license text is packaged
at `META-INF/licenses/customjukeboxdiscs/jlayer-LGPL-2.1.txt` and the
corresponding source artifact is available from Maven Central under
`javazoom:jlayer:1.0.1`.

No native codec, FFmpeg executable, telemetry library, or remote service is
bundled in this mod.
