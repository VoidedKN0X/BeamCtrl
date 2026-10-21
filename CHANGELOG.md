# Changelog

What changed in each BeamCtrl release. Every release on the [Releases page](https://github.com/VoidedKN0X/BeamCtrl/releases) also lists the exact changes in that build.

To publish a new version: bump `versionCode` and `versionName` in `app/build.gradle.kts`, add a section here, and push to `main`. Pushes without a version bump only build; they don't make a release.

## v0.4.3 (2026-10-21)

### Fixed

- **Fingerprint unlock works again after you leave the app**: once you'd left BeamCtrl with the back gesture or by swiping home, the lock screen no longer asked for your fingerprint when you came back, and tapping **Use fingerprint** did nothing either; only the PIN worked until the app was force-closed. The fingerprint prompt was asked for while BeamCtrl was still coming back on screen, and the phone dropped it without answering, which left BeamCtrl waiting for an answer that never came. Now the prompt only opens once BeamCtrl is fully back on screen, a prompt that never answered can't block the next one, and the lock screen offers your fingerprint again every time you come back to it, not just the first time after it locked.

## v0.4.2 (2026-10-21)

### Privacy

- **No hidden dependency list in the APK**: Android's build tools used to tuck an encrypted list of the app's libraries, readable only by Google, into the APK's signature block. It's no longer added, so the APK holds nothing that only Google can read and passes F-Droid's check for it. Nothing changes in the app itself.

## v0.4.1 (2026-10-21)

### Changed

- **Use wallpaper colours** is the new name of the Dynamic colour switch in Settings › Appearance, with "Replaces the default colours with ones from your wallpaper" underneath, the same wording as in every Ctrl app
- All text is now in one place, ready for translations: every screen, button, message and error (about 250 texts), including key names such as "Volume up", device types, the code library's categories and the code editor's checks. Nothing changes in English
- Screen readers now call the profile picture "You", like in the other Ctrl apps (before: "You and settings")

### Fixed

- **Counts read correctly for one item**: "1 button", "1 remote" and "1 code ready" instead of "1 buttons", "1 remotes" and "1 codes ready" (in the code library, the remote cards, importing a Flipper file and Turn off any TV)
- A failed fingerprint prompt could show a technical error message instead of "Couldn't unlock with your fingerprint"

## v0.4.0 (2026-10-21)

Settings now open from your profile picture, like in every Ctrl app, and backing up to a file is now the way to move BeamCtrl to another phone.

### New

- **Your profile picture at the top right** of the Remotes and Add tabs opens the new **You** page. BeamCtrl has no accounts, so it shows the same round placeholder picture as the other Ctrl apps. The You page has a card for this phone (its name, how many remotes you have and whether the IR blaster is ready), then **Backup**, **Settings** and **About** with the version underneath
- **Back up to a file** (tap your profile picture › Backup): saves **everything** in one file, using Android's file picker so you choose the name and the folder (Downloads, an SD card, a USB stick or any cloud storage app you have installed). The file holds every remote with all its buttons and codes, and every setting: theme, dynamic colour, vibrate on press, hold to repeat, number pad, keep screen on, lock again, hide in screenshots and hide in recent apps. It never holds the app lock's PIN or fingerprint keys (they belong to this phone's secure hardware), or whether the app lock is on. Works even before you've added a remote
- **Restore from a file** (same place): pick a backup file, check what's in it, then confirm. The confirmation says when the backup was made, how many remotes it has and how many are on the phone now, and that restoring **replaces** every remote and setting on this phone. The app lock and its PIN stay exactly as they are on this phone. Works on another phone, so this is how you move your remotes to a new phone: back up on the old one, copy the file over, restore on the new one, then set up the app lock there if you want it
- **Restoring checks the whole file first and refuses anything malformed**, instead of skipping or repairing parts of it: a file that isn't a BeamCtrl backup, one that's damaged or was edited by hand (unknown or missing fields, wrong values, duplicate remotes or buttons, names that are too long, codes the IR blaster can't send, more than 500 remotes or 500 buttons on a remote), one bigger than 4 MB, or one made by a newer BeamCtrl. You see why in a message, and nothing on the phone changes. A restore either brings back exactly what was backed up or changes nothing

### Changed

- **Settings moved to your profile picture**: there's no **Settings** tab any more, so the bottom bar only has **Remotes** and **Add**. Tap your profile picture at the top right, then **Settings** for Appearance, Privacy and security (app lock, hide in screenshots, hide in recent apps), IR blaster and Buttons. **Backup** and **About** (Support BeamCtrl, Report a bug, Source code, Licence) are on the You page itself
- **"Export all remotes" is now "Back up to a file"** and also saves your settings. **"Restore from backup" is now "Restore from a file"** and replaces what's on the phone after asking first, instead of adding the file's remotes to the ones already there. Files exported by earlier versions still restore: they replace your remotes and leave your settings as they are
- **No more phone-to-phone transfer through Android.** BeamCtrl was already left out of cloud backups; now it's also left out of Android's own phone-to-phone transfer (for example when setting up a new phone from the old one with a cable or over Wi-Fi), so nothing from BeamCtrl leaves the phone unless you back it up to a file yourself. Use **Back up to a file** and **Restore from a file** to move your remotes
- **Forgot PIN?** on the lock screen now says you can restore everything from a backup file afterwards (tap your profile picture › Restore from a file)
- **README**: Getting started, the App lock section and Support now say "tap your profile picture › Settings"; new Backup and moving to a new phone section; Privacy explains that BeamCtrl isn't in Android's cloud backup or phone-to-phone transfer

## v0.3.1 (2026-10-21)

### Changed

- **Hide in screenshots** and **Hide in recent apps** are now two separate switches in Settings > Privacy and security, both off by default, the same in every Ctrl app. **Hide in screenshots** keeps BeamCtrl out of screenshots and screen recordings (and blanks its preview in recent apps); **Hide in recent apps** only shows a blank card in the recent apps view, so screenshots still work. If you turned on the single switch in 0.3.0, both new switches start on
- The lock screen and the PIN screens are always kept out of screenshots, and with the app lock on the recent apps view always shows a blank card, whatever the switches say
- **README**: the App lock section uses the same wording as the other Ctrl apps and describes the two switches

## v0.3.0 (2026-10-21)

The app lock: the same one as in every other Ctrl app, with its own PIN and optional fingerprint unlock, so nobody else can use or change your remotes.

### New

- **App lock** (**Settings > Privacy and security > App lock**, off until you turn it on). BeamCtrl asks for its own app PIN when you open it. The PIN needs at least 6 digits and is separate from your phone's; very obvious ones like 111111 or 123456 are refused. You choose it twice when turning the lock on
- **Unlock with fingerprint** (optional): only strong biometrics such as your fingerprint, not face unlock that only uses the camera, which a photo can sometimes fool. The PIN is still needed after the phone restarts and at least every 72 hours, the same as Android does for your screen lock
- **Lock again**: as soon as you leave the app (the default), after 1, 5 or 15 minutes or 1 hour away, or only when the app restarts. Exporting or restoring a backup file and opening a link in your browser don't count as leaving, unless you stay away longer than 5 minutes
- **Change app PIN** asks for your current PIN first, and turning the app lock off asks for the PIN too
- **Forgot PIN?** on the lock screen deletes every remote and setting on the phone and turns the lock off, because the PIN can't be recovered. If you exported your remotes to a file, you can restore them afterwards
- **Hide in screenshots and recent apps** (Settings > Privacy and security, off by default): keeps BeamCtrl out of screenshots, screen recordings and the recent apps view. The lock and PIN screens are always hidden, and with the app lock on the recent apps view always shows a blank card

### Changed

- **Settings are in the same order as in the other Ctrl apps**: Appearance first, then Privacy and security, then IR blaster, Buttons, Backup and About
- **"Wallpaper colours" is now "Dynamic colour"** ("Use colours from your wallpaper instead of ember"), like in the other Ctrl apps
- **No cloud backup any more.** Your remotes and settings are no longer put in Android's cloud backup, so nothing from BeamCtrl goes to a backup service. They still move to a new phone in a direct phone-to-phone transfer, and **Export all remotes** still saves them to a file you keep. The app lock isn't transferred: its keys belong to this phone's secure hardware, so you set it up again on the new phone
- **README**: new App lock section, backups explained under Privacy, and AdCtrl and MemoCtrl added to the Ctrl series table

### Security

- **The PIN can't be guessed off the phone.** It's never stored. It's stretched with 600,000 rounds of PBKDF2 and mixed with a secret held in the phone's secure hardware (StrongBox where available), so copying BeamCtrl's files to a computer isn't enough to try PINs there
- **Wrong PINs lock the screen**: after 5 tries you wait 30 seconds, doubling with every further 5 wrong tries up to 15 minutes. Restarting the app or the phone, or changing the clock, doesn't reset the wait
- **Fingerprint unlock can't be faked**: the fingerprint has to unlock a key in the phone's secure hardware, so a tampered "success" on a rooted phone doesn't open the app. Adding a new fingerprint turns fingerprint unlock off until you turn it on again with your PIN

## v0.2.9 (2026-10-20)

### Changed

- **64-bit ARM phones only.** BeamCtrl now ships only the 64-bit ARM (arm64-v8a) build that nearly every phone from the last several years uses, like every Ctrl app, so the download is smaller. Old 32-bit phones and x86 devices (emulators, some Chromebooks) are no longer supported.
- **README**: Requirements say 64-bit ARM phones only

## v0.2.8 (2026-10-20)

BeamCtrl is now Google-free and de-Googled. Nothing changes in how you use it.

### Privacy

- **No mention of Google left in the app's own code.** BeamCtrl has no Google Play services, Firebase or other Google services and never contacts Google (it has no internet permission at all). The last mention of Google inside the app, a technical note in its setup file, is gone. It works the same on de-Googled phones such as GrapheneOS, CalyxOS, /e/OS and LineageOS without Google apps.

### Changed

- **README**: the Google-free line now reads "Google-free and de-Googled": no Google Play services, Firebase or other Google services, the app never contacts Google, and it works the same on de-Googled phones (GrapheneOS, CalyxOS, /e/OS, LineageOS without Google apps). It's the only place Google is mentioned in the README.

## v0.2.7 (2026-10-20)

### Privacy

- **No more usage stats: BeamCtrl is now telemetry-free.** The optional anonymous usage stats are gone completely, along with the Usage stats setting and the one-time question. The app no longer contacts VKNXWorks' stats server at all, and the settings it used are deleted from your phone when you update
- **No internet permission any more.** The stats were the only thing that used it, so BeamCtrl no longer asks for it and can never connect to the internet. Remotes, the code library, Turn off any TV, imports and backups always worked offline and still do

### Changed

- **README**: a Screenshots section (screenshots are coming soon), and a link preview image (`.github/assets/social-preview.png`) with BeamCtrl's icon, name and summary, for when the repo is shared on Reddit, Discord or elsewhere. It also gets a "Telemetry-free" badge and tag, and a Privacy section in place of the old Usage stats section, and the permissions list now says the IR blaster only

## v0.2.6 (2026-10-19)

### Changed

- **One About section in every Ctrl app**: Support, Report a bug, Source code and Licence (GPL-3.0), with the version underneath, at the bottom of Settings. The rows are in the same order, with the same wording and icons, as in the other Ctrl apps. The new **Report a bug** row ("Found a problem or have an idea? Open an issue on GitHub") opens BeamCtrl's issue tracker, and the old "Source code and issues" row is now simply **Source code**

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
