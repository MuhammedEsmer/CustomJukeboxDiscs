# Server Library Design

## Goal

Give every player access to tracks already uploaded to the server without uploading or storing duplicate audio. The Disc Writer will expose local computer files and the shared server catalogue as separate sources in Minecraft 1.21.1 and 1.12.2 with equivalent behavior.

## Product Language

The two Disc Writer tabs are named:

- **Local Files**: MP3 and OGG files found in the player's local upload directory.
- **Server Library**: tracks stored in the server catalogue and available for any player to write to a blank disc.

All existing catalogue entries, including tracks uploaded before this feature, appear in Server Library. Every successful new upload appears there automatically. A track removed from the catalogue disappears from subsequent library responses.

## User Experience

### Local Files

Local Files keeps the existing upload workflow:

- Show files from the local upload directory.
- Allow title entry for a new upload.
- Keep the open-folder, refresh, and write actions.
- Show upload progress and status.
- Add a successful upload to Server Library without a second copy or migration step.

### Server Library

Server Library shows a paginated list supplied by the server. Each row contains:

- Track title
- Uploader name
- Duration
- Audio format (`MP3` or `OGG`)

The selected row is visually distinct. The tab has previous-page, next-page, refresh, and write actions. A library track retains its catalogue title; the player cannot rename it while writing another disc.

The client receives metadata only while browsing. Audio is downloaded through the existing playback path only if the resulting disc is later played and the player's playback preference is enabled.

The screen keeps status text in a fixed area that cannot overlap lists, controls, or inventory slots. The selected tab is preserved for the lifetime of the open screen.

## Concise Feedback Text

New and existing upload and disc-writing feedback must be audited in both versions. Messages should state the result or required action in the fewest clear words. Technical causes remain in logs when they do not help the player act.

Preferred English wording includes:

| Situation | Text |
|---|---|
| Loading the catalogue | `Loading library…` |
| Catalogue has no entries | `Library is empty` |
| Successful write | `Disc written` |
| Missing input | `Insert a blank disc` |
| Removed or missing track | `Track unavailable` |
| Player left the writer | `Too far from writer` |
| Upload queued | `Upload queued` |
| Upload in progress | `Uploading: {percent}%` |
| Successful upload | `Upload complete` |
| Generic upload failure | `Upload failed: {reason}` |

Minecraft 1.21.1 keeps English and Turkish translations aligned. Minecraft 1.12.2 keeps the equivalent English text in its legacy language file.

## Architecture

The existing server-side track catalogue is the single source of truth. No second global database or duplicated audio pool is introduced.

The feature adds two request paths:

1. **Catalogue page request**: the client requests one bounded page; the server responds with metadata and paging totals.
2. **Catalogue write request**: the client sends the selected track hash and the currently open writer identity; the server resolves the authoritative catalogue entry and writes a new programmed disc.

The modern and legacy implementations use version-appropriate packet APIs but share the same payload semantics, validation order, page size, sorting, and visible behavior.

## Catalogue Paging

Catalogue entries are sorted consistently by creation time and then SHA-256 hash, using the existing catalogue ordering. Pages are one-based and contain at most 20 entries. The server clamps the requested page to the current valid range and never sends the complete catalogue in one packet.

The response contains:

- Requested page
- Total page count
- Total track count
- Entries for the page

Opening Server Library requests page one. Refresh requests the current page and falls back to the last available page if removals reduced the page count.

## Authorization and Validation

Uploading and library writing are separate permissions. A player denied permission to upload may still browse Server Library and write any available catalogue track, as required for the shared pool.

The server never trusts client metadata. A library write succeeds only if all checks pass:

1. The player has the Disc Writer menu open.
2. The referenced block entity is still the same writer.
3. The player is within the existing writer interaction distance.
4. The writer input still contains the required blank disc and matches its server-side fingerprint.
5. The requested SHA-256 hash resolves to a current catalogue entry.
6. The referenced audio file still exists in server storage.

The server writes the disc from its own `TrackReference`. Client-supplied titles, uploader data, durations, and formats are never accepted. Multiple players may write the same track concurrently because this operation reads immutable track metadata and does not modify the stored audio.

If a stale screen requests a deleted or missing track, the server returns `Track unavailable`; the client refreshes the current page. Invalid writer state returns the shortest actionable writer error without exposing internal details.

## Storage and Network Behavior

- Existing audio files are reused by content hash.
- Writing from Server Library does not upload, download, copy, or re-inspect audio.
- Browsing sends bounded metadata packets only.
- Playback opt-out continues to prevent audio downloads; browsing metadata is not playback and remains available.
- Existing catalogue quotas and upload access rules are unchanged.
- Existing deletion and recovery behavior remains authoritative.

## Version Parity

Minecraft 1.21.1 and 1.12.2 must provide the same:

- Tab names and meanings
- Catalogue contents and ordering
- Row metadata
- Page size and navigation behavior
- Permission behavior
- Server-side validation
- Concise feedback semantics
- Concurrent write behavior

Visual implementation may differ only where the Minecraft GUI APIs require it.

## Error Handling

- A catalogue request made without an open writer is rejected without data.
- An invalid page produces a bounded valid response or a concise failure; it cannot allocate an unbounded list.
- A missing storage file is reported as unavailable and is not silently recreated.
- A write race caused by changing the input slot fails without consuming or replacing another item.
- Malformed hashes and oversized packets are rejected by packet decoding or server validation.
- Closing the screen makes late responses harmless; they do not open screens or alter inventory.

## Verification

Tests are intentionally deferred until all requested cross-version feature changes are implemented, then run together before building release JARs.

Coverage must include both versions where their test infrastructure permits:

- Historical catalogue entries appear without migration.
- New successful uploads appear in Server Library.
- Upload-restricted players can browse and write library tracks.
- Library writing reuses existing metadata and audio.
- Blank-disc, writer identity, distance, fingerprint, missing-track, and missing-file checks fail safely.
- Deleted tracks disappear after refresh and stale writes fail.
- Paging order, boundaries, and reduced page counts are deterministic.
- Concurrent writes of one track succeed independently.
- Metadata responses remain bounded for large catalogues.
- Browsing never starts an audio download.
- Existing and new feedback strings remain concise and non-overlapping.
- Minecraft 1.21.1 and 1.12.2 expose equivalent behavior.

## Out of Scope

- Searching, filtering, favorites, playlists, ratings, and previews
- Per-track sharing controls or private uploads
- Renaming catalogue entries from the Disc Writer
- A second catalogue or duplicated global audio directory
- Changes to upload quotas, deletion commands, or playback consent
