# Security policy

Thank you for helping keep BeamCtrl and its users safe.

## Reporting a problem

**Please don't report security problems in a public issue.** Report them privately instead:

1. Go to the [Security tab](https://github.com/VoidedKN0X/BeamCtrl/security) of this repo.
2. Click **Report a vulnerability** and describe what you found, how to reproduce it, and which version of BeamCtrl you used.

Only the maintainer can see the report. I'll reply as soon as I can, work on a fix, and credit you in the changelog if you'd like.

## Supported versions

Only the latest release gets security fixes. Please update before reporting, and check the [changelog](CHANGELOG.md) to see whether it's already fixed.

## What counts

BeamCtrl has no servers, accounts or passwords, and no internet permission. It only sends infrared codes and keeps your remotes on your phone. Security problems for BeamCtrl are things like:

- BeamCtrl being able to reach the internet or send your remotes or settings anywhere, without you choosing to save a backup or export file
- Ways around the app lock, or protected screens showing up in screenshots when they shouldn't
- Another app on the phone being able to make BeamCtrl do something you didn't ask for, such as sending IR codes or changing your remotes
- A Flipper `.ir` file or backup file that makes BeamCtrl crash, hang, write outside its own storage, or do anything other than import or refuse it
- Anything that lets someone run code or read files they shouldn't

Problems with IR codes that don't work on a TV aren't security problems; please [open a bug report](https://github.com/VoidedKN0X/BeamCtrl/issues/new/choose) instead.

## How releases are protected

Every release is built from this repo by GitHub Actions and signed with the same release key. Its SHA-256 certificate fingerprint is:

```
36:1C:14:5D:FB:0F:83:25:FB:9F:61:55:C7:0C:6A:84:CB:28:03:92:D6:9F:5D:45:FA:8A:42:C6:C2:FE:B0:54
```

The [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) only accepts APKs signed with this key.
