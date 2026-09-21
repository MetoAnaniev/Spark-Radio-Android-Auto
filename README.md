# RadioSpark 📻

A native **Android (Kotlin)** online radio streaming app for phones, **Google TV**, and **Android Auto**.
Built with Jetpack (Compose, Navigation, ViewModel, Room), **Media3 / ExoPlayer**, and a
clean MVVM + repository architecture. Designed with a **Material 3 / Samsung One UI–inspired**
look: rounded cards, large readable type, high-contrast dark theme by default.

---

## ✨ Features

| Area | What you get |
|------|--------------|
| **Now Playing** | Big artwork, bold station name, genre, description, large play/pause, next/previous, favorite toggle, buffering indicator + connection status (`Connecting… / Playing / Paused / Offline`) |
| **Stations** | Live Bulgarian + international directory, grouped by country with station/genre/country search |
| **Explore** | Genre grid → filtered station list, with **Play first** quick action |
| **Favorites** | Heart any station from any list or Now Playing; persisted and ordered by last played |
| **Add / Manage** | Form to add custom stations (name, stream URL, logo, genre, country) with URL validation; edit & delete |
| **Settings** | Dark/light theme, streaming quality (Low/Medium/High), auto-start last station on Android Auto, Wi-Fi-only streaming |
| **Google TV** | Leanback launcher, TV banner, landscape layout, persistent navigation rail, D-pad focus states, and remote-friendly controls |
| **Android Auto** | `MediaLibraryService` browse tree (Favorites / All / My Stations / Countries / Genres), metadata, car playback controls |
| **Persistence** | Room database (user stations + favorites) and SharedPreferences (settings) |
| **Live directory** | Free, keyless Radio Browser API with mirror failover, 15-minute cache, and verified Radio Nova fallback |

---

## 🏗️ Architecture

Clean-ish layering so a real backend can be added without touching the UI:

```
domain/         ← pure Kotlin: models, repository INTERFACES, use-cases
  model/        Station, Genre, PlaybackState, UserSettings
  repository/   StationRepository, PlaybackRepository, SettingsRepository
  usecase/      GetStationsByGenre, ValidateStreamUrl, ToggleFavorite
data/           ← implementations
  local/        Room: RadioDatabase, DAOs, entities (UserStation, Favorite)
  remote/       Verified fallback catalog + Radio Browser public API client
  repository/   StaticStationSource + UserStationSource + RemoteStationSource + StationRepositoryImpl
  playback/     MediaControllerPlaybackRepository (UI ⇄ MediaSession bridge)
  settings/     SettingsRepositoryImpl (SharedPreferences)
playback/       RadioMediaService — MediaLibraryService (Android Auto + MediaSession + ExoPlayer)
ui/             Compose screens + ViewModel + theme
di/             AppContainer (hand-rolled DI)
```

**Data flow:** `Screen (Compose) → RadioViewModel → StationRepository → sources (static/user/remote) → Room`.
Playback flows through a single **MediaSession** owned by `RadioMediaService`, so the phone UI
and Android Auto stay perfectly in sync (same queue, same position).

---

## Google TV

RadioSpark is available from the Google TV launcher through `LEANBACK_LAUNCHER`. It does not require a touchscreen and automatically switches to a 10-foot interface with a navigation rail, four-column genre browsing, spacious station lists, visible focused-card states, and a landscape two-pane Now Playing screen. The same APK remains compatible with Android phones.

Remote navigation uses the D-pad: move focus with the directional keys and press the center/select button to activate stations and controls. Hardware media play/pause controls are handled by the shared MediaSession.

## 🚗 Android Auto

Implemented in `playback/RadioMediaService.kt`:

* Extends `MediaLibraryService` (Media3) — the modern replacement for `MediaBrowserService`.
* Exposes a browse tree:

  ```
  ROOT
   ├── Favorites
   ├── All Stations
   ├── My Stations
   ├── Countries
   │     ├── Bulgaria … International
   └── Genres
         ├── Pop … Kids   (each lists its stations)
  ```
* Metadata: station title, genre (artist), country (album), artwork URI.
* Car playback: play / pause / next / previous map to the same ExoPlayer queue as the phone.
* `res/xml/automotive_app_desc.xml` declares `<uses name="media"/>`.
* Manifest registers the service + `com.google.android.gms.car.application` metadata.

### Testing in the Android Auto emulator
1. Install the app: `adb install -r app/build/outputs/apk/release/app-release.apk`
2. Open the **Android Auto** app on the phone → *Settings → Developer settings* →
   enable **Unknown sources** (needed for sideloaded builds), and
   enable **Start head unit server** if you drive a desktop emulator.
3. In Android Studio: **Tools → SDK Manager → SDK Tools →** install *Android Auto Desktop
   Head Unit Emulator* (or use the *Automotive with Play Store* ×86 image via `avdmanager`).
4. Launch the DHU: `~/Android/Sdk/extras/google/auto/desktop-head-unit`
5. In the car UI open the media app launcher → **RadioSpark** → browse & play.

> Requirements: Android Auto requires the phone app to expose the media service and the
> `media` automotive descriptor — both are already configured here.

---

## 🚀 Build

```bash
# Debug build for phone or Google TV (fast)
./gradlew :app:assembleDebug

# Signed release APK
./gradlew :app:assembleRelease
# → app/build/outputs/apk/release/app-release.apk

# Run unit tests
./gradlew :app:testDebugUnitTest
```

Requirements: **JDK 17+**, Android SDK **API 35**, Gradle **8.9** (via wrapper), Kotlin **2.0.21**,
AGP **8.7.2**. `minSdk = 24`, `targetSdk = 35`.

Release signing reads `keystore.properties` at the project root:
```properties
storeFile=release-key.jks
storePassword=…
keyAlias=radiospark
keyPassword=…
```
(If the file is absent the release build is simply left unsigned — handy for CI.)

---

## ➕ How to add stations

### A. Built-in station (in code)
Edit `data/remote/DefaultStations.kt` and append:

```kotlin
Station(
    id = "static_my_station",                       // must be unique, prefix "static_"
    name = "My New Station",
    streamUrl = "https://stream.myserver.com/live", // http/https, .m3u/.m3u8/.pls OK
    genre = Genre.POP,                              // see Genre enum
    description = "One-line description.",
    country = "Bulgaria",
    logoUrl = "https://cdn.myserver.com/logo.png",  // optional
),
```
That's it — it appears in Stations, Explore, Favorites and the Android Auto browse tree.

### B. User station (runtime, persisted)
Use the in-app **Add station** screen (FAB on Stations). Stored in the Room `user_stations`
table and labelled *My Stations* in Android Auto.

### C. Change the seed genres
Add/remove values in `domain/model/Genre.kt`. Give each a label + Material icon and it shows
up automatically in the Explore grid and car browse tree.

---

## 🔌 Live station directory

The app uses the free, open-source [Radio Browser API](https://api.radio-browser.info/) without an API key. It loads up to 100 Bulgarian stations and 200 popular international stations, removes duplicates, and sorts Bulgaria first followed by countries and station names alphabetically. Two API mirrors are tried automatically and successful results are cached for 15 minutes.

`DefaultStations.kt` contains a verified Radio Nova high-quality AAC stream, so the app still has a working Bulgarian station when the directory is temporarily unavailable.

You can implement another source by implementing `RemoteStationApi` or `StationSource` and wiring it in `AppContainer`.

---

## 🎨 Theming
All colors live in `ui/theme/Color.kt` and are wired through `RadioSparkTheme`. The palette is
a dark, low-glare surface set with a single cyan accent (`AccentBlue = #4CC2FF`) that stays
legible day and night. Type scale is intentionally large (`displaySmall 34sp`, `titleMedium 18sp`)
for in-car glanceability.

---

## 📁 Project layout
```
app/src/main/java/com/sparklab/radio/
├── MainActivity.kt              ← single-Activity Compose host
├── RadioApp.kt                  ← Application + DI container
├── data/  domain/  playback/  ui/  di/
app/src/main/res/xml/automotive_app_desc.xml   ← declares the media app for Android Auto
```

## 📄 License
Provided as a starting point for SparkLab Academy projects — extend freely.
