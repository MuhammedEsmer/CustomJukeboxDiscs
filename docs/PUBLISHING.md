# Publishing to CurseForge

Everything the upload form asks for, and where it lives in this repository.

## What to have open

| The form asks for | Take it from |
| --- | --- |
| Project avatar | `docs/branding/curseforge-avatar.png` (400x400) |
| Summary | one line, see below |
| Description | `docs/MOD_PAGE.md`, the English half |
| The file | `releases/customjukeboxdiscs-1.21.1-<version>.jar` |
| Changelog | `docs/changelogs/<version>.md` |

Suggested summary, short enough for the card under the project name:

> Put your own MP3 and OGG music on real jukebox discs, with server-side permissions and quotas.

## Creating the project

1. Sign in at CurseForge's author portal and start a new project for **Minecraft**.
2. Category: **Mods**. Pick secondary categories that match: *Miscellaneous*, *Server Utility*,
   *Cosmetic* all fit; skip *Addons*.
3. Name: `Custom Jukebox Discs`. The URL slug is derived from it and cannot be changed later, so get
   the name right the first time.
4. Upload the avatar, paste the summary and the description.
5. Submit. A new project is reviewed by CurseForge staff before it appears publicly. This normally
   takes anywhere from a few hours to a couple of days.

## Uploading a file

1. Project page, **Files** tab, upload the jar from `releases/`.
2. **Release type**: `Release` once you are happy with it, `Beta` while you are still finding
   problems. 0.2.x is honest as Beta.
3. **Game version**: `1.21.1`. Also tick the **NeoForge** modloader; a file with no modloader tag
   will not be offered to the launcher correctly.
4. **Changelog**: paste the matching file from `docs/changelogs/`.
5. **Relations**: add *Sophisticated Backpacks* and *Sophisticated Core* as **Optional dependency**
   if you want the integration discovered. Do not add NeoForge, it is implied by the modloader tag.

## Two decisions to make before you press publish

### Licence

The repository currently says **All Rights Reserved**, and `LICENSE` explicitly withholds permission
to distribute. That is a fine choice, but be aware of what it means on CurseForge:

- Players can still download and use it.
- **Nobody else can put it in a modpack.** Your own pack is fine because you own the mod, but any
  other pack author is blocked.

If you want the mod to spread through packs, switch `mod_license` in `gradle.properties` and the
`LICENSE` file to something permissive such as MIT, and enable modpack distribution in the project
settings. If you would rather keep control, leave it as it is and expect pack authors to ask you
directly.

### Both sides required

The mod must be installed on the client *and* the server. Say so in the description, otherwise the
first server owner who installs it alone will open an issue about it.

## After it is live

- `mod_version` in `gradle.properties` is the single source of truth. Bump it, run
  `./gradlew build`, and the jar lands in `releases/` under the new name automatically.
- Write `docs/changelogs/<version>.md` at the same time; that file is what you paste into the upload.
- Tag the commit (`git tag -a v0.2.3 -m "..."`) so a published version can always be rebuilt from
  source.
