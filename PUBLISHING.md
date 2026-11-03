# RadioSpark — Release & Store Publishing

## Status (v1.6.3 / build 10)

| Platform | State |
|---|---|
| Android phone | ✅ Done (Compose, Media3, Room) |
| Android Auto | ✅ Done (MediaLibraryService browse tree, autostart, resume) |
| Google TV | ✅ Done (LEANBACK_LAUNCHER, banner, nav rail, D-pad focus, no touchscreen required) |
| iPhone / iPad / Mac | 🆕 Native SwiftUI port in `apple/` — needs first build on a Mac |
| Store metadata | 🆕 `store/` (listing text, graphics, privacy policy, review notes) |

---

## 1. Google Play (phone + TV + Auto, one AAB)

1. **Create upload key** (once, keep it safe — never commit):
   ```bash
   keytool -genkeypair -v -keystore release-key.jks -alias radiospark -keyalg RSA -keysize 4096 -validity 10000
   ```
   and fill `keystore.properties` (see README).
2. **Check target SDK:** Play requires new uploads to target the latest required API level. If Play Console rejects `targetSdk 35`, bump `compileSdk`/`targetSdk` to 36 in `app/build.gradle.kts` (needs AGP ≥ 8.9) and re-test.
3. **Build the bundle:** `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`. Bump `versionCode` for every upload.
4. **Play Console → Create app** "RadioSpark – Online Radio", category *Music & Audio*, free.
5. **Store listing:** text from `store/play/en-US/`, icon `store/play/graphics/icon_512.png`, feature graphic `feature_graphic_1024x500.png`. Screenshots still needed: ≥2 phone, plus TV screenshots (1920×1080) and Auto is reviewed from the build.
6. **App content:**
   - Privacy policy URL → host `store/privacy-policy.md` (e.g. `https://sparklab.bg/radiospark/privacy`, the link used in the apps).
   - Data safety: *No data collected, no data shared*. Ads: *No*. Target audience: 13+. Content rating questionnaire: no user-generated content shared with others.
   - Foreground service declaration: `mediaPlayback` — "continuous radio playback started by the user".
7. **Advanced settings → Form factors:** opt in to **Android TV** (upload TV banner `tv_banner_1280x720.png` + TV screenshots) and **Android Auto** (accept the Auto terms). Each gets its own review.
8. Release to **Internal testing** first, then Production.

## 2. Apple App Store (iOS + iPadOS + macOS, one universal purchase)

On a Mac with Xcode 15+:
```bash
brew install xcodegen
cd apple && xcodegen generate && open RadioSpark.xcodeproj
```
1. Set `DEVELOPMENT_TEAM` in `apple/project.yml` (or in Xcode → Signing) and regenerate.
2. Run on iPhone simulator and "My Mac"; verify playback, background audio, lock screen / Control Center, media keys on Mac.
3. **App Store Connect → New App**, bundle ID `com.sparklab.radio`, platforms iOS + macOS, primary category *Music*.
4. Metadata from `store/apple/en-US/` (name, subtitle, keywords, description, review notes). Privacy policy URL as above.
5. **App Privacy:** *Data Not Collected*. Privacy manifest is in `Resources/PrivacyInfo.xcprivacy`.
6. Screenshots: 6.9" iPhone, 13" iPad, Mac (2880×1800).
7. Archive per platform (Product → Archive, select iOS device, then My Mac) → Distribute → App Store Connect → submit for review.

Notes: App Review sometimes asks about `NSAllowsArbitraryLoads` — the justification is in `review_notes.txt`. CarPlay audio support needs a CarPlay entitlement request from Apple (developer.apple.com/contact/carplay) before it can be added.
