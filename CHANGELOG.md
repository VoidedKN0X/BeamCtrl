# Changelog

What changed in each BeamCtrl release. Every release on the [Releases page](https://github.com/VoidedKN0X/BeamCtrl/releases) also lists the exact changes in that build.

To publish a new version: bump `versionCode` and `versionName` in `app/build.gradle.kts`, add a section here, and push to `main`. Pushes without a version bump only build; they don't make a release.

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
