# Custom Jukebox Discs: YouTube Addon Changelog

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
