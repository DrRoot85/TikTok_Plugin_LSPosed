# TikTok Plugin

A clean **LSPosed (Xposed) module** that adds local video/photo downloading to the **official TikTok app** (`com.zhiliaoapp.musically`).

Rebuilt from scratch against the modern [libxposed API 102](https://github.com/libxposed/api) — based on the feature set of the classic "TikTok Mod Cloud" plugin, but **without** any of its baggage.

## Features

- 💾 **Save videos to local storage** — tap the floating button while watching any video
- 🖼️ **Photo-mode posts** — auto-downloads all images in a post
- 🚫 **No-watermark downloads** — resolves TikTok's clean CDN copy (`downloadNoWatermarkAddr` / `newDownloadAddr`, with `playwm → play` fallback) using the URL TikTok itself provided. No third-party services involved.
- 📦 **Download entire feed** — long-press the floating button to save every post in the current feed
- 🎯 **Draggable floating button** — tap = download current, long-press = download all, drag = reposition
- ⚙️ **Settings app** — change download folder, filename prefix, toggles (via `libxposed:service`)
- 🔔 **Download notifications** — with tap-to-open on Android 10+
- 🔄 **Hot reload** — settings apply instantly without restarting TikTok

## What is *not* in here

The original plugin (`2.44_plugin.apk`) contained code that this module deliberately **does not** ship:

| Removed | Reason |
|---|---|
| "Download Via MAX" Telegram bot routing | Redirects your downloads to a Telegram bot |
| "Telegram Channel" menu entries | Ad / tracking redirects |
| Forced "install latest version from our Telegram channel" dialog | Update nagging / C&C surface |
| "Upgrade to Private" paid upsell | Monetization nag |
| Custom `DownloadViaTikTokSSL` trust manager | MITM risk — downloads use the platform HTTP client with system trust |

Downloads go **straight from TikTok's own CDN to your phone**. No Telegram, no remote config, no analytics, no network calls other than the media URL TikTok gave you.

## Requirements

- **LSPosed** 1.9+ (or another libxposed-API-102-compatible framework) with scope enabled for TikTok
- Official TikTok app installed (`com.zhiliaoapp.musically`)
- Android 8.0+ (API 26+)

## Install

1. Build or grab the APK (see below)
2. Install it, enable the module in LSPosed, select **TikTok** in scope
3. Force-stop TikTok and reopen it
4. A floating **⬓** button appears — tap to save the current video

## Build

```bash
./gradlew assembleDebug
# output: apk/TikTok-Plugin-v1.0.0.apk (copy of app/build/outputs/apk/debug/app-debug.apk)
```

Requires JDK 17+, Android SDK (platform 36, build-tools 36.0.0).

> **Note on `app/libs/`:** the `libxposed-service` / `libxposed-interface` AARs
> are shipped locally with the `aar-metadata.properties` (minCompileSdk=37)
> stripped, because API 37 is not published in the Android SDK repository.
> The metadata only gates a build-time check; runtime behavior is unaffected.
> If API 37 becomes available, delete `app/libs/*.aar` and switch the
> dependencies back to `io.github.libxposed:service:102.0.0`.

## How it works

The module hooks a small set of TikTok model classes (verified against the public modded-TikTok dex, same codebase family as official releases):

- `Aweme.getVideo()` / `Video.*Addr()` getters → track the post currently on screen
- `FeedItemList.getAwemeList()` → cache the feed for bulk downloads
- `Activity.onPostResume()` → attach the floating button

Settings are stored in LSPosed remote preferences, shared between the module app and the hooked TikTok process.

## License

Apache-2.0
