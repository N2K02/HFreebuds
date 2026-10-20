# HFreebuds

[![Latest release](https://img.shields.io/github/v/release/N2K02/HFreebuds?label=download)](https://github.com/N2K02/HFreebuds/releases/latest) ![Android 12+](https://img.shields.io/badge/Android-12%2B-3DDC84?logo=android&logoColor=white) ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white) [![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Control your **HUAWEI FreeBuds** from any Android phone, without HUAWEI AI Life. Made for the **FreeBuds 5** on a Pixel, and works with other Huawei earbuds too.

**[⬇ Download the latest APK](https://github.com/N2K02/HFreebuds/releases/latest)** · Android 12 or newer · Material 3 Expressive (your wallpaper colours, light/dark)

## Layout

Laid out like the official app, in Material 3 Expressive: a home screen with **Sound** (noise control, sound effects), **Interactions** (gesture control with Double-tap / Triple-tap / Press & hold / Swipe tabs), **Ear fit** (wear detection) and **More** (Find Device, Earbud settings, Voice prompts, Phone audio, Updates, About, Connection log). Each opens its own screen.

## Features

| | |
| --- | --- |
| **Battery** | Left, right and case, with charging status; whether the earbuds are in your ears |
| **Noise control** | Off / Noise cancelling / Awareness (models that have it), with strength levels: Smart, Light, Balanced (and Deep where available) |
| **Sound** | Equalizer presets (Default, Bass boost, Treble boost, Voices), **HD audio** (sound quality first, LDAC) or connection first, **Low latency** |
| **Gestures** | Double-tap, triple-tap, press and hold (and which noise modes it cycles), swipe: each **left and right** separately, plus during calls |
| **Wear detection** | Pause music when you take an earbud out |
| **Dual-device connection** | On/off, see the connected phones/computers, set the preferred one, connect, disconnect, forget |
| **Voice prompts language** | Choose from the languages the earbuds offer |
| **Find my earbuds** | A loud chirp on the left, right or both earbuds, getting louder |
| **Phone audio** | Shows the Bluetooth codec in use (LDAC, AAC, SBC…), sample rate, bit depth and LDAC quality; shortcuts to the codec setting and the left/right balance setting, with left/right test tones |
| **About** | Model, firmware, hardware version, serial numbers (each earbud) |
| **Quick Settings tiles** | "Noise control": tap to cycle Off → Noise cancelling (→ Awareness) without opening the app. "Earbuds battery": the last left / right / case reading; tap to check again |
| **Reconnects by itself** | While the app is open, it reconnects as soon as your earbuds come back (e.g. out of the case) |
| **Connection log** | The packets sent and received, with a copy button, to troubleshoot a setting |
| **Updates** | Built-in: checks this repo's releases and installs new versions through Android's installer |

### Supported earbuds

Each model has a profile of what it supports. Every section also only appears when the earbuds actually answer, so unlisted Huawei / Honor earbuds get whatever they support.

- **HUAWEI FreeBuds 5**: battery, noise control (Smart / Light / Balanced), equalizer, HD audio, low latency and wear detection match packets captured from the official app. Gestures, dual connection and language use the protocol shared by the other models and are marked "not tested on this model yet" in the app.
- FreeBuds 5i, 6i, 4i, 3, Pro, Pro 2, Pro 3, Pro 4, Pro 5, SE, SE 2, SE 3, SE 4 ANC, FreeClip, FreeClip 2, FreeBuds Studio, FreeLace Pro, FreeLace Pro 2, Honor Earbuds 2 / 2 SE / 2 Lite, FreeBuds 4.

### What Android doesn't allow

- **Changing the Bluetooth codec** from an app: only the system can. HFreebuds shows the active codec and opens Developer options → Bluetooth audio codec / LDAC playback quality. Turn on HD audio in HFreebuds first so the earbuds offer LDAC.
- **Left/right balance** for all sound: set in Accessibility → Audio adjustment (HFreebuds opens it and has test tones).
- **Making the earbuds beep by themselves**: no Huawei command for it is known, so Find my earbuds plays the chirp through the phone; the earbuds need to be connected.

## Install

1. Open the latest **Release** on your phone and download `HFreebuds-<version>.apk`.
2. Install it (allow "install unknown apps" for your browser if asked).
3. Open HFreebuds, allow **Nearby devices**, tap **Choose earbuds** and pick your FreeBuds (pair them in Android's Bluetooth settings first).

## How it works

Huawei earbuds accept commands over a Bluetooth serial connection (RFCOMM). Each packet is `5A | length | 00 | command | parameters | CRC16`. HFreebuds builds and reads these packets itself; `ProtocolTest` checks them byte for byte against captures from the official app.

Protocol knowledge comes from these open-source projects, thank you:
- [HuaweiPods](https://github.com/Nshpiter/HuaweiPods): FreeBuds 5 packet captures
- [OpenFreebuds](https://github.com/melianmiko/OpenFreebuds): commands and per-model feature lists
- [FreeBuddy](https://github.com/TheLastGimbus/FreeBuddy)

HFreebuds' code is written from scratch in Kotlin; no code is copied from them.

## Updates

The app updates itself from this repo's Releases:

- When you open it (at most every 12 hours) it checks for a newer release. If there is one, a card shows **what's new** with **Update now**, **Later** or **Skip this version**.
- The **Updates** screen shows the version and **Check for updates**.
- The APK is downloaded inside the app, checked (size, SHA-256 checksum from GitHub, same app, newer version, same signing key) and opened in Android's installer.

## Signing

Releases are signed with a private key that is kept encrypted in `signing/keys.tar.enc` and unlocked during the build with the `SIGNING_PASSPHRASE` repository secret (see [signing/README.md](signing/README.md)).

## Build

Every push to `main` runs the protocol tests, builds the APK with GitHub Actions and publishes it as a release named "HFreebuds <version>" (1.0, 1.1, 1.2 …). Locally: `./gradlew assembleRelease` (JDK 17 + Android SDK; unsigned).

## License

MIT. See [LICENSE](LICENSE).


## Official download and project links

Android companion app for managing supported Huawei FreeBuds earbuds and their sound, noise control, gestures, and device settings.

- **Latest Android APK:** [Download the latest release](https://github.com/N2K02/HFreebuds/releases/latest)
- **All releases and version history:** [GitHub Releases](https://github.com/N2K02/HFreebuds/releases)
- **Source code and issue reporting:** [HFreebuds repository](https://github.com/N2K02/HFreebuds)

The APK is distributed through GitHub Releases. If no release is available, check the repository's Releases page for future published builds. Google indexing is not guaranteed; this section provides clear crawlable links for visitors and search engines.
