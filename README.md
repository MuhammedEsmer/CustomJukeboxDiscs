# Custom Jukebox Discs

Standalone, configurable music-disc upload and playback mod for Minecraft 1.21.1 / NeoForge 21.1.231+.

Players place MP3 or OGG files in `.minecraft/customjukeboxdiscs/uploads`, open a Disc Writer,
insert a Blank Disc, choose a file, and upload it. The server validates and stores the track in
the world. Programmed Discs work in ordinary vanilla jukeboxes and use the Records volume slider.

Uploads are OP-only by default. Operators can select `ops`, `allowlist`, or `everyone` mode and
can explicitly allow, deny, or remove individual players. An explicit deny always wins.

## Build

Requires Java 21.

```text
./gradlew clean test build
```

The distributable JAR is written to `build/libs` and must be installed on both server and client.

## Optional YouTube addon

The separate NeoForge 1.21.1 addon is built as
`releases/customjukeboxdiscs-youtube-1.21.1-0.1.0.jar`. Install it only on the server beside
Custom Jukebox Discs 0.2.9 or newer. Players keep using the existing Disc Writer link field and do
not install the addon.

The first YouTube import downloads verified `yt-dlp` and FFmpeg builds into
`config/customjukeboxdiscs-youtube/tools`. FFmpeg is a large download, so the first import takes
longer. Explicit tool paths and managed downloads can be changed in
`serverconfig/customjukeboxdiscs-youtube-server.toml`.

Only single public video links are supported. Playlists, livestreams and login-protected videos are
rejected. Server owners and players are responsible for importing only audio they are allowed to use.

## Administration

```text
/customdiscs access mode ops|allowlist|everyone
/customdiscs access allow <player>
/customdiscs access deny <player>
/customdiscs access remove <player>
```

Limits are stored in `serverconfig/customjukeboxdiscs-server.toml` inside each world. Client cache
settings are in `config/customjukeboxdiscs-client.toml`.

- Design: `docs/specs/2026-08-14-custom-jukebox-discs-design.md`
- Plan: `docs/plans/2026-08-14-custom-jukebox-discs.md`
- Server guide: `docs/SERVER_ADMIN.md`
- Privacy and copyright: `docs/PRIVACY_AND_COPYRIGHT.md`

