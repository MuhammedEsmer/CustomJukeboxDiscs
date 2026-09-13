# YouTube Import Service Design

## Goal

Move YouTube extraction out of the Minecraft JVM. The NeoForge 1.21.1 addon sends a validated YouTube video ID to a private service, receives MP3 bytes, and hands the existing destination file back to the base mod.

## Architecture

The addon remains server-side and optional. It accepts only canonical single-video YouTube URLs, then calls a configurable HTTP service with a shared bearer token. The service uses the same provider flow as `terreng/computercraft-streaming-music`: RapidAPI resolves a YouTube video to a prepared MP3 URL. Unlike the reference project, it returns MP3 instead of transcoding to ComputerCraft DFPWM.

The service is a separate Node.js process intended to bind to `127.0.0.1`. It exposes `POST /v1/import`, validates the video ID and limits, polls the provider, streams the result to the caller, and emits useful server logs. Minecraft never launches `yt-dlp` or FFmpeg.

## Request and response

Request JSON:

```json
{"videoId":"xAWDqdpOlu8","maxBytes":26214400,"maxDurationSeconds":600}
```

The request includes `Authorization: Bearer <token>`. Success is `200 audio/mpeg`. Expected failures use JSON and stable status codes: `400` invalid input, `401` bad token, `413` size limit, `422` unavailable/too long, `429` provider quota, and `502` provider failure.

## Configuration

Addon server config gains `serviceUrl`, `serviceToken`, and `requestTimeoutMillis`. The default URL is `http://127.0.0.1:8765`; the token has no usable default. The old managed-tool and executable settings are removed because this addon version uses only the service.

Service secrets are environment variables and are never committed: `CJD_SERVICE_TOKEN`, `RAPIDAPI_KEY`, and `RAPIDAPI_USERNAME`.

## Safety and limits

- Both sides validate the 11-character video ID.
- The addon rejects non-YouTube and playlist URLs before any request.
- The addon writes to a temporary file, enforces the byte limit while streaming, and removes partial files after every failure.
- The service limits concurrent jobs and response size.
- The endpoint binds to localhost by default and requires authentication.

## Verification

Java tests use an in-process HTTP server for success, authentication/provider errors, oversized responses, timeout, and partial-file cleanup. Node tests cover request validation and provider result mapping without calling RapidAPI. Final verification builds both jars and runs the existing test suite.
