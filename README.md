# BeamCtrl

**Turn your phone's IR blaster into a TV remote.**

[![Latest release](https://img.shields.io/github/v/release/VoidedKN0X/BeamCtrl?label=download)](https://github.com/VoidedKN0X/BeamCtrl/releases/latest) ![Android 13+](https://img.shields.io/badge/Android-13%2B-3DDC84?logo=android&logoColor=white) [![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE) [![Sponsor](https://img.shields.io/badge/sponsor-GitHub-ea4aaa?logo=githubsponsors&logoColor=white)](https://github.com/sponsors/VoidedKN0X)

BeamCtrl is a remote control app for Android phones with an infrared (IR) blaster. Pick your TV brand or import a Flipper Zero file, and control your TV from your phone.

Part of the [Ctrl series](#the-ctrl-series) by VKNXWorks.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/BeamCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a> <a href="https://voidedkn0x.github.io/CtrlF-Droid/"><img src=".github/assets/badge_ctrlf-droid.png" alt="Get it from our F-Droid repo" height="80"></a>

## Features

- **Ready-made remotes** for Samsung, LG, Sony, Philips and Vizio TVs, with test buttons before you save
- **Find my TV** tries each brand's power code until your TV responds
- **Import Flipper Zero `.ir` files** (for example from Flipper-IRDB), or build a remote button by button
- **Full TV layout**: power, input, d-pad, volume, channels, playback keys and a number pad
- **Hold to repeat** volume, channel and arrow keys
- **Edit any remote**: change a button's code, add buttons, or export it as a Flipper Zero file
- **Supports common IR codes**: NEC, Samsung, Sony SIRC, Philips RC5/RC6, Pronto hex and raw timings
- **Backup and restore** all remotes as a single file
- Light, dark and pure black themes with Material You colours; tablet and foldable layout

## Requirements

- Android 13 or newer
- A phone with a built-in IR blaster (many Xiaomi, OPPO, Honor, Huawei and Vivo phones have one)

## Install

1. Download the latest `.apk` file from the [Releases page](https://github.com/VoidedKN0X/BeamCtrl/releases/latest).
2. Open it on your phone. Android may ask you to allow installing apps from your browser or file manager.
3. Updates install over the old version. Your settings and login are kept.

**Automatic updates:** on your phone, tap one of the buttons below.

- **Obtainium:** install [Obtainium](https://github.com/ImranR98/Obtainium) first. The button adds BeamCtrl and installs every new release for you.
- **F-Droid:** adds the [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) to F-Droid, Droid-ify or Neo Store, with every Ctrl app and automatic updates.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/BeamCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a> <a href="https://voidedkn0x.github.io/CtrlF-Droid/"><img src=".github/assets/badge_ctrlf-droid.png" alt="Get it from our F-Droid repo" height="80"></a>

If the Obtainium button doesn't open the app, add `https://github.com/VoidedKN0X/BeamCtrl` in Obtainium by hand.

## Getting started

1. Open the **Add** tab and pick your TV brand, or tap **Find my TV**.
2. Test a few buttons, then save the remote.
3. Your remote now appears on the **Remotes** tab. Tap it to use it.

## Tips and troubleshooting

Phones can only **send** infrared, not receive it, so BeamCtrl can't learn codes from your existing remote. Use a ready-made remote or a Flipper Zero file instead.

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

BeamCtrl is free and open source. If it's useful to you, you can support development on [GitHub Sponsors](https://github.com/sponsors/VoidedKN0X). Starring the repo and [reporting bugs](https://github.com/VoidedKN0X/BeamCtrl/issues) helps too.

## The Ctrl series

Native Android apps for self-hosted services and everyday tools, all with the same Material You design.

| App | What it's for |
| --- | --- |
| **BeamCtrl** (this app) | TV remote for phones with an IR blaster |
| [CraftyCtrl](https://github.com/VoidedKN0X/CraftyCtrl) | Manage Minecraft servers on Crafty Controller |
| [ElementCtrl](https://github.com/VoidedKN0X/ElementCtrl) | Matrix chat client for matrix.org and self-hosted Synapse |
| [FilesCtrl](https://github.com/VoidedKN0X/FilesCtrl) | File manager with NFS, SMB, SFTP, FTP and WebDAV |
| [FrigateCtrl](https://github.com/VoidedKN0X/FrigateCtrl) | Watch and review your Frigate NVR cameras |
| [ImmichCtrl](https://github.com/VoidedKN0X/ImmichCtrl) | Photos and backup for your Immich server |
| [JellyCtrl](https://github.com/VoidedKN0X/JellyCtrl) | Movies, shows, music and Live TV from Jellyfin |
| [NaviCtrl](https://github.com/VoidedKN0X/NaviCtrl) | Music player for Navidrome servers |
| [SeerrCtrl](https://github.com/VoidedKN0X/SeerrCtrl) | Request movies and shows on Seerr / Jellyseerr |

## License

BeamCtrl is licensed under the [GNU General Public License v3.0](LICENSE). © VKNXWorks
