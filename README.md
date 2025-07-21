# CLICKAI

## Build & Run

1. Open this project in Android Studio (recommended).
2. Let Gradle sync and download dependencies.
3. Build and run on an Android device or emulator (minSdk 24, targetSdk 33).

## Project Structure
- `app/` — Main Android app module
- `build.gradle`, `settings.gradle` — Project build configuration

## Roadmap (Next Steps)
- [ ] Macro recording/playback (Accessibility Service)
- [ ] Timeline editor UI
- [ ] Macro import/export (JSON, code)
- [ ] Scripting engine (Kotlin)
- [ ] AI/vision integration (OpenCV, Tesseract)
- [ ] Adaptive event handler system
- [ ] Modern UI/UX improvements

---
This is a minimal Android project scaffold. All advanced macro and AI features will be implemented in subsequent steps.

## Download APK

After each push to the `main` branch, the latest debug APK is built automatically by GitHub Actions.

You can download the latest APK from the [Actions tab](https://github.com/YOUR_GITHUB_USERNAME/YOUR_REPO_NAME/actions) (look for the most recent workflow run and download the `app-debug-apk` artifact).