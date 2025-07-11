# AutoClicker-AI (Android)

[![Build](https://github.com/your-org/your-repo/actions/workflows/android.yml/badge.svg)](https://github.com/your-org/your-repo/actions/workflows/android.yml)

> 100 % on-device macro engine that recognises what’s on the screen and taps for you – no Accessibility, no root, no data leaves your phone.

---

## 📸 What it looks like

| Visual macro recorder | Pattern editor | Animated preview |
|-----------------------|---------------|------------------|
| ![](docs/recorder.gif) | ![](docs/editor.gif) | ![](docs/preview.gif) |

*(Screenshots/gifs live under `docs/`; add your own or remove the table.)*

---

## ✨ Key features

* **Visual recorder** – capture taps & swipes, automatic delays, drag-to-re-order timeline.
* **Pattern editor** – draw rectangles, choose element type (template / text / colour), link with ABOVE / BELOW / LEFT / RIGHT arrows.
* **Computer vision** – OpenCV 4.9 multi-scale template matching + on-device Tesseract OCR.
* **Privileged TapDaemon** – Binder service that injects real touch events (no Accessibility). One-time `adb` command, no root.
* Animated preview, safe-mode watchdog, macro import/export (`.json`).

---

## 🚀 Installation

1. Download the latest artefact **AutoClicker-debug-apk.zip** from the [Actions page](https://github.com/your-org/your-repo/actions/workflows/android.yml). Un-zip and install the APK.
2. First launch → grant the Screen-capture permission.
3. (Optional) enable privileged input for accuracy/background use:

```bash
# Push & start TapDaemon
adb push tapd /data/local/tmp/
adb shell "chmod 755 /data/local/tmp/tapd && /data/local/tmp/tapd &"

# Grant the app inject-events permission once
adb shell pm grant com.mycompany.autoclicker android.permission.INJECT_EVENTS
```
If you skip step 3 the app falls back to shell `input` and works in the foreground only.

---

## 🏃‍♀️ Quick usage

| Task | Steps |
|------|-------|
| Record macro | Recorder → **Start** → perform gestures → **Stop** → edit delays/repeat → **Preview** → **Save** |
| Create pattern | Pattern Editor → draw boxes → pick element types → drag arrows → **Save** |
| Run macro | Macro list → select macro → **Play** |
| Import macro | Overflow ⋮ → **Import** → pick `.json` file |
| Export macro | Long-press macro → **Share** |

The preview draws green dots and lines so you can verify before any real input is sent.

---

## 🛠 Building TapDaemon from source

```bash
NDK=$HOME/Android/Sdk/ndk/25.2.9519653   # adjust
cd tap_daemon
$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/clang++ \
    -std=c++17 -Wall TapDaemon.cpp \
    -Ibinder_headers -landroid -lbinder -llog \
    -o tapd
```

Push & start the binary, then grant the permission as shown in the installation section.

---

## ✏️ Developer notes

* Kotlin 1.9, AGP 8.2, Material 3.
* CV: `org.opencv:opencv:4.9.0`, OCR: `tess-two:9.1.0`.
* CI builds debug & release APKs, uploaded as workflow artefacts (`.github/workflows/android.yml`).
* Macro JSON schema lives in `macro/` package.

---

## Licence
Apache 2.0
