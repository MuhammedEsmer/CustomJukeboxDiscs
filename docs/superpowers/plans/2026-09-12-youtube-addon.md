# NeoForge 1.21.1 YouTube Addon Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce a server-only NeoForge 1.21.1 addon whose YouTube URLs work through the existing Disc Writer link field.

**Architecture:** The base mod exposes a narrow URL-import registry and retains ownership of permissions, writer validation, ingestion, storage, and responses. A separately packaged addon registers a YouTube importer that manages pinned media tools, queues jobs, invokes processes without a shell, and returns an MP3 temporary file to the base mod.

**Tech Stack:** Java 21, NeoForge 21.1.231, ModDevGradle, JUnit 5, `yt-dlp`, FFmpeg.

**Spec:** `docs/superpowers/specs/2026-09-12-youtube-addon-design.md`

## Global Constraints

- Target only Minecraft 1.21.1 and NeoForge 21.1.231.
- The addon is server-side; clients retain only the base mod.
- Preserve direct-audio URL behavior when no importer accepts a URL.
- Support Windows x86-64 and Linux x86-64 managed tools plus explicit executable overrides.
- Keep HooDoo as author and package separate base/addon jars and changelogs.

---

### Task 1: URL importer API and selection

**Files:**
- Create: `src/main/java/dev/muhammedesmer/customjukeboxdiscs/api/url/UrlImportRequest.java`
- Create: `src/main/java/dev/muhammedesmer/customjukeboxdiscs/api/url/UrlImportResult.java`
- Create: `src/main/java/dev/muhammedesmer/customjukeboxdiscs/api/url/UrlTrackImporter.java`
- Create: `src/main/java/dev/muhammedesmer/customjukeboxdiscs/api/url/UrlImporterRegistry.java`
- Test: `src/test/java/dev/muhammedesmer/customjukeboxdiscs/api/url/UrlImporterRegistryTest.java`

**Interfaces:**
- `UrlTrackImporter.id()`, `supports(URI)`, and `importTrack(UrlImportRequest)` returning `CompletableFuture<UrlImportResult>`.
- `UrlImporterRegistry.register(UrlTrackImporter)` and `find(URI)` returning `Optional<UrlTrackImporter>`.

- [ ] Write tests proving exact importer selection, fallback when none supports the URI, duplicate-ID rejection, and multiple-claim rejection.
- [ ] Run `./gradlew test --tests '*UrlImporterRegistryTest'` and confirm failure.
- [ ] Implement immutable request/result types and a thread-safe registry with deterministic registration order.
- [ ] Run the focused test and confirm success.
- [ ] Commit as `Add URL importer extension API`.

### Task 2: Base-mod URL pipeline integration

**Files:**
- Modify: `src/main/java/dev/muhammedesmer/customjukeboxdiscs/server/ServerRuntime.java`
- Modify: `src/main/java/dev/muhammedesmer/customjukeboxdiscs/transfer/UploadError.java`
- Modify: `src/main/resources/assets/customjukeboxdiscs/lang/en_us.json`
- Modify: `src/main/resources/assets/customjukeboxdiscs/lang/tr_tr.json`
- Test: `src/test/java/dev/muhammedesmer/customjukeboxdiscs/server/UrlImportPipelineTest.java`

**Interfaces:**
- Consumes the Task 1 registry.
- Produces one common ingestion path for importer output and direct downloads.

- [ ] Write tests proving importer-first selection, direct-link fallback, mapped structured errors, temporary cleanup, and writer revalidation.
- [ ] Run the focused test and confirm failure.
- [ ] Refactor `handleUrlUpload` minimally: parse URI, select importer, create a temporary destination, run import/fetch off-thread, then pass successful output to the existing `uploads.ingestDownloaded` path.
- [ ] Add concise localized errors for unsupported media, tools unavailable, queue full, media too long, download failure, and conversion failure.
- [ ] Run focused and existing URL/upload tests.
- [ ] Commit as `Integrate server URL importers`.

### Task 3: Addon source set and packaging

**Files:**
- Modify: `build.gradle`
- Create: `youtube-addon/src/main/resources/META-INF/neoforge.mods.toml`
- Create: `youtube-addon/src/main/resources/pack.mcmeta`
- Create: `youtube-addon/src/main/java/dev/hoodoo/customjukeboxdiscs/youtube/CustomJukeboxDiscsYouTube.java`
- Test: `src/test/java/dev/muhammedesmer/customjukeboxdiscs/YouTubeAddonPackagingTest.java`

**Interfaces:**
- Produces `youtubeAddon` source set and `youtubeAddonJar` archive named `customjukeboxdiscs-youtube-1.21.1-0.1.0.jar`.
- Addon metadata requires `customjukeboxdiscs` version `[0.2.9,)` and loads only on the server.

- [ ] Write a packaging test that opens the produced jar and verifies addon-only classes, metadata, author, dependency, and absence of base-mod classes.
- [ ] Add source-set classpaths, resource processing, jar manifest, build dependency, and release copy task.
- [ ] Add the addon entrypoint and register it on the NeoForge event bus.
- [ ] Run the packaging test and inspect jar contents.
- [ ] Commit as `Scaffold YouTube server addon`.

### Task 4: YouTube recognition, queue, and process commands

**Files:**
- Create: `youtube-addon/src/main/java/dev/hoodoo/customjukeboxdiscs/youtube/YouTubeUrl.java`
- Create: `youtube-addon/src/main/java/dev/hoodoo/customjukeboxdiscs/youtube/ImportQueue.java`
- Create: `youtube-addon/src/main/java/dev/hoodoo/customjukeboxdiscs/youtube/MediaCommands.java`
- Test: `youtube-addon/src/test/java/dev/hoodoo/customjukeboxdiscs/youtube/YouTubeUrlTest.java`
- Test: `youtube-addon/src/test/java/dev/hoodoo/customjukeboxdiscs/youtube/ImportQueueTest.java`
- Test: `youtube-addon/src/test/java/dev/hoodoo/customjukeboxdiscs/youtube/MediaCommandsTest.java`

**Interfaces:**
- `YouTubeUrl.parse(URI)` returns a canonical video URI or a typed rejection.
- `ImportQueue.submit(UUID, URI, Supplier<CompletableFuture<UrlImportResult>>)` enforces one worker, eight waiting jobs, one waiting job per player, and duplicate rejection.
- `MediaCommands.metadata(...)`, `download(...)`, and `convert(...)` return immutable argument lists.

- [ ] Write table-driven URL tests for allowed hosts, HTTPS, video IDs, playlists, live/channel/search links, and deceptive hostnames.
- [ ] Write queue tests for FIFO order, capacity, per-player limits, duplicates, cancellation, and shutdown.
- [ ] Write command tests proving fixed switches, `--` URL separation, generated paths, duration limit, and no shell concatenation.
- [ ] Run tests red, implement minimal classes, then rerun green.
- [ ] Commit as `Add safe YouTube job pipeline`.

### Task 5: Managed tools and importer

**Files:**
- Create: `youtube-addon/src/main/java/dev/hoodoo/customjukeboxdiscs/youtube/YouTubeConfig.java`
- Create: `youtube-addon/src/main/java/dev/hoodoo/customjukeboxdiscs/youtube/PlatformTools.java`
- Create: `youtube-addon/src/main/java/dev/hoodoo/customjukeboxdiscs/youtube/ManagedToolInstaller.java`
- Create: `youtube-addon/src/main/java/dev/hoodoo/customjukeboxdiscs/youtube/ProcessRunner.java`
- Create: `youtube-addon/src/main/java/dev/hoodoo/customjukeboxdiscs/youtube/YouTubeImporter.java`
- Test: matching `youtube-addon/src/test/java/...` tests using temporary directories and fake executables.

**Interfaces:**
- `ManagedToolInstaller.resolve(YouTubeConfig)` verifies pinned SHA-256 values before installation and returns `PlatformTools`.
- `ProcessRunner.run(List<String>, Duration, CancellationToken)` captures bounded diagnostics and kills timed-out/cancelled process trees.
- `YouTubeImporter` implements the base API and returns MP3 on success.

- [ ] Write failing tests for configuration bounds, OS/architecture selection, checksum mismatch, atomic installation, timeout, cancellation, cleanup, error mapping, and a successful fake-tool pipeline.
- [ ] Add pinned HTTPS tool manifests for Windows/Linux x86-64 and executable-path overrides.
- [ ] Implement bounded metadata lookup, duration rejection, download, MP3 conversion, output validation, and cleanup.
- [ ] Register the importer and close its queue/processes on server shutdown.
- [ ] Run all addon tests and commit as `Implement YouTube audio importer`.

### Task 6: Versioning, documentation, and release verification

**Files:**
- Modify: `gradle.properties`
- Modify: `CHANGELOG.md`
- Create: `docs/changelogs/0.2.9.md`
- Create: `youtube-addon/CHANGELOG.md`
- Modify: `README.md`

**Interfaces:**
- Produces base `customjukeboxdiscs-1.21.1-0.2.9.jar` and addon `customjukeboxdiscs-youtube-1.21.1-0.1.0.jar` under `releases/`.

- [ ] Set base version to 0.2.9 and document the optional server API/addon without advertising unsupported game versions.
- [ ] Add administrator installation, supported URLs, generated files, configuration, permissions, and troubleshooting instructions.
- [ ] Run `./gradlew clean test runGameTestServer build` and require all tests/GameTests to pass.
- [ ] Inspect both jars, verify metadata and SHA-256 hashes, and perform a fake-tool end-to-end dedicated-server test.
- [ ] Commit as `Release YouTube addon for NeoForge 1.21.1`.
- [ ] Push all commits after confirming a clean worktree.
