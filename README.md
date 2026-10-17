# BeamCtrl

An infrared remote control app for phones with an IR blaster, part of the VKNXWorks Ctrl series (CraftyCtrl, FrigateCtrl, SeerrCtrl, JellyCtrl, FilesCtrl, NaviCtrl). Android 13+.

## Features

**Remotes tab**
- Every saved remote as a card, most recently used first
- Quick power key on each card, without opening the remote
- Long-press a card to edit, rename or delete it

**Remote view**
- A full TV-style layout: power, input, mute, d-pad with OK, back, home, menu, volume and channel rockers, info, guide, exit, playback keys and a number pad
- Sections only show when the remote has those buttons; anything else goes in "More buttons"
- Hold volume, channel and arrow keys to keep sending (NEC remotes send proper repeat frames)
- A light in the top bar glows each time a signal goes out
- Edit mode: tap any key to change its code, tap a dimmed spot to add one, or add free-form buttons
- Export any remote as a Flipper Zero `.ir` file

**Add tab**
- Ready-made TV remotes for Samsung, LG, Sony, Philips and Vizio, with test keys before saving
- Find my TV: steps through each brand's power code until the TV responds
- Import a Flipper Zero `.ir` file (for example from Flipper-IRDB); button names like Power, Vol_up and Ch_next land in the right places
- Blank remote, built button by button

**Codes**
- NEC and NEC extended, Samsung, Sony SIRC 12/15/20-bit, Philips RC5/RC5X and RC6, Pronto hex, and raw timings
- RC5/RC6 toggle bit flips on each new press
- Signals are sent from one background thread, so presses never overlap; carrier frequencies outside the emitter's range are moved to the closest supported one

**Settings**
- IR blaster status and supported carrier frequencies
- Vibrate on press, hold to repeat, number pad, keep screen on
- Theme (system, dark, light, pure black) and wallpaper colours
- Export and restore all remotes as a JSON backup (remotes are also in the phone's Android backup)
- Phone layout with the floating glass tab island; tablet and unfolded foldable layout with a side rail

Phones can only send infrared, not receive it, so codes can't be learned from an existing remote.

## Building

GitHub Actions builds a release APK on every push to `main` and publishes it on the Releases page.

Release signing uses the same setup as the other Ctrl apps:

| Name | Where |
| --- | --- |
| `KEYSTORE_BASE64` | Secret: the keystore, base64 encoded |
| `KEYSTORE_PASSWORD` | Secret |
| `KEY_PASSWORD` | Secret |
| `KEY_ALIAS` | Variable (or secret) |

Without them, CI fails instead of publishing a debug-signed release (local builds still fall back to the debug key).

Locally: `./gradlew assembleRelease` (JDK 17, Android SDK 36).
