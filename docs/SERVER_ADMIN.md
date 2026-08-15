# Server administration

Tracks are stored under `<world>/customjukeboxdiscs/tracks` by SHA-256. Temporary uploads use
`<world>/customjukeboxdiscs/tmp`. Back up both the world data and this directory together.

Uploads default to operators with permission level 3. Use `/customdiscs access` to change the
mode or manage per-player allow/deny entries, and `/customdiscs access status [player]` to read the
current state back. Deny entries override operator status, allowlists, and the everyone mode. The
server console is never denied.

Use `/customdiscs tracks list [page]`, `tracks info <sha256>`, and `tracks delete <sha256>` to audit
and moderate stored audio, and `/customdiscs reload` to apply edited config limits without a restart.
Deleting a track leaves already written discs intact; they report `Track unavailable` and stay silent.

On start-up the server deletes leftover temporary uploads and logs any mismatch between the catalog
and the audio files. It never deletes validated audio on its own. See `CONFIGURATION.md` for the
full key list.

Server limits cover source size, duration, tracks and bytes per player, total storage, concurrent
sessions, transfer chunk size, rate, timeout, and enabled formats. Stop the server before manually
changing stored audio. Recipes are ordinary datapack recipes and may be replaced by modpacks.
