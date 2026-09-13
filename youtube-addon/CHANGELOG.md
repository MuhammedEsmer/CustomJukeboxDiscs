# Custom Jukebox Discs: YouTube Addon Changelog

## 0.2.1 — NeoForge 1.21.1

### Changed

- Uses the resolved video title when the Disc Writer title field is empty.
- Reports real download progress to the Disc Writer.
- Requires Custom Jukebox Discs 0.2.10 or newer.

## 0.2.0 — NeoForge 1.21.1

### Changed

- Moved YouTube resolution out of the Minecraft process into the private companion service.
- Replaced managed `yt-dlp` and FFmpeg downloads with an authenticated localhost HTTP connection.
- Added bounded MP3 transfer and clearer handling for service limits, queue pressure and timeouts.

### Server setup

- The addon remains server-side and requires Custom Jukebox Discs 0.2.9 or newer.
- The companion service requires the administrator's own RapidAPI credentials and shared service token.
- Players do not install the addon or service.

## 0.1.0 — NeoForge 1.21.1

### Added

- Write a disc by pasting a single YouTube video link into the existing Disc Writer link field.
- Server-side download and MP3 conversion using managed `yt-dlp` and FFmpeg tools.
- Support for regular, short, mobile and YouTube Music video links.
- Bounded server-wide import queue, duration checks and base-mod upload limit enforcement.
- Automatic verified tool installation on Windows x86-64 and Linux x86-64 servers.

### Limitations

- Playlists, channels, searches, livestreams, private videos and login-protected videos are unsupported.
- The addon is installed only on the server and requires Custom Jukebox Discs 0.2.9 or newer.
