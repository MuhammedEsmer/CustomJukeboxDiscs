# Server Library Implementation Plan

> **For agentic workers:** Execute tasks in order. Commit each completed task. Tests and release builds are deferred to the final task by explicit user request.

**Goal:** Add matching Local Files and Server Library Disc Writer tabs to Minecraft 1.21.1 and 1.12.2, backed by the existing server catalogue.

**Architecture:** Add bounded catalogue page and direct-write messages to each version's existing network layer. The server remains authoritative for catalogue metadata, writer state, distance, blank-disc fingerprint, and stored audio availability. Each client screen owns only its current page and display state.

**Tech Stack:** Java 21/NeoForge 1.21.1, Java 8/Forge 1.12.2, Minecraft GUI and packet APIs, Gradle.

**Spec:** `docs/specs/2026-08-31-server-library-design.md`

## Global Constraints

- Both versions expose equivalent behavior and wording.
- Server Library pages contain at most 20 entries.
- Browsing sends metadata only and never downloads audio.
- Upload permission does not restrict browsing or library writing.
- Existing catalogue/audio storage is reused without duplication.
- Feedback text is short but actionable, including existing upload and write messages.
- Do not run tests, build JARs, or copy releases until Task 6.

---

### Task 1: Modern catalogue messages and server operations

**Files:**
- Create: `src/main/java/dev/muhammedesmer/customjukeboxdiscs/network/payload/LibraryPageRequest.java`
- Create: `src/main/java/dev/muhammedesmer/customjukeboxdiscs/network/payload/LibraryPageResponse.java`
- Create: `src/main/java/dev/muhammedesmer/customjukeboxdiscs/network/payload/LibraryWriteRequest.java`
- Create: `src/main/java/dev/muhammedesmer/customjukeboxdiscs/network/payload/LibraryWriteResponse.java`
- Modify: `src/main/java/dev/muhammedesmer/customjukeboxdiscs/network/ModPayloads.java`
- Modify: `src/main/java/dev/muhammedesmer/customjukeboxdiscs/server/ServerRuntime.java`
- Modify: `src/main/java/dev/muhammedesmer/customjukeboxdiscs/storage/TrackCatalogData.java`

**Interfaces:**
- `LibraryPageRequest(int page)` requests a one-based page.
- `LibraryPageResponse(int page, int pageCount, int totalTracks, List<TrackReference> tracks)` contains no audio bytes.
- `LibraryWriteRequest(String sha256, long inputFingerprint)` identifies one authoritative catalogue entry and the observed writer input.
- `LibraryWriteResponse(Result result)` uses `WRITTEN`, `INVALID_WRITER`, or `TRACK_UNAVAILABLE`.
- `TrackCatalogData.pageClamped(int requestedPage, int pageSize)` returns a valid page in `1..pageCount`.

- [ ] Add bounded stream codecs: page must be positive after decode, hashes use the existing 64-character bound, and response lists decode at no more than 20 entries.
- [ ] Register both server-bound requests and both client-bound responses in `ModPayloads`; extend `ServerHandler` and `ClientHandler` including their safe fallback implementations.
- [ ] Handle page requests only while `DiscWriterMenu` is open; return `catalog.pageClamped(request.page(), 20)`.
- [ ] Handle writes by resolving the current catalogue entry and storage file, checking the open menu/writer and fingerprint, then calling `DiscWriterBlockEntity.writeDisc` with the server-owned `TrackReference`.
- [ ] Notify the open menu after a successful write and return the shortest actionable result.
- [ ] Run `git diff --check`, inspect packet bounds, and commit as `Add the modern server library protocol`.

### Task 2: Modern Disc Writer tabs and concise messages

**Files:**
- Create: `src/main/java/dev/muhammedesmer/customjukeboxdiscs/client/transfer/ClientLibraryManager.java`
- Modify: `src/main/java/dev/muhammedesmer/customjukeboxdiscs/client/ClientModEvents.java`
- Modify: `src/main/java/dev/muhammedesmer/customjukeboxdiscs/client/screen/DiscWriterScreen.java`
- Modify: `src/main/resources/assets/customjukeboxdiscs/lang/en_us.json`
- Modify: `src/main/resources/assets/customjukeboxdiscs/lang/tr_tr.json`
- Modify: `src/main/resources/assets/customjukeboxdiscs/textures/gui/disc_writer.png` only if existing texture regions cannot support the tabs cleanly.

**Interfaces:**
- `ClientLibraryManager.requestPage(int page)`, `write(String sha256, long fingerprint)`, `handle(LibraryPageResponse)`, and `handle(LibraryWriteResponse)` route responses to the currently open writer screen without retaining audio.
- `DiscWriterScreen` exposes response callbacks that ignore late responses after closure.

- [ ] Add `Local Files` and `Server Library` tab controls without moving or overlapping inventory slots.
- [ ] Preserve the local file/title/upload workflow under Local Files.
- [ ] Render up to four visible rows from the current 20-entry server page with scrolling, title, uploader, duration, and format; add previous, next, refresh, and write actions.
- [ ] Request page one on first opening Server Library and refresh the current clamped page after a stale write.
- [ ] Keep the server title immutable and do not request/download track audio while browsing.
- [ ] Shorten existing upload/write feedback and add aligned English/Turkish library text.
- [ ] Run `git diff --check`, inspect all screen coordinate bounds statically, and commit as `Add the modern Disc Writer library tabs`.

### Task 3: Legacy catalogue messages and server operations

**Files:**
- Create: `forge-1.12.2/src/main/java/dev/hoodoo/customjukeboxdiscs/network/packet/PacketLibraryPageRequest.java`
- Create: `forge-1.12.2/src/main/java/dev/hoodoo/customjukeboxdiscs/network/packet/PacketLibraryPageResponse.java`
- Create: `forge-1.12.2/src/main/java/dev/hoodoo/customjukeboxdiscs/network/packet/PacketLibraryWriteRequest.java`
- Create: `forge-1.12.2/src/main/java/dev/hoodoo/customjukeboxdiscs/network/packet/PacketLibraryWriteResponse.java`
- Modify: `forge-1.12.2/src/main/java/dev/hoodoo/customjukeboxdiscs/network/ModNetwork.java`
- Modify: `forge-1.12.2/src/main/java/dev/hoodoo/customjukeboxdiscs/server/ServerRuntime.java`
- Modify: `forge-1.12.2/src/main/java/dev/hoodoo/customjukeboxdiscs/storage/TrackCatalogSavedData.java`

**Interfaces:** Legacy packets carry the same fields, list bound, result values, and validation semantics as Task 1 using `IMessage` and scheduled main-thread handlers.

- [ ] Implement explicit `ByteBuf` bounds for pages, count `0..20`, strings, enum ordinals, and track metadata.
- [ ] Register packet IDs in `ModNetwork` without changing existing IDs.
- [ ] Clamp catalogue pages and serve them only to players with `ContainerDiscWriter` open.
- [ ] Resolve writes exclusively from `TrackCatalogSavedData`, verify storage, container/writer identity, distance through `isUsableByPlayer`, and fingerprint, then call `TileEntityDiscWriter.writeDisc`.
- [ ] Synchronize the container after success and send the equivalent result.
- [ ] Run `git diff --check`, compare modern/legacy fields and validation order, and commit as `Add the legacy server library protocol`.

### Task 4: Legacy Disc Writer tabs and concise messages

**Files:**
- Create: `forge-1.12.2/src/main/java/dev/hoodoo/customjukeboxdiscs/client/transfer/ClientLibraryManager.java`
- Modify: `forge-1.12.2/src/main/java/dev/hoodoo/customjukeboxdiscs/client/screen/GuiDiscWriter.java`
- Modify: `forge-1.12.2/src/main/resources/assets/customjukeboxdiscs/lang/en_us.lang`
- Modify: `forge-1.12.2/src/main/resources/assets/customjukeboxdiscs/textures/gui/disc_writer.png` only if required for readable tabs.

**Interfaces:** Match Task 2 using Java 8-compatible types and Forge 1.12 scheduled client packet handlers.

- [ ] Add the same two tabs, four-row viewport, metadata columns, page navigation, refresh, and write action.
- [ ] Preserve local upload behavior and make server titles immutable.
- [ ] Ignore late responses unless `Minecraft.currentScreen` is the same writer screen instance.
- [ ] Use the same concise English semantics as the modern language file.
- [ ] Run `git diff --check`, compare both UI state machines and coordinates, and commit as `Add the legacy Disc Writer library tabs`.

### Task 5: Cross-version static parity review

**Files:** All files changed in Tasks 1-4.

- [ ] Compare message fields, page size, ordering, writer validation, missing-file handling, concurrency behavior, and refresh rules side by side.
- [ ] Audit every upload and Disc Writer status translation for concise wording and remove only obsolete keys created by these changes.
- [ ] Confirm browsing code has no call to `TrackRequest`, download queues, or cache writes.
- [ ] Confirm unrelated working-tree changes are untouched.
- [ ] Commit any parity correction as `Align server library behavior across versions`.

### Task 6: Deferred tests, builds, and release artifacts

**Files:**
- Add or modify focused tests under each version's existing `src/test` tree when packet/catalog logic can run outside Minecraft.
- Update changelogs/version metadata only when the user starts the release step.

- [ ] Add catalogue clamp/boundary tests, packet round-trip/bounds tests, write validation tests, and concise translation assertions where supported.
- [ ] Run modern unit tests and compile.
- [ ] Run legacy unit tests and compile.
- [ ] Run configured GameTests/CI-equivalent checks.
- [ ] Build both JARs only after all preceding checks pass.
- [ ] Report exact artifact paths and commit test/release changes separately.
