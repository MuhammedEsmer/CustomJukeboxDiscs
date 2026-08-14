# Server administration

Tracks are stored under `<world>/customjukeboxdiscs/tracks` by SHA-256. Temporary uploads use
`<world>/customjukeboxdiscs/tmp`. Back up both the world data and this directory together.

Uploads default to operators with permission level 3. Use `/customdiscs access` to change the
mode or manage per-player allow/deny entries. Deny entries override operator status, allowlists,
and the everyone mode. The server console is never denied.

Server limits cover source size, duration, tracks and bytes per player, total storage, concurrent
sessions, transfer chunk size, rate, timeout, and enabled formats. Stop the server before manually
changing stored audio. Recipes are ordinary datapack recipes and may be replaced by modpacks.
