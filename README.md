# CLICKAI

## Build & Run

1. Open this project in Android Studio (recommended).
2. Let Gradle sync and download dependencies.
3. Build and run on an Android device or emulator (minSdk 24, targetSdk 34).

## Project Structure
- `app/` — Main Android app module
- `build.gradle.kts`, `settings.gradle.kts` — Project build configuration (Kotlin DSL)
- `schema/macro.schema.json` — JSON schema for macros
- `com.primordia.clickai.*` — New accessibility-driven macro engine, detectors, and UI
- `com.clickai.macroapp.*` — Legacy prototype kept for reference

## Download APKs
- Debug builds: go to GitHub → Actions → select the latest run on main → download artifact named `app-debug-apk`. These artifacts are tied to the workflow run, not to Releases.
- Releases: push a tag like `v0.1.0` → CI creates a GitHub Release and attaches the signed APK automatically.

## Requirements
- Android 7.0+ (gestures supported reliably on 7.0/7.1+; tested on 8+)
- Enable the Accessibility Service in Settings for automation.

## Element & Pattern Recognition
- Image Templates: Multi-scale normalized cross-correlation matching with user-provided reference images. Threshold configurable.
- Color Detection: Presence within region using RGB tolerance.
- Text Recognition: ML Kit Text Recognition v2. Supports exact or regex queries.
- Spatial Patterns: Compose elements (image/text/color) and relate them via ABOVE/BELOW/LEFT_OF/RIGHT_OF. Pattern detector scaffolding included.
- Visual Pattern Editor: Basic activity stub to add elements and save; extend to support rectangle drawing on screenshots.

## Safety & Policy
- Use responsibly and comply with app Terms of Service.
- The service shows a persistent notification when running; no data leaves the device by default.

## Macro Logic & Input Injection
- Reliable Tap Injection: Uses AccessibilityService gestures; for privileged input (root/adb), integrate a privileged service as a fallback.
- Macro Logic: Steps include Click, Swipe, Wait, InputText, OcrWait (regex), ImageMatch (multi-scale), ColorWait, PatternWait, Loop, Conditional.

## Notes on Screenshot APIs
To enable element recognition:
- MediaProjection API to capture screen (with user consent), or
- AccessibilityService.takeScreenshot on API 33+ (Android 13) with appropriate flags.

Wire the screenshot provider into `OcrDetector`, `ImageMatchDetector`, `ColorDetector`, and `PatternDetector`.