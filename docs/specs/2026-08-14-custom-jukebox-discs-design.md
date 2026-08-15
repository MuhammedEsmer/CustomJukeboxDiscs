# Custom Jukebox Discs Design

## Product definition

Custom Jukebox Discs is a standalone and configurable Minecraft 1.21.1 NeoForge mod. Players place MP3 or OGG Vorbis files in a dedicated local folder, upload an approved track through an in-game Disc Writer, and write the track reference onto a programmable music disc. Programmed discs play through the normal Minecraft jukebox.

The mod has no HooDoo-specific names, stages, recipes, or dependencies. Modpacks may change its recipes and progression through datapacks or KubeJS.

## Platform and distribution

- Minecraft: 1.21.1 only for the first release.
- Loader: NeoForge 21.1.231 or newer within the 21.1 line.
- Java: 21.
- Mod ID: `customjukeboxdiscs`.
- Base package: `dev.muhammedesmer.customjukeboxdiscs`.
- Repository: `MuhammedEsmer/CustomJukeboxDiscs`.
- Required on both client and server.
- The first release does not target Fabric or other Minecraft versions.

## Player experience

1. The player copies an `.mp3` or `.ogg` file into `.minecraft/customjukeboxdiscs/uploads`.
2. The player opens the Disc Writer.
3. The screen lists valid local files and reports invalid files without uploading them.
4. The player inserts one Blank Disc, selects a track, optionally edits its title, and starts writing.
5. The server checks permission and quotas before accepting bytes.
6. If the same SHA-256 track already exists, the server reuses it without uploading it again.
7. On success, the Blank Disc becomes a Programmed Disc containing a synchronized track reference.
8. The Programmed Disc works in a normal jukebox, including hopper insertion/ejection, comparator output, block breaking, and distance attenuation.

The upload folder is opened through a button in the Disc Writer screen. The mod never provides an arbitrary filesystem browser.

## Items and block

- `blank_disc`: stack size 16; consumed only after the server confirms a successful write.
- `programmed_disc`: stack size 1; stores `TrackReference` as a custom data component.
- `disc_writer`: block, block entity, menu, and screen with one Blank Disc input slot.

`TrackReference` contains:

- `sha256`: lowercase 64-character hexadecimal content ID.
- `title`: sanitized UTF-8 display title, 1-64 code points.
- `uploaderUuid`: original uploader UUID.
- `uploaderName`: snapshot used only for display, 1-16 code points.
- `durationMillis`: server-validated duration.
- `format`: `MP3` or `OGG_VORBIS`.

The item never contains audio bytes or a local/server filesystem path.

## Access control

The server is authoritative. The default access mode is `OPS`.

Modes:

- `OPS`: only players with permission level 3 or greater may upload/write.
- `ALLOWLIST`: operators and allowlisted players may upload/write.
- `EVERYONE`: all players may upload/write unless denied.

The deny list has precedence over every non-console grant, including operator status. Server console commands cannot be denied. Denial controls uploading and writing; it does not prevent a player from hearing or using an already written disc.

Commands requiring permission level 3:

```text
/customdiscs access mode <ops|allowlist|everyone>
/customdiscs access allow <player>
/customdiscs access deny <player>
/customdiscs access remove <player>
/customdiscs access status [player]
/customdiscs tracks list [page]
/customdiscs tracks info <sha256>
/customdiscs tracks delete <sha256>
/customdiscs reload
```

Access mode, allowlist, and deny list are world-specific and stored with SavedData.

## Limits and configuration

Default server values:

- Maximum source file size: 10 MiB.
- Maximum decoded duration: 600 seconds.
- Maximum tracks owned by one uploader: 20.
- Maximum bytes owned by one uploader: 100 MiB.
- Maximum total server track storage: 2 GiB.
- Maximum simultaneous upload sessions per player: 1.
- Network chunk payload: 31 KiB, because a serverbound custom payload may not exceed 32,767 bytes.
- Upload rate: 512 KiB/s per player.
- Upload inactivity timeout: 30 seconds.
- Supported formats: MP3 and OGG Vorbis, independently switchable.

Default client values:

- Cache maximum: 512 MiB.
- Upload folder scan maximum: 1,000 files.
- Cache eviction: least recently used, never evicting a currently playing track.

Server and client values use NeoForge configuration files. Recipes remain normal JSON recipes and are therefore datapack-overridable.

## Storage model

Audio bytes are stored per world under:

```text
<world>/customjukeboxdiscs/tracks/<first-two-hash-characters>/<sha256>.<mp3|ogg>
```

Temporary uploads are stored under `<world>/customjukeboxdiscs/tmp` and are removed after timeout, logout, server startup recovery, or failed validation.

Track metadata is stored in overworld SavedData. The catalog contains hash, format, validated byte count, duration, owner UUID/name, title, and creation timestamp. Byte files are moved atomically from the temporary directory only after full validation. Catalog changes are committed only after the atomic move succeeds.

Client cache is isolated by server identity:

```text
.minecraft/customjukeboxdiscs/cache/<server-key>/<sha256>.<mp3|ogg>
```

The server key is derived from the multiplayer address or integrated-world identity and must not contain raw address characters.

## Validation and abuse resistance

The server never trusts extensions, client duration, client hash, packet ordering, item state, or paths.

Validation order:

1. Check upload permission and active-session limit.
2. Reject declared size above the configured limit before allocation.
3. Accept fixed-size chunks only for the current session and exact next offset.
4. Stream bytes to a random server-created temporary filename while calculating SHA-256.
5. Enforce byte rate, exact final length, timeout, and quota.
6. Detect MP3 or OGG Vorbis from signatures and parse the stream with bounded readers.
7. Derive actual duration and reject malformed, unsupported, zero-length, or over-duration files.
8. Deduplicate by the server-calculated hash.
9. Atomically move the validated file and update the catalog.
10. Consume the Blank Disc and create the Programmed Disc on the server thread.

Track title and metadata are sanitized. No embedded script, image, URL, playlist, archive, or executable content is interpreted. The mod does not download internet URLs.

## Networking

NeoForge custom payloads use explicit `StreamCodec` limits. Large audio is never placed in an item component, NBT, a single packet, or SavedData.

Upload payloads:

- `UploadBeginRequest`: local hash, declared bytes, format hint, title.
- `UploadBeginResponse`: denied, already present, or session ID with negotiated chunk size.
- `UploadChunk`: session ID, exact offset, at most 31 KiB.
- `UploadFinish`: session ID and client hash.
- `UploadResult`: success or stable error code.

Download/playback payloads:

- `JukeboxPlay`: dimension, block position, track reference, server start tick.
- `JukeboxStop`: dimension and block position.
- `TrackRequest`: SHA-256 and next offset.
- `TrackChunk`: SHA-256, exact offset, total size, bounded bytes.
- `TrackUnavailable`: SHA-256 and stable error code.

Disk Writer menu synchronization carries only small state and progress values. File I/O, hashing, metadata parsing, and decoding run off the game thread. Inventory mutation, SavedData mutation, and world interaction return to the server thread.

## Audio playback

Files remain in their validated source format. This avoids quality loss, server transcoding cost, native FFmpeg binaries, and platform-specific executables.

- OGG Vorbis uses Minecraft's existing OGG stream decoder.
- MP3 uses a bundled, license-compliant pure-Java decoder feeding PCM frames into Minecraft/OpenAL streaming audio.
- Audio is played through the `RECORDS` sound source with jukebox position and vanilla distance attenuation.
- A cache miss starts download immediately. After verification, decoding skips/discards frames up to the current server playback offset before audio begins.
- Hash mismatch deletes the cache entry and permits one clean retry.
- Disconnect, dimension change, jukebox stop/eject, block removal, or track end closes streams and OpenAL sources.

The mod registers one silent jukebox-song placeholder so vanilla accepts the Programmed Disc. A narrowly scoped mixin bridges the dynamic duration and play/stop events. The mixin must not replace general jukebox inventory logic.

## Jukebox compatibility

- Programmed Disc carries the vanilla `JUKEBOX_PLAYABLE` component referencing the silent placeholder.
- Vanilla jukebox and hopper acceptance remain unchanged.
- Comparator output is 15 while a Programmed Disc is inserted.
- The custom bridge stops playback at the server-validated duration rather than the placeholder duration.
- Breaking the jukebox or ejecting the disc sends `JukeboxStop`.
- Nearby players receive active-playback synchronization when entering range, changing dimension, logging in, or when periodic reconciliation detects missing state.
- Missing/deleted tracks remain valid items but show `Track unavailable` and produce no sound.

## Dependency and licensing policy

- NeoForge and Minecraft libraries are provided by the runtime.
- A pure-Java MP3 decoder may be embedded only after its redistribution requirements are documented and its license text is included in the final JAR/META-INF and repository.
- Audio metadata libraries must receive the same license audit.
- No FFmpeg executable, native codec binary, telemetry, analytics, or remote service is included.

## Failure handling

Stable player-facing error codes cover permission denied, denied player, unsupported format, malformed audio, size limit, duration limit, player quota, server quota, rate limit, timeout, missing Blank Disc, writer changed, hash mismatch, storage failure, and track deleted.

Failed uploads never consume a Blank Disc. Temporary files are cleaned deterministically. A storage/catalog mismatch is logged with the hash and repaired conservatively: orphan temporary files are deleted; catalog entries without audio become unavailable; unreferenced validated files are reported to operators and are not deleted automatically.

## Testing strategy

- Pure unit tests: permission precedence, configuration boundaries, path generation, title sanitization, hash validation, quotas, chunk ordering, rate limiting, timeout, cache eviction, and catalog serialization.
- Parser tests: valid/corrupt/truncated/oversized MP3 and OGG fixtures, deceptive extensions, variable bitrate MP3, and excessive metadata.
- NeoForge game tests: Disc Writer inventory mutation, Data Component persistence, recipe override, vanilla jukebox acceptance, hopper behavior, comparator signal, eject/break stop, and missing-track item behavior.
- Network integration tests: interrupted upload, duplicate chunks, wrong offsets, simultaneous clients, disconnect cleanup, cache miss/download/hash retry, and server rejection.
- Manual two-client dedicated-server tests: both formats, positional audio, RECORDS volume slider, late arrival synchronization, dimension change, and cache reuse.
- Release checks: dedicated-server classloading with no client-only references, clean JAR contents, dependency licenses, and Java 21 compatibility.

## Explicit non-goals for version 1

- Fabric support.
- Minecraft versions other than 1.21.1.
- Internet URL, YouTube, Spotify, or radio streaming.
- Playlists, album art, waveform editor, microphone recording, or live voice.
- Server-side audio transcoding.
- DRM or copyright enforcement beyond operator-controlled upload permissions and deletion.
- HooDoo-specific recipes, stages, branding, or configuration.

