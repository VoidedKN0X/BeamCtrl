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
- **Backup and restore** all remotes as a single file
- **Vibrate on press**, keep the screen on while a remote is open, and show or hide the number pad
- Light, dark and pure black themes, with an ember accent or your wallpaper's Material You colours; tablet and foldable layout

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

## Tips and troubleshooting

Phones can only **send** infrared, not receive it, so BeamCtrl can't learn codes from your existing remote. Use the code library, a ready-made remote or a Flipper Zero file instead.

## Privacy

**Telemetry-free.** BeamCtrl has no analytics, crash reporting, usage stats or tracking of any kind. It has no internet permission, works fully offline and never connects to the internet.

- Remotes, the code library, Turn off any TV, imports and backups all stay on your phone.
- The Support, Report a bug, Source code and Licence links in Settings open in your browser, only when you tap them.

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

BeamCtrl is free and open source. If it's useful to you, you can support development on [GitHub Sponsors](https://github.com/sponsors/VoidedKN0X). In the app, **Settings → About** has **Support BeamCtrl** and **Report a bug**, which opens the [issue tracker](https://github.com/VoidedKN0X/BeamCtrl/issues). Starring the repo helps too.

## The Ctrl series

Native Android apps for self-hosted services and everyday tools, all with the same Material You design.

| App | What it's for |
| --- | --- |
| [ArrCtrl](https://github.com/VoidedKN0X/ArrCtrl) | Radarr, Sonarr, Prowlarr, Bazarr, qBittorrent, autobrr and Cleanuparr in one app |
| **BeamCtrl** (this app) | TV remote for phones with an IR blaster |
| [CraftyCtrl](https://github.com/VoidedKN0X/CraftyCtrl) | Manage Minecraft servers on Crafty Controller |
| [ElementCtrl](https://github.com/VoidedKN0X/ElementCtrl) | Matrix chat client for matrix.org and self-hosted Synapse |
| [FilesCtrl](https://github.com/VoidedKN0X/FilesCtrl) | File manager with NFS, SMB, SFTP, FTP and WebDAV |
| [FrigateCtrl](https://github.com/VoidedKN0X/FrigateCtrl) | Watch and review your Frigate NVR cameras |
| [ImmichCtrl](https://github.com/VoidedKN0X/ImmichCtrl) | Photos and backup for your Immich server |
| [JellyCtrl](https://github.com/VoidedKN0X/JellyCtrl) | Movies, shows, music and Live TV from Jellyfin |
| [NaviCtrl](https://github.com/VoidedKN0X/NaviCtrl) | Music player for Navidrome servers |
| [PaperCtrl](https://github.com/VoidedKN0X/PaperCtrl) | Scan and browse documents on Paperless-ngx |
| [PrintCtrl](https://github.com/VoidedKN0X/PrintCtrl) | Bambu Lab printers through Bambuddy, and Spoolman spools |
| [ProxCtrl](https://github.com/VoidedKN0X/ProxCtrl) | Manage your Proxmox VE server |
| [SeerrCtrl](https://github.com/VoidedKN0X/SeerrCtrl) | Request movies and shows on Seerr / Jellyseerr |
| [TrueCtrl](https://github.com/VoidedKN0X/TrueCtrl) | Monitor and manage your TrueNAS server |

## License

BeamCtrl is licensed under the [GNU General Public License v3.0](LICENSE). © VKNXWorks

The built-in code library comes from [Flipper-IRDB](https://github.com/Lucaslhm/Flipper-IRDB) (CC0 1.0), packed with `tools/irdb/pack_irdb.py`.
