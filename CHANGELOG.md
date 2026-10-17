# Changelog

What changed in each BeamCtrl release. Every release on the [Releases page](https://github.com/VoidedKN0X/BeamCtrl/releases) also lists the exact changes in that build.

To publish a new version: bump `versionCode` and `versionName` in `app/build.gradle.kts`, add a section here, and push to `main`. Pushes without a version bump only build; they don't make a release.

## v0.1.6 (2026-10-18)

A security check of the whole app. Remotes, buttons and settings work exactly as before.

### Security

- **Imported files are checked before they're used.** Flipper `.ir` files and backups bigger than 4 MB are refused straight away ("That file is too big") instead of being loaded into memory, so a huge or broken file can't freeze or crash the app.
- **Limits on what a file can contain.** An import keeps at most 500 signals per `.ir` file, 500 remotes per backup and 500 buttons per remote. Names are cut to a sensible length and can't contain hidden control characters or line breaks.
- **Only codes your phone can actually send are kept.** Raw and Pronto codes from files are checked for a sane carrier frequency (10 to 500 kHz), at most 2,048 timings, and a total length of two seconds or less (Android's own limit). Codes that fail are listed as skipped instead of being saved and failing later.
- **Backups can't clash with your remotes.** Every remote and button in a restored backup gets a fresh id if its id is missing, malformed or already in use, and files that aren't BeamCtrl backups are rejected.
- **Exported `.ir` files can't be tampered with through a button name.** Line breaks in remote and button names are removed on export, so a name can't add extra (fake) signals to the file you share.
- **Your saved remotes are never silently wiped.** If the remotes file on the phone can't be read, it's now kept aside instead of being overwritten by the next change.
- **Android backup is end-to-end encrypted only.** Remotes and settings go into your Google backup only when the phone has a screen lock (so the backup is encrypted), and still move over in a phone-to-phone transfer.
- **No internet access, guaranteed.** BeamCtrl never goes online; the app now explicitly removes the internet and network-state permissions so no library can add them. The only permission it asks for is the IR blaster.

## v0.1.5 (2026-10-18)

- Version numbers are now plain, with no build number on the end, and a new version only comes out when something in the app actually changes. Nothing else in the app has changed.

## v0.1.2 (2026-10-17)

First release.

- **Remotes tab** with a TV-style remote and an edit mode to arrange your own buttons
- **Brand remotes** for Samsung, LG, Sony, Philips and Vizio, with a brand finder
- **Flipper Zero `.ir` import and export**
- Supports NEC, Samsung, Sony, RC5, RC6, Pronto and raw IR codes
- Settings and JSON backup of your remotes
- Same design as the other Ctrl apps
