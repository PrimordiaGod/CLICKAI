# Advanced AutoClicker-AI (Android)

[![Build](https://github.com/your-org/your-repo/actions/workflows/android.yml/badge.svg)](https://github.com/your-org/your-repo/actions/workflows/android.yml)

> Advanced 100% on-device macro engine with AI-powered pattern recognition, sophisticated macro management, and real-time performance monitoring – no Accessibility, no root, no data leaves your phone.

---

## 📸 What it looks like

| Advanced Control Panel | Pattern Recognition | Performance Monitoring |
|-----------------------|-------------------|----------------------|
| ![](docs/advanced_ui.gif) | ![](docs/pattern_recognition.gif) | ![](docs/performance_monitoring.gif) |

*(Screenshots/gifs live under `docs/`; add your own or remove the table.)*

---

## ✨ Advanced Features

### 🤖 AI-Powered Pattern Recognition
- **Multi-Pattern Detection**: Simultaneously detect templates, text, colors, and layout patterns
- **Advanced OCR**: Real-time text recognition with regex pattern matching
- **Color Pattern Analysis**: HSV color space detection with tolerance ranges
- **Gesture Recognition**: Track and recognize complex touch gestures
- **Animation Detection**: Detect motion and screen changes over time
- **Layout Analysis**: Understand UI structure and element relationships

### 🎛️ Advanced Macro Management
- **Priority-Based Execution**: Macros with higher priority execute first
- **Scheduled Macros**: Time-based and interval-based macro execution
- **Execution Limits**: Set maximum execution counts per macro
- **Tag-Based Organization**: Organize macros with custom tags
- **Execution History**: Track and analyze macro performance
- **Conditional Logic**: Complex AND/OR/NOT condition combinations
- **State Management**: Persistent state tracking across macro executions

### 📊 Real-Time Performance Monitoring
- **FPS Tracking**: Monitor frame rate and detection performance
- **Memory Usage**: Track memory consumption and optimization
- **Detection Time**: Measure pattern recognition speed
- **Active Macro Count**: Monitor concurrent macro executions
- **Performance Charts**: Visual performance metrics in real-time
- **Logging System**: Comprehensive logging with different severity levels

### 🎨 Advanced UI Controls
- **Tabbed Interface**: Organized control panels for different features
- **Floating Action Buttons**: Quick access to recording, playback, and settings
- **Real-Time Status**: Live status updates and error reporting
- **Visual Feedback**: Animated previews and ghost effects
- **Responsive Design**: Adaptive UI for different screen sizes
- **Dark Theme**: Modern dark interface with Material Design

### 🔧 Advanced Conditions
- **Composite Conditions**: AND/OR/NOT logic combinations
- **Time-Based Conditions**: Start time, end time, and interval controls
- **Count-Based Conditions**: Execution counting with reset options
- **Performance Conditions**: FPS, memory, and detection time thresholds
- **State Conditions**: Persistent state tracking and comparison
- **Multi-Template Conditions**: Multiple template matching with thresholds

### 📱 Enhanced Recording & Playback
- **Visual Recording**: Real-time recording with visual feedback
- **Action Timeline**: Drag-and-drop macro editing
- **Delay Optimization**: Automatic delay calculation and adjustment
- **Preview Mode**: Safe preview before execution
- **Import/Export**: JSON-based macro sharing and backup
- **Version Control**: Macro versioning and rollback support

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

## 🏃‍♀️ Advanced Usage

### Pattern Recognition
```kotlin
// Register custom patterns
val buttonPattern = PatternDefinition(
    id = "custom_button",
    name = "Custom Button",
    type = PatternType.TEMPLATE_MATCH,
    threshold = 0.8f,
    priority = 1
)
patternEngine.registerPattern(buttonPattern)
```

### Advanced Macro Creation
```kotlin
// Complex condition-based macro
val advancedMacro = macro("Advanced Macro") {
    waitUntil(condition {
        template(buttonTemplate, 0.8f)
        text(Regex(".*button.*"), false)
        color(Rect(100, 100, 200, 200), Color.RED, 10)
        and()
    }!!) {
        click(150, 250)
        waitMs(1000)
    }
    actions {
        swipe(100, 100, 300, 300, 500)
        inputText("Advanced automation")
    }
}
```

### Performance Monitoring
```kotlin
// Monitor performance metrics
val metrics = PerformanceMetrics(
    fps = 30.0f,
    detectionTime = 150L,
    memoryUsage = 50 * 1024 * 1024L,
    activeMacros = 2
)
controlPanel.updatePerformanceMetrics(metrics)
```

### Macro Management
```kotlin
// Create macro with advanced features
val macroInfo = MacroInfo(
    id = "scheduled_macro",
    name = "Scheduled Macro",
    macro = myMacro,
    priority = 5,
    maxExecutions = 10,
    schedule = MacroSchedule(
        startTime = System.currentTimeMillis() + 60000, // 1 minute from now
        intervalMs = 300000, // Every 5 minutes
        daysOfWeek = setOf(1, 2, 3, 4, 5) // Weekdays only
    ),
    tags = setOf("scheduled", "workflow")
)
macroManager.addMacro(macroInfo)
```

---

## 🛠️ Building from Source

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 34
- NDK 25.2.9519653
- OpenCV 4.9.0
- Tesseract OCR

### Build Steps
```bash
# Clone the repository
git clone https://github.com/your-org/autoclicker-ai.git
cd autoclicker-ai

# Build TapDaemon
NDK=$HOME/Android/Sdk/ndk/25.2.9519653
cd tap_daemon
$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/clang++ \
    -std=c++17 -Wall TapDaemon.cpp \
    -Ibinder_headers -landroid -lbinder -llog \
    -o tapd

# Build Android app
cd ..
./gradlew assembleDebug
```

### Advanced Configuration
```gradle
// Add to app/build.gradle for advanced features
dependencies {
    implementation 'androidx.viewpager2:viewpager2:1.0.0'
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
}
```

---

## 📚 API Reference

### Pattern Recognition
- `PatternRecognitionEngine`: Main pattern detection engine
- `PatternDefinition`: Pattern configuration and metadata
- `PatternMatch`: Detection results with confidence scores
- `PatternType`: Supported pattern types (TEMPLATE_MATCH, TEXT_PATTERN, etc.)

### Macro Management
- `MacroManager`: Central macro management system
- `MacroInfo`: Macro metadata and configuration
- `MacroSchedule`: Scheduling and timing configuration
- `MacroExecutionResult`: Execution results and metrics

### Advanced Conditions
- `AdvancedCondition`: Base class for all conditions
- `AndCondition`, `OrCondition`, `NotCondition`: Logical operators
- `TimeCondition`: Time-based execution control
- `CountCondition`: Execution counting and limits
- `PerformanceCondition`: Performance-based triggers

### Performance Monitoring
- `PerformanceMetrics`: Real-time performance data
- `PerformanceChart`: Visual performance charts
- `MonitoringPanel`: Real-time monitoring interface

---

## 🔧 Configuration

### Advanced Settings
```json
{
  "performance": {
    "maxDetectionTime": 1000,
    "minFps": 10,
    "maxMemoryUsage": 100000000
  },
  "patterns": {
    "defaultThreshold": 0.8,
    "maxPatterns": 100,
    "cacheSize": 50
  },
  "macros": {
    "maxConcurrent": 5,
    "defaultPriority": 1,
    "historySize": 100
  }
}
```

### Pattern Configuration
```kotlin
// HSV color range detection
val colorPattern = PatternDefinition(
    id = "red_button",
    name = "Red Button",
    type = PatternType.COLOR_PATTERN,
    colorRanges = listOf(
        ColorRange(
            hsvMin = HSV(0.0, 100.0, 50.0),
            hsvMax = HSV(10.0, 255.0, 255.0),
            minPixels = 100
        )
    )
)
```

---

## 🐛 Troubleshooting

### Common Issues
1. **OpenCV Initialization Failed**: Ensure OpenCV native libraries are properly linked
2. **Pattern Detection Slow**: Reduce pattern complexity or increase threshold
3. **Memory Usage High**: Clear pattern cache or reduce concurrent macros
4. **Macro Not Executing**: Check condition logic and execution limits

### Debug Mode
```bash
# Enable debug logging
adb shell setprop log.tag.AutoClicker DEBUG

# Monitor performance
adb shell dumpsys meminfo com.mycompany.autoclicker
```

---

## 🤝 Contributing

### Development Setup
1. Fork the repository
2. Create a feature branch
3. Implement your changes
4. Add tests for new features
5. Submit a pull request

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comprehensive documentation
- Include unit tests for new features

---

## 📄 License
Apache 2.0

---

## 🙏 Acknowledgments
- OpenCV team for computer vision capabilities
- Tesseract team for OCR functionality
- Material Design team for UI components
- Android team for the robust platform
