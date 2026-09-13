# YouTube Import Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Minecraft-side yt-dlp execution with an authenticated private YouTube-to-MP3 service.

**Architecture:** A Node.js localhost service resolves MP3 through RapidAPI. The NeoForge addon downloads the bounded MP3 response into the base mod's existing import destination.

**Tech Stack:** Java 21 `HttpClient`, NeoForge 1.21.1, Node.js 20 built-ins, JUnit 5, Node test runner

**Spec:** `docs/superpowers/specs/2026-09-13-youtube-import-service-design.md`

## Global Constraints

- Addon remains server-side and optional.
- Service binds to `127.0.0.1` by default and requires a bearer token.
- No secrets are committed.
- No VDS deployment or server restart is performed in this change.

---

### Task 1: Addon HTTP importer

**Files:**
- Create: `youtube-addon/src/main/java/dev/hoodoo/customjukeboxdiscs/youtube/ImportServiceClient.java`
- Modify: `youtube-addon/src/main/java/dev/hoodoo/customjukeboxdiscs/youtube/YouTubeImporter.java`
- Modify: `youtube-addon/src/main/java/dev/hoodoo/customjukeboxdiscs/youtube/YouTubeConfig.java`
- Modify: `youtube-addon/src/main/java/dev/hoodoo/customjukeboxdiscs/youtube/CustomJukeboxDiscsYouTube.java`
- Test: `src/test/java/dev/muhammedesmer/customjukeboxdiscs/youtubeaddon/YouTubeImporterTest.java`

**Interfaces:**
- Consumes: canonical `URI`, destination `Path`, duration/byte limits, cancellation flag.
- Produces: `CompletableFuture<UrlImportResult>` and a complete MP3 destination on success.

- [ ] Add failing tests using an in-process HTTP server for success, service errors, oversized bodies, timeout, and cleanup.
- [ ] Run the focused test and confirm failures are caused by the missing HTTP client.
- [ ] Implement bounded authenticated HTTP download and stable error mapping.
- [ ] Run focused and full Java tests.
- [ ] Commit the addon change.

### Task 2: Private conversion service

**Files:**
- Create: `youtube-service/package.json`
- Create: `youtube-service/src/server.js`
- Create: `youtube-service/src/provider.js`
- Create: `youtube-service/test/service.test.js`
- Create: `youtube-service/.env.example`
- Create: `youtube-service/README.md`

**Interfaces:**
- Consumes: authenticated `POST /v1/import` JSON request.
- Produces: bounded `audio/mpeg` response or stable JSON error.

- [ ] Add failing Node tests for authentication, input validation, provider states, and limits.
- [ ] Run `node --test` and confirm the expected failures.
- [ ] Implement the smallest service and provider client that passes them.
- [ ] Run Node tests.
- [ ] Commit the service change.

### Task 3: Release verification

**Files:**
- Modify: `youtube-addon/CHANGELOG.md`
- Modify: `THIRD_PARTY_LICENSES.md` only if copied reference code requires attribution.

**Interfaces:**
- Consumes: completed addon and service.
- Produces: verified addon jar and deployment instructions.

- [ ] Document the service-backed importer and manual secret setup.
- [ ] Run Java tests, Node tests, and the Gradle build.
- [ ] Inspect release jar names and SHA-256 hashes.
- [ ] Commit documentation and release metadata.
