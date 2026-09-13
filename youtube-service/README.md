# Custom Jukebox Discs YouTube Service

Private companion service for the NeoForge 1.21.1 YouTube addon. It resolves YouTube audio through RapidAPI outside the Minecraft JVM and returns a bounded MP3 response.

## Requirements

- Node.js 20 or newer
- RapidAPI subscriptions for `YT-API` and `YouTube MP3` from the same account

## Configure

Set the five variables shown in `.env.example` in the service process environment. Use a long random value for `CJD_SERVICE_TOKEN`. Do not commit real credentials.

Configure `config/customjukeboxdiscs-youtube-server.toml` on the Minecraft server:

```toml
[service]
url = "http://127.0.0.1:8765/v1/import"
token = "the-same-long-random-value"
timeoutMillis = 120000
```

## Run

```text
npm test
npm start
```

Run it under the VDS process manager so it starts independently of Minecraft. Keep the default localhost bind when the service and Minecraft server are on the same machine.

The included `deploy/customjukeboxdiscs-youtube.service` unit runs it automatically with systemd.
