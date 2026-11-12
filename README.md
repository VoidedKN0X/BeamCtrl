# BeamCtrl

**Turn your phone's IR blaster into a TV remote.**

[![Latest release](https://img.shields.io/github/v/release/VoidedKN0X/BeamCtrl?label=download)](https://github.com/VoidedKN0X/BeamCtrl/releases/latest) ![Android 15+](https://img.shields.io/badge/Android-15%2B-3DDC84?logo=android&logoColor=white) [![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE) ![Telemetry-free](https://img.shields.io/badge/telemetry-none-brightgreen) [![Sponsor](https://img.shields.io/badge/sponsor-GitHub-ea4aaa?logo=githubsponsors&logoColor=white)](https://github.com/sponsors/VoidedKN0X)

BeamCtrl is a remote control app for Android phones with an infrared (IR) blaster. Find your device in the built-in code library, pick your TV brand or import a Flipper Zero file, and control it from your phone.

Part of the [Ctrl series](#the-ctrl-series) by VKNXWorks.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/BeamCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a> <a href="https://voidedkn0x.github.io/CtrlF-Droid/"><img src=".github/assets/badge_ctrlf-droid.png" alt="Get it from our F-Droid repo" height="80"></a>

## Features

- **Code library with 8,500+ remotes** for TVs, soundbars, projectors, air conditioners, fans, lights and more, built into the app, so it works offline
- **Works fully offline.** BeamCtrl doesn't have the internet permission and never goes online
- **Try every button before you save** a remote
- **Turn off any TV**: sends every TV power code in turn until the TV switches off
- **Ready-made remotes** for Samsung, LG, Sony, Philips and Vizio TVs, and **Find my TV** to work out which one yours uses
- **Import Flipper Zero `.ir` files** (for example from Flipper-IRDB), or build a remote button by button
- **Full TV layout**: power, input, d-pad, volume, channels, playback keys and a number pad
- **Hold to repeat** volume, channel and arrow keys
- **Edit any remote**: change a button's code, add buttons, or export it as a Flipper Zero file
- **Supports common IR codes**: NEC, Samsung, Sony SIRC, Philips RC5/RC6, Pronto hex and raw timings
- **Back up to a file** and **restore** every remote and setting, also on another phone
- **Vibrate on press**, keep the screen on while a remote is open, and show or hide the number pad
- **App lock** with its own PIN and optional fingerprint unlock, so nobody else can use or change your remotes
- **Ctrl / Signal look**, shared by every Ctrl app: dark and light, following your phone's dark theme setting, high contrast, one violet accent, Space Grotesk and JetBrains Mono; tablet and foldable layout

## Requirements

- Android 15 or newer, on a 64-bit ARM phone (nearly every phone from the last several years)
- A phone with a built-in IR blaster (many Xiaomi, OPPO, Honor, Huawei and Vivo phones have one)
- Permissions: the IR blaster, and fingerprint unlock for the optional app lock. No internet (see [Is it safe to install?](#is-it-safe-to-install))
- **Google-free and de-Googled:** no Google Play services, Firebase or other Google services, and the app never contacts Google. It works the same on de-Googled phones (GrapheneOS, CalyxOS, /e/OS, LineageOS without Google apps).

## Install

1. Open the [latest release](https://github.com/VoidedKN0X/BeamCtrl/releases/latest) on your phone, scroll down to **Assets** and tap the file that ends in `.apk` (for example `BeamCtrl-v0.6.3.apk`) to download it. The "Source code" zip and tar.gz files are not the app; you don't need them.
2. Open it on your phone. Android may ask you to allow installing apps from your browser or file manager.
3. Updates install over the old version. Your remotes and settings are kept.

**Automatic updates:** on your phone, tap one of the buttons below.

- **Obtainium:** install [Obtainium](https://github.com/ImranR98/Obtainium) first. The button adds BeamCtrl and installs every new release for you.
- **F-Droid:** adds the [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) to F-Droid, Droid-ify or Neo Store, with every Ctrl app and automatic updates.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/BeamCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a> <a href="https://voidedkn0x.github.io/CtrlF-Droid/"><img src=".github/assets/badge_ctrlf-droid.png" alt="Get it from our F-Droid repo" height="80"></a>

If the Obtainium button doesn't open the app, add `https://github.com/VoidedKN0X/BeamCtrl` in Obtainium by hand.

## Is it safe to install?

- **Open source.** All of BeamCtrl's code is in this repo, under the GPL-3.0 licence. Anyone can read it.
- **Built by GitHub, not on a personal computer.** Every release on the [Releases page](https://github.com/VoidedKN0X/BeamCtrl/releases) is built from this code by GitHub Actions, using [the build workflow](.github/workflows/build.yml).
- **Always signed with the same key.** Every release is signed with the same release key. Android refuses to install an update signed with a different key over the app you have, and the CtrlF-Droid repo only accepts APKs signed with this key. Its SHA-256 certificate fingerprint is:

  ```
  36:1C:14:5D:FB:0F:83:25:FB:9F:61:55:C7:0C:6A:84:CB:28:03:92:D6:9F:5D:45:FA:8A:42:C6:C2:FE:B0:54
  ```

  Advanced users can compare it with [AppVerifier](https://github.com/soupslurpr/AppVerifier).
- **Check a download.** Each release's notes end with the APK's SHA-256, so you can check the file you downloaded (`sha256sum BeamCtrl-vX.Y.Z.apk`). With the [GitHub CLI](https://cli.github.com/), `gh attestation verify BeamCtrl-vX.Y.Z.apk -R VoidedKN0X/BeamCtrl` proves the APK was built by this repo's build workflow from the tagged commit. The signing-certificate fingerprint above stays the check Android and AppVerifier use.

**Permissions.** BeamCtrl asks for only three, and **no internet permission**, so it can't go online at all.

| Permission | What BeamCtrl uses it for |
| --- | --- |
| Transmit infrared (`TRANSMIT_IR`) | Sending codes through your phone's IR blaster. This is what makes the remote work. |
| Use biometric hardware (`USE_BIOMETRIC`) | Unlocking the app lock with your fingerprint, only if you turn that on. |
| Hide other apps' floating windows (`HIDE_OVERLAY_WINDOWS`) | Hide other apps' floating windows over the PIN screen, and over the lock screen and confirmations, only while they show. Granted at install, without a prompt. It isn't a network permission: BeamCtrl stays offline. |

`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` also shows up in the app's details: it's an internal permission Android adds to every app built for Android 13 and newer, and only keeps BeamCtrl's own broadcasts private. Every build is checked before release: if the APK ever had an internet or network permission, the build would fail.

More about what stays on your phone in [Privacy](#privacy).

## Getting started

1. Open the **Add** tab and tap **Search all remotes**, then search for your device's brand or model. For a TV you can also pick its brand, or tap **Find my TV**.
2. Point your phone at the device, test a few buttons, then save the remote.
3. Your remote now appears on the **Remotes** tab. Tap it to use it.

Settings, Backup and About are behind your profile picture at the top right of the Remotes and Add tabs: tap it to open the You page, then **Settings**.

## Backup and moving to a new phone

Tap your profile picture at the top right. Under **Backup**:

- **Back up to a file** saves everything in one file, in a place you choose with Android's file picker (Downloads, an SD card, a USB stick or a cloud storage app you've installed). The file holds every remote with all its buttons and codes, and every setting. It never holds the app lock's PIN or fingerprint keys, or whether the lock is on.
- **Restore from a file** reads a backup file, checks all of it, and asks before it **replaces** every remote and setting on the phone. The app lock and its PIN stay as they are on that phone. A file that isn't a BeamCtrl backup, is damaged or was edited by hand, or was made by a newer BeamCtrl is refused with a message, and nothing changes. Files exported by BeamCtrl 0.3.1 and older still restore (remotes only).

**To move to a new phone:** back up on the old phone, copy the file to the new one, install BeamCtrl there and tap **Restore from a file**. Then turn the app lock on again if you use it. BeamCtrl isn't included in Android's cloud backup or its phone-to-phone transfer, so the backup file is the only way your remotes move. The file isn't encrypted (it only holds remotes and settings), so keep it somewhere private.

## Tips and troubleshooting

Phones can only **send** infrared, not receive it, so BeamCtrl can't learn codes from your existing remote. Use the code library, a ready-made remote or a Flipper Zero file instead.

## App lock

Turn it on under **Settings > Privacy and security > App lock** (tap your profile picture at the top right, then **Settings**). It's off until you turn it on.

- **Its own app PIN** of at least 6 digits, separate from your phone's screen lock. Very obvious PINs such as 111111 or 123456 are refused.
- **Unlock with fingerprint** (optional) only accepts strong biometrics, such as the fingerprint sensor, checked by the phone's secure hardware. Face unlock that only uses the camera isn't accepted. The PIN is still needed after the phone restarts and at least every 72 hours, and adding a new fingerprint to the phone turns fingerprint unlock off until you turn it on again.
- **The PIN is never stored.** It's stretched with 600,000 rounds of PBKDF2 and mixed with a key held in the phone's secure hardware, so a copy of the app's files isn't enough to guess it on a computer.
- **Wrong PINs:** after 5 tries you wait 30 seconds, doubling up to 15 minutes. Restarting the app or the phone, or changing the clock, doesn't skip the wait.
- **Lock again:** as soon as you leave the app (the default), after 1, 5 or 15 minutes or 1 hour away, or only when the app restarts. Backing up to a file, restoring from one and opening a link in your browser don't count as leaving, unless you stay away for more than 5 minutes.
- **Change app PIN** asks for your current PIN first, and turning the app lock off asks for the PIN too.
- **Forgot your PIN?** It can't be recovered. The lock screen offers to delete every remote and setting on the phone and turn the lock off; if you backed up to a file, you can restore everything from it afterwards.
- **BeamCtrl has no servers or passwords**, so the app lock only keeps the app closed until it's unlocked.

**Hide in screenshots** and **Hide in recent apps** (the last two switches in the same section, also without the app lock, both off by default): the first keeps BeamCtrl out of screenshots and screen recordings (and blanks its recent-apps preview), the second shows a blank card for BeamCtrl in the recent-apps view while screenshots still work. With the app lock on, the recent-apps view always shows a blank card. The lock screen and the PIN screens are always kept out of screenshots.

## Privacy

**Telemetry-free and Google-free.** BeamCtrl has no analytics, crash reporting, usage stats, ads or tracking of any kind, and no Google libraries or services. It has no internet permission, works fully offline and never connects to anything: there are no network destinations at all.

- Remotes, the code library, Turn off any TV, imports and backups all stay on your phone. BeamCtrl is left out of Android's cloud backup **and** its phone-to-phone transfer, so nothing leaves the phone unless you tap **Back up to a file** and choose where the file goes.
- The Support, Report a bug, Source code and Licence links on the You page (tap your profile picture) open in your browser, only when you tap them.

## How BeamCtrl is made

BeamCtrl is a one-person project by VoidedKN0X (VKNXWorks). Much of the code is written with the help of AI (Claude, by Anthropic). I decide what the app does, review the changes and use the app myself. Every change is listed in the [changelog](CHANGELOG.md), and you can read every line of code here.

## Build from source

You need [Android Studio](https://developer.android.com/studio), or JDK 17 and the Android SDK.

1. Clone the repo:
   ```bash
   git clone https://github.com/VoidedKN0X/BeamCtrl.git
   cd BeamCtrl
   ```
2. Build the app:
   ```bash
   ./gradlew assembleDebug
   ```
   On Windows, use `gradlew.bat assembleDebug`.
3. The APK is in `app/build/outputs/apk/debug/`. Copy it to your phone and install it.

Or open the folder in Android Studio and press **Run** with your phone connected.

**Dependency verification:** every library and Gradle plugin the build downloads is checked against its SHA-256 in `gradle/verification-metadata.xml`, and the build stops if one doesn't match. When you add or update a dependency or plugin, run the **Dependency verification metadata** workflow from the Actions tab, check the diff of the file it prints, and commit the new `gradle/verification-metadata.xml` in the same push.

## Support the project

BeamCtrl is free and open source. If it's useful to you, you can support development on [GitHub Sponsors](https://github.com/sponsors/VoidedKN0X). In the app, tap your profile picture: **About** at the bottom of the You page has **Support BeamCtrl** and **Report a bug**, which opens the [issue tracker](https://github.com/VoidedKN0X/BeamCtrl/issues). Starring the repo helps too.

Found a bug or have an idea? [Open an issue](https://github.com/VoidedKN0X/BeamCtrl/issues/new/choose) and pick **Bug report** or **Feature request**; the form asks for everything needed.

Found a security problem? Please don't open a public issue; see [SECURITY.md](SECURITY.md).

## The Ctrl series

Native Android apps for self-hosted services and everyday tools, all with the same Material You design.

| App | What it's for |
| --- | --- |
| [AdCtrl](https://github.com/VoidedKN0X/AdCtrl) | Control your AdGuard Home DNS server |
| [ArrCtrl](https://github.com/VoidedKN0X/ArrCtrl) | Radarr, Sonarr, Prowlarr, Bazarr, qBittorrent, autobrr and Cleanuparr in one app |
| **BeamCtrl** (this app) | TV remote for phones with an IR blaster |
| [CallCtrl](https://github.com/VoidedKN0X/CallCtrl) | Phone app: dialer, call history and in-call screen |
| [CameraCtrl](https://github.com/VoidedKN0X/CameraCtrl) | Private camera with RAW (DNG) on every lens |
| [ChatCtrl](https://github.com/VoidedKN0X/ChatCtrl) | Matrix chat client for matrix.org and self-hosted Synapse |
| [ClockCtrl](https://github.com/VoidedKN0X/ClockCtrl) | Alarms, timers, stopwatch and world clock |
| [ContactCtrl](https://github.com/VoidedKN0X/ContactCtrl) | Contacts: browse, edit, import and export |
| [CraftyCtrl](https://github.com/VoidedKN0X/CraftyCtrl) | Manage Minecraft servers on Crafty Controller |
| [DateCtrl](https://github.com/VoidedKN0X/DateCtrl) | Calendar for the calendars on your phone and your accounts |
| [FilesCtrl](https://github.com/VoidedKN0X/FilesCtrl) | File manager with NFS, SMB, SFTP, FTP and WebDAV |
| [FrigateCtrl](https://github.com/VoidedKN0X/FrigateCtrl) | Watch and review your Frigate NVR cameras |
| [GalleryCtrl](https://github.com/VoidedKN0X/GalleryCtrl) | Private, offline gallery with an encrypted vault |
| [ImmichCtrl](https://github.com/VoidedKN0X/ImmichCtrl) | Photos and backup for your Immich server |
| [JellyCtrl](https://github.com/VoidedKN0X/JellyCtrl) | Movies, shows, music and Live TV from Jellyfin |
| [LauncherCtrl](https://github.com/VoidedKN0X/LauncherCtrl) | Minimal, private home screen |
| [LijnCtrl](https://github.com/VoidedKN0X/LijnCtrl) | Live De Lijn bus and tram departures |
| [LumenCtrl](https://github.com/VoidedKN0X/LumenCtrl) | Private, offline photo and video editor |
| [MailCtrl](https://github.com/VoidedKN0X/MailCtrl) | Email for Gmail and Proton Mail |
| [MapsCtrl](https://github.com/VoidedKN0X/MapsCtrl) | Private, offline maps and navigation |
| [MemoCtrl](https://github.com/VoidedKN0X/MemoCtrl) | Notes on your Memos server |
| [NaviCtrl](https://github.com/VoidedKN0X/NaviCtrl) | Music player for Navidrome servers |
| [PaperCtrl](https://github.com/VoidedKN0X/PaperCtrl) | Scan and browse documents on Paperless-ngx |
| [PrintCtrl](https://github.com/VoidedKN0X/PrintCtrl) | Bambu Lab printers through Bambuddy, and Spoolman spools |
| [ProxCtrl](https://github.com/VoidedKN0X/ProxCtrl) | Manage your Proxmox VE server |
| [RecorderCtrl](https://github.com/VoidedKN0X/RecorderCtrl) | Private, offline voice recorder |
| [RideCtrl](https://github.com/VoidedKN0X/RideCtrl) | Offline maintenance, fuel and mod log for your vehicles |
| [SeerrCtrl](https://github.com/VoidedKN0X/SeerrCtrl) | Request movies and shows on Seerr / Jellyseerr |
| [ShutterCtrl](https://github.com/VoidedKN0X/ShutterCtrl) | Remote control and photo transfer for Canon cameras |
| [SoundCtrl](https://github.com/VoidedKN0X/SoundCtrl) | JBL headphones and speakers, without the account |
| [SumCtrl](https://github.com/VoidedKN0X/SumCtrl) | Offline calculator, unit converter and work tools |
| [SynapseCtrl](https://github.com/VoidedKN0X/SynapseCtrl) | Administer your Matrix Synapse server |
| [TalkCtrl](https://github.com/VoidedKN0X/TalkCtrl) | Cardo intercom settings, radio and updates |
| [TapoCtrl](https://github.com/VoidedKN0X/TapoCtrl) | Tapo plugs and bulbs on your home network |
| [TextCtrl](https://github.com/VoidedKN0X/TextCtrl) | Private SMS and MMS messaging |
| [TrueCtrl](https://github.com/VoidedKN0X/TrueCtrl) | Monitor and manage your TrueNAS server |
| [WakeCtrl](https://github.com/VoidedKN0X/WakeCtrl) | Keep the screen awake, per app or while charging |
| [WrtCtrl](https://github.com/VoidedKN0X/WrtCtrl) | Manage your OpenWrt router |

## License

BeamCtrl is licensed under the [GNU General Public License v3.0](LICENSE). © VKNXWorks

BeamCtrl isn't made by or affiliated with any TV maker; brand names are only used to say which TVs a code set works with.

The built-in code library comes from [Flipper-IRDB](https://github.com/Lucaslhm/Flipper-IRDB) (CC0 1.0), packed with `tools/irdb/pack_irdb.py`.

Fonts, bundled in the app (nothing is downloaded): [Space Grotesk](https://github.com/floriankarsten/space-grotesk) and [JetBrains Mono](https://github.com/JetBrains/JetBrainsMono), both under the SIL Open Font License 1.1 ([FONT-LICENSE-SpaceGrotesk.txt](FONT-LICENSE-SpaceGrotesk.txt), [FONT-LICENSE-JetBrainsMono.txt](FONT-LICENSE-JetBrainsMono.txt)).
