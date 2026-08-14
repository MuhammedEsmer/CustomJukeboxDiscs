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

