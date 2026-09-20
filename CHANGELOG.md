# Changelog

All notable changes to Custom Jukebox Discs. Versions follow [semantic versioning](https://semver.org/);
1.0.0 is reserved for the first release considered feature complete. Per-version notes for uploading
live in `docs/changelogs/`.

## 0.3.0

### Changed

- Unified the NeoForge 1.21.1, NeoForge 26.1.2 and Forge 1.12.2 releases under one version.
- New dedicated-server worlds now allow disc writing for everyone by default; existing saved access modes are preserved.
- Singleplayer world owners can always write discs, including hardcore worlds and worlds with cheats disabled.
- Link writes may leave the title empty; direct links derive a readable title from the file name.
- Raised new-server defaults to 25 MiB per source and a 120-second upload timeout.

### Fixed

- Valid MP3 files with metadata or padding before the first audio frame are no longer rejected as unsupported.
- Files merely renamed to `.mp3` are still rejected when they contain no valid MP3 stream.
- Newly uploaded local tracks are copied into the client cache so freshly written discs can play immediately.
- Server Library writes use the server's current Disc Writer slot state instead of a client-side identity value.
- Short, unique hashes printed by the track list command can be used for deletion; ambiguous prefixes are rejected.
- Disc Writer status messages wrap within the interface, successful writes keep the progress bar full, and the written track name is shown.
- Server Library rows no longer overlap or render outside the list panel on the 26.1.2 and 1.12.2 ports.

## 0.2.10

### Changed

- Link imports can now supply a track title automatically when the title field is left empty.
- The Disc Writer now shows link resolution, real download percentage and disc-writing stages.
- Successful writes keep the progress bar full and show the finished track name.

### Fixed

- Link imports no longer require users to invent a title before the provider has read the link.
- Link-based writes no longer attempt to cache a nonexistent local source file on the client.

## 0.2.9

### Added

- Added a server URL-import API used by optional media addons.
- Added support for the separate NeoForge 1.21.1 YouTube addon without requiring it on clients.

### Changed

- Supported addon URLs can be imported even when the unrestricted direct-link downloader is disabled.
- URL imports now return to the Minecraft server thread before writing the finished disc.

### Fixed

- Newly uploaded local tracks are now verified and copied into the client cache immediately.
- Fixed newly written discs sometimes remaining silent until their audio was downloaded again.
- `/customdiscs tracks delete` now accepts the shortened hash printed by the track list command.
- Ambiguous shortened hashes are rejected without deleting either track.
- Increased the default audio source limit from 10 MiB to 25 MiB.
- Increased the default upload timeout from 30 seconds to 120 seconds.
- URL download timeouts now show a specific, compact error in the Disc Writer.
- Disc Writer status messages now wrap inside their available area.

## 0.2.8

### Added

- Native NeoForge build for Minecraft 26.1.2, suitable for All the Mods 11.

### Changed

- Re-released the NeoForge 1.21.1 build after correcting its CurseForge file listing.

### Fixed

- Fixed invisible Disc Writer list/status text on Minecraft 26.1.2.
- Fixed dark, backwards-facing discs displayed on Disc Racks on Minecraft 26.1.2.
- Fixed Disc Rack slots 2–9 rendering incorrectly and appearing through walls on Minecraft 26.1.2.
- Fixed the raw key category name and restored real track titles in the Now Playing message.

## 0.2.7

### Changed

- Uploads are serialised server-wide: one disc is written at a time. A second writer is told
  "Someone else is uploading; you are next in the queue" and retries automatically until it is free.

## 0.2.6

### Fixed

- Writing a disc failed on every dedicated server ("The Disc Writer changed"): the writer
  fingerprint was compared across the client and server processes, where an item identity hash
  never matches. The server now uses its own fingerprint throughout. Singleplayer was unaffected.

## 0.2.5

No player-facing changes.

### Fixed

- The Gradle wrapper was committed without its executable bit, so continuous builds never ran.

### Changed

- Continuous builds now run the game tests on a dedicated server as well as the unit tests.

## 0.2.4

### Changed

- Released under the MIT licence; modpacks may include it freely.

### Fixed

- Packaged the LGPL-2.1 licence text for the bundled JLayer decoder.

## 0.2.3

### Added

- Mod logo for the in-game mod list.

## 0.2.2

### Added

- `urlUploads.allowPrivateAddresses` for an audio host on the server's own network; off by default.

## 0.2.1

### Fixed

- Disc Rack drew its discs on the back face instead of the front.
- Music from a backpack the listener carries jumped between the ears when walking sideways.
- A link no longer has to end in `.mp3` or `.ogg`; the server decides the format from the bytes.
- The link field hint and the upload status text overflowed their widgets.

### Changed

- The default allowed download hosts now cover the Internet Archive, Free Music Archive, Discord's
  CDN and GitHub Pages.

## 0.2.0

### Added

- Programmed discs now come in several colours, picked at random when a disc is written.
- Custom Jukebox Discs has its own creative mode tab.
- Disc Rack: a nine-slot shelf that shows the discs it holds. Right-click a disc to take it,
  sneak or click the frame to open it like a chest.
- Per-player playback opt-out. Turning custom music off also stops the client from downloading
  any track, so it costs no bandwidth.
- Tracks can be written from a direct `https` audio link. The server downloads and validates the
  file; operators control which hosts are allowed.
- Reworked Disc Writer screen with labelled areas, a scrollable file list and a progress bar.

### Changed

- Jar names now include the Minecraft version, for example `customjukeboxdiscs-1.21.1-0.2.0.jar`.

## 0.1.0

### Added

- Upload MP3 or OGG Vorbis files through the Disc Writer and write them onto programmed discs.
- Programmed discs play in a vanilla jukebox with positional audio on the RECORDS channel.
- Server-authoritative validation, per-player and per-server quotas, and world-local storage.
- Verified per-server client cache with least-recently-used eviction.
- Operator commands for access mode, allow and deny lists, the track catalog, and config reload.
- Optional Sophisticated Backpacks and Storage integration through their jukebox upgrade.
