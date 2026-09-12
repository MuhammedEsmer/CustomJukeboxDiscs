# Custom Jukebox Discs YouTube Addon Design

## Scope

Create a separate, server-side NeoForge 1.21.1 addon that lets the existing Disc Writer link field accept supported YouTube links. The addon will not be published on CurseForge. Minecraft 1.12.2 and 26.1.2 are outside this first release.

The base mod remains usable without the addon. Clients only need the base mod; the addon and its media tools run on the dedicated server.

## User experience

1. A player inserts a Blank Disc and opens the existing Disc Writer.
2. The player enters a title and pastes a YouTube video URL into the existing link field.
3. The screen reports short states such as `Queued`, `Downloading`, `Converting`, `Writing`, or a concise failure.
4. The server downloads audio, converts it to MP3, validates it with the base mod's existing limits, adds it to the Server Library, and writes the inserted disc.

Direct audio URLs continue through the current downloader. No new screen, tab, command, or client-side addon is required.

## Architecture

### Base-mod integration point

The base mod will expose a small server API for URL importers. An importer declares whether it supports a URI and asynchronously produces a temporary audio file. `ServerRuntime.handleUrlUpload` asks registered importers first and falls back to the existing direct-file downloader when none accepts the URI.

The API carries only the request URL, destination path, progress callback, cancellation signal, and a structured result. It does not expose Disc Writer internals. The base mod remains responsible for permissions, writer validation, audio inspection, storage, catalog insertion, disc writing, and network responses.

Only one importer may claim a URL. Duplicate registration identifiers and multiple claims are rejected and logged.

### Addon

The addon depends on Custom Jukebox Discs 0.2.9 or newer and registers a YouTube importer during server startup. It accepts only single-video URLs from `youtube.com`, `www.youtube.com`, `m.youtube.com`, `music.youtube.com`, and `youtu.be`. Playlists, channels, searches, live streams, private videos, cookie-based access, and other websites are rejected.

The importer invokes `yt-dlp` with a fixed argument list and invokes FFmpeg to produce MP3 audio. User-controlled values are passed as process arguments, never through a shell command. Temporary files use server-generated names and are deleted after success, failure, cancellation, or startup recovery.

### Media tools

On first use, the addon installs pinned builds of `yt-dlp` and FFmpeg under `config/customjukeboxdiscs-youtube/tools/`. Downloads use HTTPS and hard-coded SHA-256 checksums. Partial downloads are written to temporary files and moved into place only after verification.

The first release supports Windows x86-64 and Linux x86-64. An administrator may override both executable paths in configuration. Unsupported platforms receive a clear server log and a short in-game error; the base mod and direct audio links continue working.

Tools are not silently updated to unverified latest releases. Updating the addon may update the pinned tool versions and checksums.

## Queue and lifecycle

YouTube jobs use one server-wide worker and a bounded FIFO queue. The default queue holds eight waiting jobs and permits one waiting job per player. Repeated requests for the same player and URL are rejected as duplicates.

Before accepting a job, the base mod verifies access permission, an open Disc Writer, and a matching Blank Disc. Before writing the result, it verifies the same writer session and disc fingerprint again. Closing the screen, changing the input disc, disconnecting, stopping the server, or exceeding a timeout cancels the job and removes temporary files.

The job timeout defaults to five minutes. `yt-dlp` rejects media longer than the addon's configured maximum before downloading. The converted file must also pass all base-mod size, duration, and audio-format checks, so the addon cannot bypass server upload limits.

## Configuration

`config/customjukeboxdiscs-youtube-server.toml` contains:

- `enabled` (default `true`)
- `maxDurationSeconds` (default `900`)
- `jobTimeoutSeconds` (default `300`)
- `maxQueuedJobs` (default `8`)
- `ytDlpPath` and `ffmpegPath` (empty means managed tools)
- `managedToolDownloads` (default `true`)

Configuration cannot add arbitrary supported hosts or raw command-line arguments.

## Errors and logging

Player messages stay brief: unsupported link, playlist unsupported, queue full, too long, download failed, conversion failed, tools unavailable, or writer changed. Detailed process output and exception information go only to the server log. URLs are sanitized before logging so incidental query parameters are not retained.

## Testing

- Unit tests cover host recognition, playlist rejection, argument construction, queue limits, duplicate requests, timeout/cancellation, configuration, checksum verification, and temporary-file cleanup.
- Base-mod integration tests cover importer selection, direct-link fallback, permission rejection, writer revalidation, ingestion, Server Library insertion, and disc writing.
- Process tests use fake `yt-dlp` and FFmpeg executables; automated tests never contact YouTube.
- A manual dedicated-server test verifies one public video, simultaneous players, cancellation, restart cleanup, playback, and Server Library reuse.

## Release layout

- Base mod: NeoForge 1.21.1 version 0.2.9, containing the importer API and unchanged client protocol.
- Addon: `customjukeboxdiscs-youtube-1.21.1-0.1.0.jar`.
- Both projects retain HooDoo as the developer name and receive separate changelogs and Git commits.

## Rejected alternatives

- A pure Java YouTube extractor is more likely to break when YouTube changes its delivery logic.
- Bundling large platform-specific FFmpeg binaries inside one jar makes updates and platform handling harder.
- Requiring the addon on clients adds no capability because downloading and conversion are entirely server-side.
