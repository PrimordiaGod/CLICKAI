# CLICKAI

## Build & Run

1. Open this project in Android Studio (recommended).
2. Let Gradle sync and download dependencies.
3. Build and run on an Android device or emulator (minSdk 24, targetSdk 34).

## Project Structure
- `app/` — Main Android app module
- `build.gradle.kts`, `settings.gradle.kts` — Project build configuration (Kotlin DSL)
- `schema/macro.schema.json` — JSON schema for macros

## Download APKs
- Debug builds: go to GitHub → Actions → select the latest run on main → download artifact named `app-debug-apk`. These artifacts are tied to the workflow run, not to Releases.
- Releases: push a tag like `v0.1.0` → CI creates a GitHub Release and attaches the signed APK automatically.

## Requirements
- Android 7.0+ (gestures supported reliably on 7.0/7.1+; tested on 8+)
- Enable the Accessibility Service in Settings for automation.

## OCR & Image Match
- OCR uses ML Kit Text Recognition v2; ensure initial model download completes and keep images readable for best results.
- Image matching uses an OpenCV template matching hook (integration point included; provide SDK/AAR and JNI binding).

## Safety & Policy
- Use responsibly and comply with app Terms of Service.
- The service shows a persistent notification when running; no data leaves the device by default.

## Notes on Screenshot APIs
For OCR and ImageMatch to work, integrate either:
- MediaProjection API to capture screen (with user consent), or
- AccessibilityService.takeScreenshot on API 33+ (Android 13) with appropriate flags.

Replace the ViewModel screenshotProvider() stub with a real capture path.