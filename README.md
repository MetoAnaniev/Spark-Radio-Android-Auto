# RadioSpark 📻

A native **Android (Kotlin)** online radio streaming app with **full Android Auto support**.
Built with Jetpack (Compose, Navigation, ViewModel, Room), **Media3 / ExoPlayer**, and a
clean MVVM + repository architecture. Designed with a **Material 3 / Samsung One UI–inspired**
look: rounded cards, large readable type, high-contrast dark theme by default.

---

## ✨ Features

| Area | What you get |
|------|--------------|
| **Now Playing** | Big artwork, bold station name, genre, description, large play/pause, next/previous, favorite toggle, buffering indicator + connection status (`Connecting… / Playing / Paused / Offline`) |
| **Stations** | Scrollable card list of all stations (built-in + user + remote) with search |
| **Explore** | Genre grid → filtered station list, with **Play first** quick action |
| **Favorites** | Heart any station from any list or Now Playing; persisted and ordered by last played |
| **Add / Manage** | Form to add custom stations (name, stream URL, logo, genre, country) with URL validation; edit & delete |
| **Settings** | Dark/light theme, streaming quality (Low/Medium/High), auto-start last station on Android Auto, Wi-Fi-only streaming |
| **Android Auto** | `MediaLibraryService` browse tree (Favorites / All / My Stations / Genres), metadata, car playback controls |
| **Persistence** | Room database (user stations + favorites) and SharedPreferences (settings) |
| **API-ready** | `RemoteStationApi` interface with a `Mock` (offline) and a real `Http` implementation |

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
  remote/       DefaultStations (in-code catalog) + RemoteStationApi (Mock | Http)
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

## 🚗 Android Auto

Implemented in `playback/RadioMediaService.kt`:

* Extends `MediaLibraryService` (Media3) — the modern replacement for `MediaBrowserService`.
* Exposes a browse tree:

  ```
  ROOT
   ├── Favorites
   ├── All Stations
   ├── My Stations
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
# Debug build (fast)
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

## 🔌 Connect a real REST API later

The only file you touch is the DI container.

```kotlin
// di/AppContainer.kt
remoteSource = RemoteStationSource(
    RemoteStationApi.Http("https://api.your-service.com")  // ← replace Mock()
)
```

Expected endpoints (contract in `data/remote/RemoteStationApi.kt`):

```
GET  /stations              → [ { id, name, streamUrl, genre, description, logoUrl, country } ]
GET  /stations?genre=jazz   → filtered list
POST /stations              → create a station (JSON body)
```

Swap OkHttp for Retrofit/Ktor inside `RemoteStationApi.Http` if you prefer — the rest of the
app depends only on the `RemoteStationApi` **interface**, so nothing else changes.

You can also implement a **custom source** (e.g. a podcast directory) by implementing
`StationSource` and adding it to `StationRepositoryImpl`.

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
