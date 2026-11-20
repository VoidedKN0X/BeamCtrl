# BeamCtrl

## Hard rules

- **No `INTERNET` permission, ever** (the manifest removes it; CI fails the build if the APK has any network permission).
- **Google-free:** no Google libraries, services or SDKs, including transitive ones (CI checks `releaseRuntimeClasspath` for anything from `com.google.`). Guava's `listenablefuture` and `profileinstaller` are excluded in `app/build.gradle.kts`.
- **Telemetry-free:** no analytics or crash reporting.

## Encryption at rest

- `remotes.json` is sealed with AES-256-GCM (`data/crypto/SealedFile`, marker `BEAMCTRL-REST1`) under the data key (`DataKeyStore`: 32 random bytes in `filesDir/data.key`, wrapped by the Keystore key `beamctrl_data_at_rest` through `KeystoreWrapper`/`KeystoreAes`; no user authentication). Settings (DataStore) are not encrypted; the app lock vault is separate and unchanged.
- Readers must accept both sealed and ordinary files (`SealedFile.isSealed`). Without a data key the app reads and writes the ordinary file as before.
- Never delete or overwrite a file that can't be opened, never replace an existing `data.key`, and never create a Keystore key on the decrypt path (`KeystoreAes.existing`). An ordinary file only becomes a sealed one through `SealedFile.sealInPlace` (temp file, sync, read back, compare, atomic rename). Sealed remotes that can't be opened put `RemoteRepository.unreadable` on: nothing is written and the UI shows the "Can't open your remotes" screen.
- Backups and Flipper `.ir` exports stay ordinary files in their existing formats (they are written from the list in memory).
- All of it runs off the main thread (the repository's load and saves). There is no unit-test source set in this repo; the same classes are tested in RideCtrl (`SealedFileTest`).

## Dependency verification

Gradle checks every library and plugin download against its SHA-256 in `gradle/verification-metadata.xml` (checksums only, no PGP). When a dependency or plugin changes, run the `verification-metadata.yml` workflow (`gh api -X POST repos/VoidedKN0X/BeamCtrl/actions/workflows/verification-metadata.yml/dispatches -f ref=main`), decode the file from its log, check the diff (only Maven Central and Google's Maven) and commit it in the same push. Never turn verification off or make it lenient.

`settings.gradle.kts` lists only `google()` and `mavenCentral()` for plugins, not `gradlePluginPortal()`: the portal serves different files for the Kotlin plugin markers, which made verification fail whenever Maven Central had a hiccup. Don't add it back unfiltered (see the comment in that file).

## Build and signing (CI)

`.github/workflows/build.yml` has four jobs: `version` (is this a release?), `build` (Gradle, **no secrets**, `UNSIGNED_RELEASE=true` gives `app-release-unsigned.apk`; runs every check), `sign` (no checkout, no Gradle: `zipalign` + `apksigner` with the release key, then checks the signer against `EXPECTED_CERT_SHA256` and that the APK's contents didn't change) and `release` (attestation, release notes, publish).

- Never give the `build` job a secret, and never run Gradle or check out the repo in `sign`. New build checks (tests, lint, APK checks) go in `build`.
- `EXPECTED_CERT_SHA256` in `build.yml` must match the fingerprint in `SECURITY.md` and `README.md`; the build fails if it doesn't.
- No Gradle cache in any workflow (`cache-disabled: true`): a cache saved by a failed run used to break every later build.

## Releasing

- A release is published only when `versionName` in `app/build.gradle.kts` is bumped. Bump `versionCode` by 1 at the same time.
- Tags are `vX.Y.Z` and releases are named "BeamCtrl X.Y.Z". Never add a build number (`-build.N`, "(build N)").
- Use the same version for every change that ships together; README, licence and CI changes don't need a bump.

## Changelog

Every new version needs a detailed entry at the top of `CHANGELOG.md`, under a heading for that version (match the existing headings). CI refuses to release without one, and the entry becomes the release notes on GitHub, in Obtainium and in the CtrlF-Droid repo.

- Write for people using the app, not for developers: what's new, what changed, what was fixed.
- Cover every user-visible change in the version, one bullet each, with enough detail to understand it without opening the app.
- Group with `### New`, `### Changed`, `### Fixed` and `### Security` when there's more than a couple of items.
