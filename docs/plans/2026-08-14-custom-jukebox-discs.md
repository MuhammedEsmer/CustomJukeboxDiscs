# Custom Jukebox Discs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a standalone configurable NeoForge 1.21.1 mod that lets authorized players upload bounded MP3/OGG tracks, write content-addressed Programmed Discs, and hear them through vanilla jukeboxes.

**Architecture:** The server owns permission, catalog, quotas, validation, and world-local audio storage. Audio bytes move in bounded ordered payload chunks and are cached per server on clients. A custom data component identifies a track, while a silent vanilla jukebox placeholder plus narrow mixins bridge dynamic playback into Minecraft/OpenAL.

**Tech Stack:** Java 21, NeoForge 21.1.231, ModDevGradle official 1.21.1 MDK, Gradle, JUnit 5, NeoForge GameTest, Mixin, SHA-256, Minecraft OGG decoder, pure-Java MP3 decoder after license audit.

## Global Constraints

- Mod ID is exactly `customjukeboxdiscs`; base package is `dev.muhammedesmer.customjukeboxdiscs`.
- Target only Minecraft 1.21.1 and NeoForge 21.1.231+ in the 21.1 line.
- Require Java 21 and require the mod on both client and server.
- Do not add Fabric, FFmpeg/native binaries, remote services, telemetry, URL streaming, or HooDoo-specific behavior.
- Default maximums: 10 MiB/file, 600 seconds, 20 tracks/player, 100 MiB/player, 2 GiB/server, one upload/player, 32 KiB/chunk, 512 KiB/s, 30-second timeout, 512 MiB client cache.
- Server recalculates hash, format, duration, quotas, and permissions; no client-provided path or metadata is trusted.
- Every product task follows red-green-refactor and ends in a focused commit.

---

## Planned file structure

```text
build.gradle
gradle.properties
settings.gradle
src/main/java/dev/muhammedesmer/customjukeboxdiscs/
  CustomJukeboxDiscs.java
  config/ClientConfig.java
  config/ServerConfig.java
  content/ModBlocks.java
  content/ModDataComponents.java
  content/ModItems.java
  content/ModMenus.java
  content/ModSounds.java
  content/disc/AudioFormat.java
  content/disc/ProgrammedDiscItem.java
  content/disc/TrackReference.java
  content/writer/DiscWriterBlock.java
  content/writer/DiscWriterBlockEntity.java
  content/writer/DiscWriterMenu.java
  permission/AccessMode.java
  permission/AccessPolicyData.java
  permission/AccessService.java
  storage/AudioInspector.java
  storage/TrackCatalogData.java
  storage/TrackMetadata.java
  storage/TrackStorage.java
  transfer/UploadManager.java
  transfer/UploadSession.java
  network/ModPayloads.java
  network/payload/*.java
  command/CustomDiscsCommand.java
  jukebox/JukeboxPlaybackService.java
  mixin/JukeboxBlockEntityMixin.java
src/main/java/dev/muhammedesmer/customjukeboxdiscs/client/
  ClientBootstrap.java
  cache/ClientTrackCache.java
  cache/ServerCacheKey.java
  audio/DynamicAudioEngine.java
  audio/Mp3PcmStream.java
  screen/DiscWriterScreen.java
  transfer/ClientDownloadManager.java
  transfer/ClientUploadManager.java
  mixin/SoundBufferLibraryMixin.java
src/main/resources/
  META-INF/neoforge.mods.toml
  customjukeboxdiscs.mixins.json
  assets/customjukeboxdiscs/lang/en_us.json
  assets/customjukeboxdiscs/lang/tr_tr.json
  assets/customjukeboxdiscs/models/**
  assets/customjukeboxdiscs/textures/**
  assets/customjukeboxdiscs/sounds.json
  data/customjukeboxdiscs/jukebox_song/silent_dynamic.json
  data/customjukeboxdiscs/recipe/blank_disc.json
  data/customjukeboxdiscs/recipe/disc_writer.json
src/test/java/dev/muhammedesmer/customjukeboxdiscs/**
src/gametest/java/dev/muhammedesmer/customjukeboxdiscs/**
```

## Core interfaces

```java
public record TrackReference(
    String sha256, String title, UUID uploaderUuid, String uploaderName,
    long durationMillis, AudioFormat format
) {}

public record TrackMetadata(
    TrackReference reference, long byteCount, Instant createdAt
) {}

public interface AudioInspector {
    InspectionResult inspect(Path path, long maxBytes, Duration maxDuration) throws IOException;
}

public interface TrackStorage {
    Path createTemporary(UUID sessionId) throws IOException;
    Optional<Path> find(String sha256, AudioFormat format);
    Path commit(Path temporary, String sha256, AudioFormat format) throws IOException;
    void delete(String sha256) throws IOException;
}

public interface AccessService {
    AccessDecision mayUpload(CommandSourceStack source, UUID playerId);
    void setMode(AccessMode mode);
    void allow(UUID playerId);
    void deny(UUID playerId);
    void remove(UUID playerId);
}

public interface DynamicAudioEngine {
    CompletableFuture<Void> play(BlockPos jukebox, TrackReference track, long elapsedMillis);
    void stop(BlockPos jukebox);
    void stopAll();
}
```

### Task 1: Bootstrap the standalone NeoForge repository

**Files:**
- Create: Gradle wrapper and root build files from the official NeoForge 1.21.1 ModDevGradle MDK.
- Create: `src/main/java/dev/muhammedesmer/customjukeboxdiscs/CustomJukeboxDiscs.java`
- Create: `src/main/resources/META-INF/neoforge.mods.toml`
- Create: `LICENSE`, `THIRD_PARTY_LICENSES.md`, `.github/workflows/build.yml`
- Test: `src/test/java/dev/muhammedesmer/customjukeboxdiscs/ModIdentityTest.java`

**Produces:** A Java 21 project whose mod ID and version constraints are machine-tested.

- [ ] Add a failing `ModIdentityTest` asserting `customjukeboxdiscs`, Minecraft `[1.21.1,1.21.2)`, NeoForge `[21.1.231,22)`, and Java 21 metadata.
- [ ] Run `./gradlew test --tests '*ModIdentityTest'`; expect failure because the MDK metadata is not configured.
- [ ] Configure the official MDK, `group=dev.muhammedesmer`, mod ID, version ranges, run configurations, JUnit Platform, and separate GameTest source set.
- [ ] Add the minimal `@Mod(CustomJukeboxDiscs.MOD_ID)` entry point and no gameplay logic.
- [ ] Run `./gradlew test build`; expect PASS and a JAR without run/log/cache files.
- [ ] Commit: `chore: bootstrap Custom Jukebox Discs NeoForge mod`.

### Task 2: Define immutable track data and bounded codecs

**Files:**
- Create: `content/disc/AudioFormat.java`
- Create: `content/disc/TrackReference.java`
- Create: `storage/TrackMetadata.java`
- Create: `content/ModDataComponents.java`
- Test: `TrackReferenceCodecTest.java`

**Produces:** `TrackReference.CODEC`, `TrackReference.STREAM_CODEC`, and registered `customjukeboxdiscs:track_reference`.

- [ ] Write failing tests for round-trip disk/network codecs and rejection of non-lowercase/non-64-character hashes, titles outside 1-64 code points, names outside 1-16 code points, nonpositive/over-600-second duration, and unsupported format values.
- [ ] Run `./gradlew test --tests '*TrackReferenceCodecTest'`; expect failures for missing types.
- [ ] Implement immutable records, explicit bounded `Codec`/`StreamCodec`, constructor validation, and the synchronized/persistent data component.
- [ ] Run the focused tests and `./gradlew test`; expect PASS.
- [ ] Commit: `feat: add bounded track reference component`.

### Task 3: Implement configuration and world-specific permissions

**Files:**
- Create: `config/ClientConfig.java`, `config/ServerConfig.java`
- Create: `permission/AccessMode.java`, `AccessPolicyData.java`, `AccessService.java`, `DefaultAccessService.java`
- Test: `AccessServiceTest.java`, `ServerConfigValidationTest.java`

**Produces:** Server-authoritative `mayUpload`, world SavedData persistence, and validated limits.

- [ ] Write table-driven failing tests for OPS/ALLOWLIST/EVERYONE, operator access, allow entries, deny precedence over operator status, console immunity, remove behavior, serialization, and every numeric boundary.
- [ ] Run the two focused test classes; expect missing-type failures.
- [ ] Implement config specs and `AccessPolicyData` with mode plus UUID allow/deny sets; call `setDirty()` on every mutation.
- [ ] Implement `DefaultAccessService` with deny-first evaluation and permission level 3.
- [ ] Run all unit tests; expect PASS.
- [ ] Commit: `feat: add configurable upload permissions and limits`.

### Task 4: Build content-addressed storage and audio validation

**Files:**
- Create: `storage/AudioInspector.java`, `BoundedAudioInspector.java`, `InspectionResult.java`
- Create: `storage/TrackStorage.java`, `FileTrackStorage.java`, `TrackCatalogData.java`
- Add: bounded MP3 metadata/decoder and OGG parsing dependencies only after recording license obligations in `THIRD_PARTY_LICENSES.md`.
- Test: `AudioInspectorTest.java`, `FileTrackStorageTest.java`, `TrackCatalogDataTest.java`
- Fixtures: `src/test/resources/audio/*`

**Produces:** Safe inspection, deterministic paths, atomic commit, catalog persistence, and deduplication.

- [ ] Add tiny valid MP3/OGG fixtures plus truncated, wrong-signature, oversized-metadata, zero-duration, and deceptive-extension fixtures.
- [ ] Write failing tests proving signature-based type detection, real duration calculation, bounded reads, SHA-256 calculation, two-level hash paths, atomic move, deduplication, and no automatic deletion of orphan validated files.
- [ ] Run the three focused test classes; expect failures.
- [ ] Implement streaming hash/inspection with size checked before parser creation and no unbounded allocation.
- [ ] Implement `<world>/customjukeboxdiscs/{tmp,tracks}` storage, random temporary names, atomic move fallback with fsync, and SavedData catalog.
- [ ] Run all unit tests and a 100-file malformed-input corpus; expect PASS with no file outside the test root changed.
- [ ] Commit: `feat: add validated content-addressed track storage`.

### Task 5: Implement the ordered upload protocol

**Files:**
- Create: `network/ModPayloads.java`
- Create: `network/payload/UploadBeginRequest.java`, `UploadBeginResponse.java`, `UploadChunk.java`, `UploadFinish.java`, `UploadResult.java`
- Create: `transfer/UploadSession.java`, `UploadManager.java`, `UploadError.java`
- Test: `UploadManagerTest.java`, `UploadPayloadCodecTest.java`

**Produces:** `UploadManager.begin/append/finish/cancel` and payloads capped at 32 KiB.

- [ ] Write failing tests for unauthorized begin, one-session limit, already-present hash, declared oversize, chunks over 32 KiB, wrong offsets, duplicate chunks, rate excess, timeout, logout cleanup, client/server hash disagreement, per-player quota, total quota, and storage failure.
- [ ] Run focused tests; expect failures.
- [ ] Implement payload codecs with explicit string/byte-array limits and stable error enums rather than exception text.
- [ ] Implement sequential temporary-file writes and server-calculated SHA-256 without retaining whole tracks in memory.
- [ ] Keep file I/O and inspection off-thread; marshal catalog and inventory changes to the server thread.
- [ ] Run all unit tests; expect PASS and verify temporary files are removed on every failure path.
- [ ] Commit: `feat: add bounded resumable upload sessions`.

### Task 6: Add discs, Disc Writer, and datapack recipes

**Files:**
- Create: `content/ModItems.java`, `ModBlocks.java`, `ModMenus.java`
- Create: `content/disc/ProgrammedDiscItem.java`
- Create: `content/writer/DiscWriterBlock.java`, `DiscWriterBlockEntity.java`, `DiscWriterMenu.java`
- Create: client `screen/DiscWriterScreen.java`, `transfer/ClientUploadManager.java`
- Create: models, textures, translations, loot table, and JSON recipes.
- Test: `DiscWriterGameTests.java`, `ProgrammedDiscGameTests.java`

**Produces:** Blank Disc → Programmed Disc flow and a screen limited to `.minecraft/customjukeboxdiscs/uploads`.

- [ ] Write failing GameTests proving one Blank Disc is required, failed uploads consume nothing, changing/removing the input cancels finalization, successful reuse/upload writes one Programmed Disc, and TrackReference survives save/load and item transfer.
- [ ] Write failing client-unit tests for upload-folder containment, 1,000-file scan cap, supported extensions, stable sorting, and title sanitization.
- [ ] Implement registry content and a one-slot block entity/menu; the server rechecks the slot before replacing the item.
- [ ] Implement the screen with file list, open-folder button, title field, progress, cancel, and stable translated errors; do not add an arbitrary path picker.
- [ ] Add ordinary JSON recipes so modpacks can override them.
- [ ] Run `./gradlew test runGameTestServer`; expect PASS.
- [ ] Commit: `feat: add Disc Writer and programmable discs`.

### Task 7: Implement client download cache and bounded transfer

**Files:**
- Create: `network/payload/JukeboxPlay.java`, `JukeboxStop.java`, `TrackRequest.java`, `TrackChunk.java`, `TrackUnavailable.java`
- Create: client `cache/ClientTrackCache.java`, `ServerCacheKey.java`, `transfer/ClientDownloadManager.java`
- Test: `ClientTrackCacheTest.java`, `ClientDownloadManagerTest.java`, `DownloadPayloadCodecTest.java`

**Produces:** Per-server verified cache, ordered downloads, one hash retry, and 512 MiB LRU eviction.

- [ ] Write failing tests for safe server keys, path containment, exact offsets, duplicate/out-of-order chunks, declared oversize, hash mismatch deletion, one retry only, interrupted download cleanup, cache reuse, LRU ordering, and protection of active tracks.
- [ ] Run focused tests; expect failures.
- [ ] Implement bounded clientbound/serverbound codecs and temporary `.part` files; never buffer a full track in packet handlers.
- [ ] Implement SHA-256 verification, atomic cache commit, and per-server LRU index.
- [ ] Run all unit tests; expect PASS.
- [ ] Commit: `feat: add verified per-server track cache`.

### Task 8: Stream OGG and MP3 through Minecraft/OpenAL

**Files:**
- Create: client `audio/DynamicAudioEngine.java`, `MinecraftDynamicAudioEngine.java`, `Mp3PcmStream.java`, `PlayingTrack.java`
- Create: client `mixin/SoundBufferLibraryMixin.java`
- Modify: `customjukeboxdiscs.mixins.json`, `THIRD_PARTY_LICENSES.md`
- Test: `Mp3PcmStreamTest.java`, `DynamicAudioEngineTest.java`

**Produces:** Positional RECORDS-category streaming with seek-by-discard and deterministic cleanup.

- [ ] Write failing decoder tests for mono/stereo MP3, VBR, truncated frames, bounded PCM production, elapsed-offset discard, EOF, and close idempotence.
- [ ] Write failing engine tests using fake sound/cache adapters for start, replace-at-same-position, stop, stop-all, cache-miss continuation, dimension change, and hash retry failure.
- [ ] Run focused tests; expect failures.
- [ ] Implement OGG using Minecraft's OGG stream and MP3 using the audited pure-Java decoder; expose PCM via the smallest mixin seam needed by `SoundBufferLibrary`.
- [ ] Register sound instances under `SoundSource.RECORDS` with jukebox position and vanilla attenuation; ensure decoding runs outside render/server threads.
- [ ] Run unit tests and manually verify no client-only class loads under `./gradlew runServer`.
- [ ] Commit: `feat: add dynamic MP3 and OGG playback`.

### Task 9: Bridge Programmed Discs into vanilla jukeboxes

**Files:**
- Create: `content/ModSounds.java`, `jukebox/JukeboxPlaybackService.java`
- Create: `mixin/JukeboxBlockEntityMixin.java`
- Create: `data/customjukeboxdiscs/jukebox_song/silent_dynamic.json`, silent OGG asset, and `sounds.json`.
- Test: `JukeboxGameTests.java`, `JukeboxPlaybackServiceTest.java`

**Produces:** Vanilla acceptance with dynamic play/stop duration and late-listener synchronization.

- [ ] Write failing GameTests for direct insertion, hopper insertion, hopper extraction, comparator 15, eject, block break, missing track, validated-duration stop, and no behavior change for vanilla discs.
- [ ] Write failing service tests for nearby-player filtering, login/dimension/range reconciliation, duplicate-play suppression, and stop broadcast.
- [ ] Run focused tests; expect failures.
- [ ] Add the silent `JUKEBOX_PLAYABLE` placeholder to Programmed Disc defaults.
- [ ] Implement one narrow mixin that observes custom-disc start/stop and overrides duration only for Programmed Discs; do not replace vanilla inventory behavior.
- [ ] Implement server active-playback tracking and 20-tick nearby-player reconciliation.
- [ ] Run `./gradlew test runGameTestServer`; expect PASS for custom and vanilla jukebox cases.
- [ ] Commit: `feat: play programmed tracks through vanilla jukeboxes`.

### Task 10: Add administration, recovery, documentation, and release gates

**Files:**
- Create: `command/CustomDiscsCommand.java`
- Create: `docs/CONFIGURATION.md`, `docs/SERVER_ADMIN.md`, `docs/PRIVACY_AND_COPYRIGHT.md`
- Modify: lifecycle handlers, translations, `README.md`, CI workflow.
- Test: `CustomDiscsCommandTest.java`, `RecoveryTest.java`, dedicated-server integration scripts.

**Produces:** Complete operator controls, deterministic recovery, documented privacy, and a release-ready JAR.

- [ ] Write failing command tests for every access/tracks command, pagination, permission level, deny precedence, delete semantics, and reload validation.
- [ ] Write failing recovery tests for expired temporary files, catalog-without-file, unreferenced validated file reporting, interrupted cache/download files, and clean shutdown.
- [ ] Implement Brigadier commands with translatable responses and no raw filesystem paths in player-visible errors.
- [ ] Implement startup recovery and conservative mismatch reporting; never automatically delete unreferenced validated tracks.
- [ ] Document install, client upload folder, defaults, operator commands, cache/storage paths, backups, moderation, content visibility, licensing, and modpack recipe overrides.
- [ ] Run `./gradlew clean test runGameTestServer build`; expect zero failures.
- [ ] Run a manual two-client dedicated-server matrix for MP3, OGG, permissions, cache miss/hit, late arrival, RECORDS volume, dimension change, disconnect, and jukebox removal.
- [ ] Inspect the built JAR: no fixtures, temp audio, secrets, native executables, run files, or undeclared licenses.
- [ ] Commit: `release: prepare Custom Jukebox Discs 1.0.0`.

## Final release acceptance

- `./gradlew clean test runGameTestServer build` passes on Java 21.
- A dedicated NeoForge 21.1.231 server starts without client-only classloading errors.
- Two clients hear validated MP3 and OGG through the same vanilla jukebox with positional RECORDS audio.
- Deny rules override all player grants; failed uploads consume no Blank Disc or permanent quota.
- All file/network bounds are tested, all third-party licenses are packaged, and the release JAR contains no native executable.

