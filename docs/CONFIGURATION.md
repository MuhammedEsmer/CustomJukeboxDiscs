# Configuration

## Server config

`<instance>/config/customjukeboxdiscs-server.toml`, reloadable in game with `/customdiscs reload`.

| Key | Default | Meaning |
| --- | --- | --- |
| `limits.maxSourceBytes` | 10485760 | Largest accepted source file. |
| `limits.maxDurationMillis` | 600000 | Largest accepted decoded duration; also the hard ceiling. |
| `limits.maxTracksPerPlayer` | 20 | Tracks a single uploader may own. |
| `limits.maxBytesPerPlayer` | 104857600 | Bytes a single uploader may own. |
| `limits.maxServerBytes` | 2147483648 | Total audio the server will store. |
| `limits.maxSessionsPerPlayer` | 1 | Concurrent uploads per player. |
| `limits.chunkBytes` | 32768 | Requested chunk size; capped at 31 KiB because a serverbound custom payload may not exceed 32767 bytes. |
| `limits.uploadBytesPerSecond` | 524288 | Per-player transfer rate. |
| `limits.uploadTimeoutMillis` | 30000 | Inactivity timeout for an upload session. |
| `formats.mp3Enabled` | true | Accept MP3 uploads. |
| `formats.oggEnabled` | true | Accept OGG Vorbis uploads. |
| `urlUploads.enabled` | false | Allow writing a disc from a direct https link. |
| `urlUploads.allowedHosts` | archive.org, freemusicarchive.org, cdn.discordapp.com, ... | Hosts the server may download from; subdomains count. |
| `urlUploads.allowPrivateAddresses` | false | Allow downloading from the server's own network. Only for a host you control. |

Values are validated as a set: `chunkBytes <= maxSourceBytes <= maxBytesPerPlayer <= maxServerBytes`,
and at least one format must stay enabled.

## Client config

`<instance>/config/customjukeboxdiscs-client.toml`.

| Key | Default | Meaning |
| --- | --- | --- |
| `cache.maxCacheBytes` | 536870912 | Cache budget per server; least recently used tracks are evicted first. |
| `uploads.maxUploadScanFiles` | 1000 | Files listed from the upload folder. |
| `playbackEnabled` | true | Hear custom discs. Off also stops this client downloading any track. |

## Paths

- Upload folder: `.minecraft/customjukeboxdiscs/uploads`
- Client cache: `.minecraft/customjukeboxdiscs/cache/<server-key>/<sha256>.<mp3|ogg>`
- Server audio: `<world>/customjukeboxdiscs/tracks/<first-two-hash-characters>/<sha256>.<mp3|ogg>`
- Server temporary uploads: `<world>/customjukeboxdiscs/tmp`

## Commands

All require permission level 3.

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

## Writing from a link

With `urlUploads.enabled` the Disc Writer accepts an `https` link to a plain `.mp3` or `.ogg` file
instead of a local file. The server downloads it and runs it through the same validation, quotas and
catalog as an upload, so nothing about playback changes.

Only hosts in `allowedHosts` are fetched, redirects are refused, and addresses inside the server's own
network are blocked unless `allowPrivateAddresses` is switched on.

Streaming services are not supported and will not be. The link has to point at an audio file the
server can download: your own web space, the Internet Archive, the Free Music Archive, a file you
uploaded to Discord, a release asset on GitHub. Spotify audio is DRM protected, and taking audio out
of YouTube breaks its terms of service.

## Turning custom music off

Each player can silence custom discs with the *Toggle custom music* key (default `J`) or the
`playbackEnabled` client option. A player who turns it off is never sent a play message, so their
client never downloads a track and the music costs them no bandwidth.

## Recipes

`blank_disc`, `disc_writer` and `disc_rack` are ordinary JSON recipes under `data/customjukeboxdiscs/recipe`
and may be replaced by a datapack or KubeJS.

## Start-up recovery

On every server start the mod deletes leftover temporary uploads, then logs a warning for each
catalogued track whose audio file is missing and for each validated audio file the catalog does not
reference. Validated audio is never deleted automatically; remove it manually if unwanted.
