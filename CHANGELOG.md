# Changelog

All notable changes to Custom Jukebox Discs. Versions follow [semantic versioning](https://semver.org/);
1.0.0 is reserved for the first release considered feature complete. Per-version notes for uploading
live in `docs/changelogs/`.

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
