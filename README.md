# BeamCtrl

**Turn your phone's IR blaster into a TV remote.**

[![Latest release](https://img.shields.io/github/v/release/VoidedKN0X/BeamCtrl?label=download)](https://github.com/VoidedKN0X/BeamCtrl/releases/latest) ![Android 13+](https://img.shields.io/badge/Android-13%2B-3DDC84?logo=android&logoColor=white) [![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE) ![Telemetry-free](https://img.shields.io/badge/telemetry-none-brightgreen) [![Sponsor](https://img.shields.io/badge/sponsor-GitHub-ea4aaa?logo=githubsponsors&logoColor=white)](https://github.com/sponsors/VoidedKN0X)

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

## Screenshots

Coming soon.

## Requirements

- Android 13 or newer, on a 64-bit ARM phone (nearly every phone from the last several years)
- A phone with a built-in IR blaster (many Xiaomi, OPPO, Honor, Huawei and Vivo phones have one)
- Permissions: the IR blaster only (no internet)
- **Google-free and de-Googled:** no Google Play services, Firebase or other Google services, and the app never contacts Google. It works the same on de-Googled phones (GrapheneOS, CalyxOS, /e/OS, LineageOS without Google apps).

## Install

1. Download the latest `.apk` file from the [Releases page](https://github.com/VoidedKN0X/BeamCtrl/releases/latest).
2. Open it on your phone. Android may ask you to allow installing apps from your browser or file manager.
3. Updates install over the old version. Your remotes and settings are kept.

**Automatic updates:** on your phone, tap one of the buttons below.

- **Obtainium:** install [Obtainium](https://github.com/ImranR98/Obtainium) first. The button adds BeamCtrl and installs every new release for you.
- **F-Droid:** adds the [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) to F-Droid, Droid-ify or Neo Store, with every Ctrl app and automatic updates.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/BeamCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a> <a href="https://voidedkn0x.github.io/CtrlF-Droid/"><img src=".github/assets/badge_ctrlf-droid.png" alt="Get it from our F-Droid repo" height="80"></a>

If the Obtainium button doesn't open the app, add `https://github.com/VoidedKN0X/BeamCtrl` in Obtainium by hand.

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

**Telemetry-free.** BeamCtrl has no analytics, crash reporting, usage stats or tracking of any kind. It has no internet permission, works fully offline and never connects to the internet.

- Remotes, the code library, Turn off any TV, imports and backups all stay on your phone. BeamCtrl is left out of Android's cloud backup **and** its phone-to-phone transfer, so nothing leaves the phone unless you tap **Back up to a file** and choose where the file goes.
- The Support, Report a bug, Source code and Licence links on the You page (tap your profile picture) open in your browser, only when you tap them.

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

## Support the project

BeamCtrl is free and open source. If it's useful to you, you can support development on [GitHub Sponsors](https://github.com/sponsors/VoidedKN0X). In the app, tap your profile picture: **About** at the bottom of the You page has **Support BeamCtrl** and **Report a bug**, which opens the [issue tracker](https://github.com/VoidedKN0X/BeamCtrl/issues). Starring the repo helps too.

## The Ctrl series

Native Android apps for self-hosted services and everyday tools, all with the same Material You design.

| App | What it's for |
| --- | --- |
| [AdCtrl](https://github.com/VoidedKN0X/AdCtrl) | Control your AdGuard Home DNS server |
| [ArrCtrl](https://github.com/VoidedKN0X/ArrCtrl) | Radarr, Sonarr, Prowlarr, Bazarr, qBittorrent, autobrr and Cleanuparr in one app |
| **BeamCtrl** (this app) | TV remote for phones with an IR blaster |
| [CraftyCtrl](https://github.com/VoidedKN0X/CraftyCtrl) | Manage Minecraft servers on Crafty Controller |
| [ElementCtrl](https://github.com/VoidedKN0X/ElementCtrl) | Matrix chat client for matrix.org and self-hosted Synapse |
| [FilesCtrl](https://github.com/VoidedKN0X/FilesCtrl) | File manager with NFS, SMB, SFTP, FTP and WebDAV |
| [FrigateCtrl](https://github.com/VoidedKN0X/FrigateCtrl) | Watch and review your Frigate NVR cameras |
| [ImmichCtrl](https://github.com/VoidedKN0X/ImmichCtrl) | Photos and backup for your Immich server |
| [JellyCtrl](https://github.com/VoidedKN0X/JellyCtrl) | Movies, shows, music and Live TV from Jellyfin |
| [MemoCtrl](https://github.com/VoidedKN0X/MemoCtrl) | Notes on your Memos server |
| [NaviCtrl](https://github.com/VoidedKN0X/NaviCtrl) | Music player for Navidrome servers |
| [PaperCtrl](https://github.com/VoidedKN0X/PaperCtrl) | Scan and browse documents on Paperless-ngx |
| [PrintCtrl](https://github.com/VoidedKN0X/PrintCtrl) | Bambu Lab printers through Bambuddy, and Spoolman spools |
| [ProxCtrl](https://github.com/VoidedKN0X/ProxCtrl) | Manage your Proxmox VE server |
| [SeerrCtrl](https://github.com/VoidedKN0X/SeerrCtrl) | Request movies and shows on Seerr / Jellyseerr |
| [TrueCtrl](https://github.com/VoidedKN0X/TrueCtrl) | Monitor and manage your TrueNAS server |

## License

BeamCtrl is licensed under the [GNU General Public License v3.0](LICENSE). © VKNXWorks

The built-in code library comes from [Flipper-IRDB](https://github.com/Lucaslhm/Flipper-IRDB) (CC0 1.0), packed with `tools/irdb/pack_irdb.py`.

Fonts, bundled in the app (nothing is downloaded): [Space Grotesk](https://github.com/floriankarsten/space-grotesk) and [JetBrains Mono](https://github.com/JetBrains/JetBrainsMono), both under the SIL Open Font License 1.1 ([FONT-LICENSE-SpaceGrotesk.txt](FONT-LICENSE-SpaceGrotesk.txt), [FONT-LICENSE-JetBrainsMono.txt](FONT-LICENSE-JetBrainsMono.txt)).
