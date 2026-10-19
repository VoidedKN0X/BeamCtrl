# Changelog

What changed in each BeamCtrl release. Every release on the [Releases page](https://github.com/VoidedKN0X/BeamCtrl/releases) also lists the exact changes in that build.

To publish a new version: bump `versionCode` and `versionName` in `app/build.gradle.kts`, add a section here, and push to `main`. Pushes without a version bump only build; they don't make a release.

## v0.2.5 (2026-10-19)

### New

- **Support BeamCtrl** under Settings → About opens VKNXWorks' GitHub Sponsors page in your browser. BeamCtrl is free and open source, and this is the place to sponsor its development if you'd like to.
- **New About group at the bottom of Settings.** Besides **Support BeamCtrl** it has **Source code and issues**, which opens BeamCtrl's GitHub page where you can read the code and report bugs, and **Licence**, which opens the GNU General Public License v3.0 that BeamCtrl is released under. The app version line stays underneath.
- The README's **Support the project** section now also points to the **Support BeamCtrl** button.

## v0.2.4 (2026-10-19)

BeamCtrl is now completely Google-free. Nothing changes in how you use it.

### Privacy

- **Emoji no longer make BeamCtrl ask Google for a font.** A standard Android library inside the app asked Google Play services for a downloadable emoji font every time BeamCtrl started, which could make Google Play services fetch that font from Google. This lookup is now switched off: emoji are drawn with your phone's own emoji font. On phones without Google Play services nothing changes; on other phones, emoji can look slightly different if the phone's built-in emoji font is older.
- **Checked: BeamCtrl is Google-free.** It contains no Google Play services, Firebase, ML Kit, Google Cast or Play Core, and it doesn't load fonts or anything else from Google's servers.

### Changed

- **README: new Google-free line** under Requirements: works without Google Play services, contains no Google libraries that contact Google, and draws emoji with your phone's own emoji font.
- **README brought up to date with BeamCtrl 0.2.3.** The install steps no longer say your "login" is kept on update (BeamCtrl has no account); they now say your remotes and settings are kept. The feature list now also mentions the Vibrate on press, Keep screen on and Number pad settings, and that the colours are either BeamCtrl's ember accent or your wallpaper's Material You colours. Requirements now list the permissions the app asks for: the IR blaster, and internet only for the optional usage stats. No change to the app itself

## v0.2.3 (2026-10-19)

A privacy review of the new optional usage stats. They're still off unless you said yes, and the review confirmed that they're the only thing in BeamCtrl that uses the internet: remotes, the code library, Turn off any TV, imports and backups never go online, and the stats only ever go to the stats server over HTTPS, without redirects, cookies or caching. This release makes switching them off take effect even more strictly.

### Security

- **Switching usage stats off now throws away anything not yet sent.** Before, a stat recorded just before you switched them off could still be waiting in the app, and if you switched stats back on within a few seconds it could have been sent after all. Now everything waiting is discarded the moment you switch stats off, so nothing recorded before that point can ever go out

### Changed

- **README: the usage stats list is now exact.** It now also mentions that saving a remote from the code library is counted (with its device type), and that Turn off any TV records whether it carried on from where it stopped. Nothing new is collected; these were already listed in the 0.2.2 notes
- **All Ctrl apps in the README.** The README's Ctrl series table now lists all 14 apps. New in the table: [ArrCtrl](https://github.com/VoidedKN0X/ArrCtrl) (Radarr, Sonarr, Prowlarr, Bazarr, qBittorrent, autobrr and Cleanuparr in one app), [PaperCtrl](https://github.com/VoidedKN0X/PaperCtrl) (scan and browse documents on Paperless-ngx), [ProxCtrl](https://github.com/VoidedKN0X/ProxCtrl) (manage your Proxmox VE server) and [TrueCtrl](https://github.com/VoidedKN0X/TrueCtrl) (monitor and manage your TrueNAS server). Every app in the table is also in the [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) for automatic updates. No change to the app itself.

## v0.2.2 (2026-10-19)

**BeamCtrl now has the internet permission, used only for optional anonymous usage stats.** Until now BeamCtrl had no internet access at all. It still works fully offline: your remotes, the code library, Turn off any TV, imports and backups never go online. The new permission is used for one thing only, the usage stats below, and they're **off unless you say yes**. If you say no (or never answer), BeamCtrl sends nothing.

### New

- **Optional anonymous usage stats.** When you first open this version, BeamCtrl asks once whether you'd like to share anonymous usage stats, so I can see how many people use the app and which features they use. Nothing is sent unless you tap **Share stats**, and you can switch it on or off any time in **Settings > Usage stats**
- **What's shared:** which screens you open (for example "Remotes", "Library" or "Settings", never which remote), the kind of device a remote is for (TV, air conditioner, projector and so on) the first time you press one of its buttons on each visit, that you searched or browsed the code library and which device type you picked, that you saved a remote from the library, that you started Turn off any TV, whether your phone has an IR blaster, the app version, Android version, phone model, language, and phone or tablet
- **What's never shared:** your remotes' names, brands, models or IR codes, what you search for, files you import or back up, or any ID that follows you between sessions. Each session gets a new random ID, and stats from before and after you switch them off and on again aren't linked
- **Where it goes:** to VKNXWorks' own self-hosted server (Aptabase), not to Google or any analytics company. Builds you make from source can't send stats at all, and the setting doesn't appear in them

### Changed

- **Internet permission added, for the usage stats only.** Earlier versions removed the internet permission entirely. This version asks for it so the optional stats can be sent; the network-state permission is still removed, and the IR blaster permission is unchanged
- **PrintCtrl in the Ctrl series list.** The README's table of Ctrl apps now includes [PrintCtrl](https://github.com/VoidedKN0X/PrintCtrl), the app for Bambu Lab printers through a self-hosted Bambuddy server, and Spoolman spools.
- The README has a new **Usage stats** section listing exactly what's collected, and explains that the app still works fully offline

## v0.2.1 (2026-10-18)

A security check of the new code library and Turn off any TV. Everything works as before, with one change to when Turn off any TV stops.

### Security

- **Turn off any TV now stops when you leave the app.** Before, going to the home screen, switching to another app or locking the phone while it was running kept the IR blaster sending power codes in the background for up to about a minute and a half, with no way to tap Stop. It now stops straight away in all of those cases, just as it already did when you went back from the screen. When you come back, tap Continue to carry on from where it stopped.
- **A damaged entry in the code library can't close the app.** If a remote's entry in the built-in library is ever unreadable, it's left out of the list instead of stopping the whole library from opening, and the library screens show nothing rather than closing the app if the library can't be read.

### Checked, no change needed

- BeamCtrl still has no internet access: the released app asks only for the IR blaster permission, and the code library is read from inside the app.
- Remotes from the library, files you import and backups you restore all go through the same checks (size, number of buttons, carrier frequency, signal length and names).
- Turn off any TV can only be started from its own screen, never from outside the app, and always ends after the 368 codes in the list.

## v0.2.0 (2026-10-18)

A built-in code library with thousands of remotes, and a way to turn off any TV.

### New

- **Code library with 8,581 remotes.** Open **Add → Search all remotes** to find a remote by brand or model, or browse by device type: TVs, soundbars, AV receivers, projectors, streaming boxes, cable boxes, air conditioners, fans, lights and about 40 more. The remotes come from the [Flipper-IRDB](https://github.com/Lucaslhm/Flipper-IRDB) community database.
- **Try before you save.** Each remote in the library opens as the finished remote, with the same layout you'll get after saving, and every button already works. Point your phone at the device, try a few buttons, then give it a name and save it.
- **Picking a brand with only one remote opens it straight away**, and brands with several list every model with how many buttons it has. A small power icon marks remotes that have a power button.
- **Turn off any TV.** Open **Add → Turn off any TV** and tap Start. BeamCtrl sends the power code of every TV in the library, 368 in all, with the most common TVs first. A full run takes about a minute; tap Stop as soon as the TV switches off.
- **Find the code that worked.** After stopping, the last eight codes sent are listed with a Test button each, so you can find which one switched your TV off. The search button next to each opens that brand's remotes in the library.
- The screen stays on while codes are being sent, and sending stops when you leave the screen.

### Changed

- **Still fully offline.** The library is built into the app (about 5 MB), so BeamCtrl still has no internet access. New remotes from the database arrive with app updates.
- Remotes from the library go through the same checks as imported Flipper files. Buttons that use a code format BeamCtrl can't send yet (for example Panasonic's Kaseikyo) are left out, and the remote says how many.
- **Vizio** remotes now have the d-pad, OK, back, home, menu, exit, info and playback buttons as well as power, volume, channels, input and numbers.

### Fixed

- **Philips:** channel up and down sent the wrong codes. They now use the standard Philips channel codes.

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
