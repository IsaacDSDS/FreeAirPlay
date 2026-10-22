# FreeAirPlay

**AirCast TV**, an AirPlay receiver for Android TV: mirror an iPhone, iPad or Mac screen (with
sound), send videos with the AirPlay button in Safari and video apps, and stream AirPlay audio to
the TV, the way an Apple TV does. Built and tested on a TCL Android TV
(Android 11, Realtek RTD2851A, `armeabi-v7a`).

## How it works

```
iPhone / iPad / Mac
   │  mDNS (_airplay._tcp, _raop._tcp)  ──►  internal mDNS responder (UDP 5353)
   │  RTSP/HTTP control: /info, pair-setup/verify, fp-setup (FairPlay), SETUP, RECORD …
   │  mirror stream (TCP, AES-CTR)  /  audio (RTP, AES-CBC)
   │  AirPlay video (HTTP): /play <url>, /rate, /scrub, /playback-info, /stop
   ▼
libaircast.so ── UxPlay 1.74 protocol core (C) + OpenSSL libcrypto + libplist + Apple ALAC decoder
   │  JNI: decrypted H.264/H.265 access units, AAC/PCM audio, metadata, session events,
   │       AirPlay video URL + remote-control commands
   ▼
Kotlin receiver ── MediaCodec (hardware video → SurfaceView), MediaCodec AAC-ELD/LC + AudioTrack,
                   Media3 ExoPlayer for AirPlay video URLs (progressive and HLS)
```

The AirPlay protocol (pairing, FairPlay key exchange, RTP) comes from
[UxPlay](https://github.com/FDH2/UxPlay), vendored unmodified in `app/src/main/cpp/uxplay`
except for one patch in `mdnsd/dnssd_mdnsd.c` (Android's `gethostname()` returns `localhost`,
so the app sets a unique mDNS host label). `android/aircast_jni.c` replaces `uxplay.cpp`:
same startup order, advertised feature bits and reset/reconnect loop, with GStreamer swapped for
JNI callbacks.

Kotlin code follows feature-first Clean Architecture:

```
core/                       DI (ServiceLocator), network address, Surface adapter
features/receiver/domain/   ReceiverState, NowPlaying, AirPlayReceiverRepository (pure Kotlin)
features/receiver/data/     JNI bridge, VideoDecoder, AudioPlayer, StreamPlayer, DMAP parser, identity store
features/receiver/presentation/  ReceiverActivity, ReceiverService
shared/ui/                  AspectRatioFrameLayout
```

UI tokens and components are documented in [STYLES.md](STYLES.md).

### Video modes

The receiver advertises a 3840×2160 display at up to 60 fps with H.265 support (AirPlay
feature bit 42). Clients that can encode HEVC (recent iPhones/iPads, Apple-silicon Macs) then
mirror in 4K H.265; others send H.264 at up to 1080p. Both match the TV's hardware decoders
(Realtek: HEVC up to 4096×2176 @ 60 fps, AVC up to 1920×1088). The fps value is only a ceiling —
the client lowers it when the content is static.

### AirPlay video (the AirPlay button in Safari and video apps)

Feature bits 0 and 4 are advertised (UxPlay's `-hls` mode), so instead of sending only audio,
apps hand the TV the video itself:

- **Safari and apps that use direct links** send the media URL (`.mp4`, `.m3u8`…), which the TV
  downloads and plays itself with ExoPlayer. HLS links without a `.m3u8` suffix are retried as HLS.
- **The YouTube app** sends HLS playlists over the AirPlay connection; the UxPlay core serves them
  at `http://localhost:<port>/master.m3u8` and the segments come straight from YouTube. The audio
  track follows the TV's language list.

The client keeps control: pause/play (`/rate`), seek (`/scrub`) and its progress bar
(`/playback-info`, answered from a snapshot so HTTP threads never wait for the player). When the
video ends or fails, the receiver reports it as finished and the session closes. Mirroring and
AirPlay video share the screen surface but never at the same time.

The TV remote controls the video too, and the phone follows on its next `/playback-info` poll:

| Key | Action |
|---|---|
| OK / Play-Pause | pause or resume |
| ← / → | back / forward 10 s |
| Rewind / Fast-forward | back / forward 30 s |
| ↑ / ↓ / Info | show the controls |
| Back / Stop | close the player at once and drop the client's AirPlay video connections, so the phone ends its session too (like Menu on an Apple TV) |

Mirroring can't be controlled from the TV: the picture is the phone's screen.

## Build

Requirements: Android SDK (platform 35, NDK 27.1.12297006, CMake 3.22.1), JDK 17+.

```sh
# one-off: OpenSSL libcrypto for both ABIs (already done; output in app/src/main/cpp/third_party/openssl)
scripts/build_openssl.sh <work-dir> "$PWD/app/src/main/cpp/third_party/openssl" 3.5.4

./gradlew assembleRelease     # signed with the local debug key, for sideloading
```

## Install on the TV

Enable *Developer options → USB/Network debugging* on the TV, then:

```sh
adb connect <tv-ip>:5555
adb install -r app/build/outputs/apk/release/app-release.apk
# let the receiver bring the mirroring screen to the front from the background
adb shell appops set com.isaacbenavides.aircast SYSTEM_ALERT_WINDOW allow
```

The receiver starts when **AirCast TV** is opened from the launcher; nothing starts it on boot or
after an update. Once opened it stays discoverable in the background (until the TV restarts or
the system stops the app) and comes to the front by itself (waking the screensaver) when a
device starts mirroring or sends a video.

## TCL firmware notes

TCL's `tcl-service.jar` (`TclAppBootManagerImpl.canStartForegroundGo`) blocks foreground
services of third-party apps unless the app has a visible activity, is a TCL/system app, or is on a
cloud-delivered whitelist. The app therefore:

- starts its service from `ReceiverActivity` and promotes it to foreground every time the
  activity resumes.

Check with `adb shell dumpsys activity services com.isaacbenavides.aircast | grep isForeground`.

## Limitations

- Content protected with FairPlay Streaming DRM (Apple TV+, Netflix…) stays black when mirrored —
  true of every non-Apple receiver.
- AirPlay video only works with unprotected streams. Apps that use FairPlay Streaming (Apple TV+,
  Netflix, Disney+, Max, Prime Video…) can't play on any non-Apple receiver; links that need the
  phone's cookies or are tied to its IP address fail too. Screen Mirroring still works for
  unprotected content.
- Audio and video are rendered as soon as they are decoded (no clock sync), which keeps latency
  low for mirroring.
- Anyone on the same network can connect (no PIN), like UxPlay's default.

## License

GPL-3.0, inherited from UxPlay (`app/src/main/cpp/uxplay/LICENSE`). Third-party code: libplist
(LGPL-2.1), Apple ALAC decoder (Apache-2.0), OpenSSL (Apache-2.0), llhttp (MIT),
AndroidX Media3 (Apache-2.0).
