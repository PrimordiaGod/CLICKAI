package com.mycompany.autoclicker

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mycompany.autoclicker.cv.ColorDetector
import com.mycompany.autoclicker.cv.CvTemplateMatcher
import com.mycompany.autoclicker.macro.*
import com.mycompany.autoclicker.pattern.PatternDefinition
import com.mycompany.autoclicker.pattern.PatternRecognitionEngine
import com.mycompany.autoclicker.pattern.PatternType
import com.mycompany.autoclicker.tap.TapClient
import com.mycompany.autoclicker.ui.AdvancedControlPanel
import com.mycompany.autoclicker.ui.DetectionOverlayView
import com.mycompany.autoclicker.ui.AdvancedControlPanel.PerformanceMetrics
import kotlinx.coroutines.*
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.atomic.AtomicLong

class MainActivity : AppCompatActivity() {

    private var screenGrabber: ScreenGrabber? = null
    private lateinit var preview: ImageView
    private lateinit var overlay: DetectionOverlayView
    private lateinit var tvInfo: TextView
    private lateinit var controlPanel: AdvancedControlPanel
    private lateinit var fabRecord: FloatingActionButton
    private lateinit var fabPlay: FloatingActionButton
    private lateinit var fabSettings: FloatingActionButton
    
    private val scope = MainScope()
    private lateinit var macroManager: MacroManager
    private lateinit var patternEngine: PatternRecognitionEngine
    private lateinit var tapClient: TapClient
    
    // Performance monitoring
    private val frameCount = AtomicLong(0)
    private val lastFrameTime = AtomicLong(0)
    private val fpsHistory = mutableListOf<Float>()
    private val maxFpsHistorySize = 30
    
    // Recording state
    private var isRecording = false
    private var recordedActions = mutableListOf<Action>()
    private var recordingStartTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_advanced)

        initializeViews()
        initializeServices()
        setupEventListeners()
        setupPerformanceMonitoring()
        
        // Request screen capture permission
        ScreenGrabber.requestPermission(this)
    }

    private fun initializeViews() {
        preview = findViewById(R.id.preview)
        overlay = findViewById(R.id.overlay)
        tvInfo = findViewById(R.id.tvInfo)
        controlPanel = findViewById(R.id.controlPanel)
        fabRecord = findViewById(R.id.fabRecord)
        fabPlay = findViewById(R.id.fabPlay)
        fabSettings = findViewById(R.id.fabSettings)
    }

    private fun initializeServices() {
        // Initialize macro manager
        macroManager = MacroManager(this)
        
        // Initialize pattern recognition engine
        patternEngine = PatternRecognitionEngine()
        
        // Initialize tap client
        tapClient = TapClient(this)
        
        // Set up control panel
        controlPanel.setMacroManager(macroManager)
        controlPanel.setPatternEngine(patternEngine)
        
        // Register some default patterns
        registerDefaultPatterns()
        
        // Create some example macros
        createExampleMacros()
    }

    private fun setupEventListeners() {
        // Set touch listener for selection mode (long press toggles)
        overlay.setOnTouchListener(object : View.OnTouchListener {
            var startX = 0f
            var startY = 0f
            var selecting = false
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                event ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.x
                        startY = event.y
                        selecting = true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (selecting) {
                            val endX = event.x
                            val endY = event.y
                            val rect = Rect(startX.toInt(), startY.toInt(), endX.toInt(), endY.toInt())
                            overlay.addSelection(rect)
                            selecting = false
                        }
                    }
                }
                return true
            }
        })

        // FAB event listeners
        fabRecord.setOnClickListener {
            toggleRecording()
        }

        fabPlay.setOnClickListener {
            showMacroSelectionDialog()
        }

        fabSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun setupPerformanceMonitoring() {
        scope.launch {
            while (isActive) {
                val currentTime = System.currentTimeMillis()
                val lastTime = lastFrameTime.get()
                
                if (lastTime > 0) {
                    val fps = 1000f / (currentTime - lastTime)
                    fpsHistory.add(fps)
                    if (fpsHistory.size > maxFpsHistorySize) {
                        fpsHistory.removeAt(0)
                    }
                }
                
                lastFrameTime.set(currentTime)
                delay(1000) // Update every second
            }
        }
    }

    private fun registerDefaultPatterns() {
        // Register some common UI patterns
        val buttonPattern = PatternDefinition(
            id = "button_pattern",
            name = "Button Pattern",
            type = PatternType.TEMPLATE_MATCH,
            threshold = 0.7f,
            priority = 1
        )
        patternEngine.registerPattern(buttonPattern)

        val textPattern = PatternDefinition(
            id = "text_pattern",
            name = "Text Pattern",
            type = PatternType.TEXT_PATTERN,
            textPatterns = listOf(Regex(".*")),
            threshold = 0.8f,
            priority = 2
        )
        patternEngine.registerPattern(textPattern)
    }

    private fun createExampleMacros() {
        // Example macro 1: Simple click automation
        val simpleMacro = macro("Simple Click") {
            waitUntil(Condition.TemplatePresent(generateTemplateBitmap(), 0.9f)) {
                click(100, 200)
                waitMs(500)
            }
            actions {
                inputText("Hello World")
            }
        }

        val macroInfo1 = MacroInfo(
            id = "simple_click_macro",
            name = "Simple Click Macro",
            macro = simpleMacro,
            priority = 1,
            tags = setOf("basic", "click")
        )
        macroManager.addMacro(macroInfo1)

        // Example macro 2: Advanced pattern-based macro
        val advancedMacro = macro("Advanced Pattern Macro") {
            waitUntil(Condition.TemplatePresent(generateTemplateBitmap(), 0.8f)) {
                click(150, 250)
                waitMs(1000)
            }
            actions {
                swipe(100, 100, 300, 300, 500)
                inputText("Advanced automation")
            }
        }

        val macroInfo2 = MacroInfo(
            id = "advanced_pattern_macro",
            name = "Advanced Pattern Macro",
            macro = advancedMacro,
            priority = 2,
            maxExecutions = 5,
            tags = setOf("advanced", "pattern")
        )
        macroManager.addMacro(macroInfo2)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ScreenGrabber.REQUEST_MEDIA_PROJECTION) {
            screenGrabber = ScreenGrabber.fromResult(this, resultCode, data)?.apply {
                startScreenCapture()
            }
        }
    }

    private fun startScreenCapture() {
        screenGrabber?.start { bitmap: Bitmap ->
            runOnUiThread {
                preview.setImageBitmap(bitmap)
                updatePerformanceMetrics(bitmap)
            }

            // Run detection every 10th frame (~1-2 fps) to keep UI smooth
            if (frameCount.get() % 10 == 0) {
                performAdvancedDetection(bitmap)
            }
            frameCount.incrementAndGet()
        }
    }

    private fun performAdvancedDetection(bitmap: Bitmap) {
        scope.launch {
            try {
                val startTime = System.currentTimeMillis()
                
                // Perform pattern recognition
                val patternMatches = patternEngine.detectPatterns(bitmap)
                
                // Perform template matching
                val templateResult = CvTemplateMatcher.matchMultiScale(bitmap, generateTemplateBitmap())
                
                val detectionTime = System.currentTimeMillis() - startTime
                
                runOnUiThread {
                    updateDetectionResults(templateResult, patternMatches, detectionTime)
                }
                
                // Check if any macros should be triggered
                checkMacroTriggers(bitmap, patternMatches)
                
            } catch (e: Exception) {
                runOnUiThread {
                    controlPanel.updateStatus("Detection error: ${e.message}", true)
                }
            }
        }
    }

    private fun updateDetectionResults(
        templateResult: CvTemplateMatcher.MultiScaleResult,
        patternMatches: List<com.mycompany.autoclicker.pattern.PatternMatch>,
        detectionTime: Long
    ) {
        if (templateResult.score > 0.8f) {
            val rect = Rect(
                templateResult.x,
                templateResult.y,
                (templateResult.x + generateTemplateBitmap().width * templateResult.scale).toInt(),
                (templateResult.y + generateTemplateBitmap().height * templateResult.scale).toInt()
            )
            val sampledColor = ColorDetector.sampleColor(preview.drawable?.toBitmap() ?: return, rect.centerX(), rect.centerY())
            
            overlay.boundingRect = rect
            overlay.sampleColor = sampledColor
            tvInfo.text = "Score: %.2f Scale: %.2f\nRGB: #%06X\nPatterns: %d\nDetection: %dms".format(
                templateResult.score, templateResult.scale, sampledColor and 0xFFFFFF,
                patternMatches.size, detectionTime
            )
        }

        // Update performance metrics
        val avgFps = fpsHistory.average().toFloat()
        val memoryUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val activeMacros = macroManager.getRunningMacros().size
        
        val metrics = PerformanceMetrics(
            fps = avgFps,
            detectionTime = detectionTime,
            memoryUsage = memoryUsage,
            activeMacros = activeMacros
        )
        controlPanel.updatePerformanceMetrics(metrics)
    }

    private fun checkMacroTriggers(bitmap: Bitmap, patternMatches: List<com.mycompany.autoclicker.pattern.PatternMatch>) {
        scope.launch {
            val macros = macroManager.macroState.value.values.filter { it.isEnabled }
            
            for (macroInfo in macros) {
                try {
                    // Check if macro should be triggered based on patterns
                    val shouldTrigger = patternMatches.any { it.confidence >= 0.8f }
                    
                    if (shouldTrigger) {
                        macroManager.executeMacro(macroInfo.id, { bitmap }, tapClient)
                        runOnUiThread {
                            Snackbar.make(controlPanel, "Executing macro: ${macroInfo.name}", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        controlPanel.updateStatus("Macro execution failed: ${e.message}", true)
                    }
                }
            }
        }
    }

    private fun updatePerformanceMetrics(bitmap: Bitmap) {
        val currentTime = System.currentTimeMillis()
        val lastTime = lastFrameTime.get()
        
        if (lastTime > 0) {
            val fps = 1000f / (currentTime - lastTime)
            fpsHistory.add(fps)
            if (fpsHistory.size > maxFpsHistorySize) {
                fpsHistory.removeAt(0)
            }
        }
        
        lastFrameTime.set(currentTime)
    }

    private fun toggleRecording() {
        if (!isRecording) {
            startRecording()
        } else {
            stopRecording()
        }
    }

    private fun startRecording() {
        isRecording = true
        recordedActions.clear()
        recordingStartTime = System.currentTimeMillis()
        fabRecord.setImageResource(android.R.drawable.ic_media_pause)
        
        Snackbar.make(controlPanel, "Recording started", Snackbar.LENGTH_SHORT).show()
        controlPanel.updateStatus("Recording macro...", false)
    }

    private fun stopRecording() {
        isRecording = false
        fabRecord.setImageResource(android.R.drawable.ic_media_record)
        
        if (recordedActions.isNotEmpty()) {
            showRecordingSaveDialog()
        }
        
        Snackbar.make(controlPanel, "Recording stopped", Snackbar.LENGTH_SHORT).show()
        controlPanel.updateStatus("Recording stopped", false)
    }

    private fun showRecordingSaveDialog() {
        // Show dialog to save recorded macro
        val macro = macro("Recorded Macro") {
            actions {
                for (action in recordedActions) {
                    when (action) {
                        is Action.Click -> click(action.x, action.y)
                        is Action.Swipe -> swipe(action.x1, action.y1, action.x2, action.y2, action.durationMs)
                        is Action.Wait -> waitMs(action.millis)
                        is Action.InputText -> inputText(action.text)
                        else -> {}
                    }
                }
            }
        }

        val macroInfo = MacroInfo(
            id = "recorded_${System.currentTimeMillis()}",
            name = "Recorded Macro",
            macro = macro,
            tags = setOf("recorded")
        )
        
        macroManager.addMacro(macroInfo)
        Snackbar.make(controlPanel, "Macro saved: ${macroInfo.name}", Snackbar.LENGTH_LONG).show()
    }

    private fun showMacroSelectionDialog() {
        val macros = macroManager.macroState.value.values.toList()
        if (macros.isEmpty()) {
            Snackbar.make(controlPanel, "No macros available", Snackbar.LENGTH_SHORT).show()
            return
        }

        // Show macro selection dialog
        val macroNames = macros.map { it.name }.toTypedArray()
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Select Macro")
            .setItems(macroNames) { _, which ->
                val selectedMacro = macros[which]
                executeMacro(selectedMacro)
            }
            .show()
    }

    private fun executeMacro(macroInfo: MacroInfo) {
        scope.launch {
            try {
                val bitmap = preview.drawable?.toBitmap() ?: return@launch
                macroManager.executeMacro(macroInfo.id, { bitmap }, tapClient)
                
                runOnUiThread {
                    Snackbar.make(controlPanel, "Executing: ${macroInfo.name}", Snackbar.LENGTH_SHORT).show()
                    controlPanel.updateStatus("Executing macro: ${macroInfo.name}", false)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    controlPanel.updateStatus("Macro execution failed: ${e.message}", true)
                }
            }
        }
    }

    private fun showSettingsDialog() {
        // Show settings dialog with advanced options
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Settings")
            .setItems(arrayOf("Performance", "Patterns", "Macros", "Advanced")) { _, which ->
                when (which) {
                    0 -> showPerformanceSettings()
                    1 -> showPatternSettings()
                    2 -> showMacroSettings()
                    3 -> showAdvancedSettings()
                }
            }
            .show()
    }

    private fun showPerformanceSettings() {
        // Show performance monitoring settings
        Snackbar.make(controlPanel, "Performance settings", Snackbar.LENGTH_SHORT).show()
    }

    private fun showPatternSettings() {
        // Show pattern recognition settings
        Snackbar.make(controlPanel, "Pattern settings", Snackbar.LENGTH_SHORT).show()
    }

    private fun showMacroSettings() {
        // Show macro management settings
        Snackbar.make(controlPanel, "Macro settings", Snackbar.LENGTH_SHORT).show()
    }

    private fun showAdvancedSettings() {
        // Show advanced settings
        Snackbar.make(controlPanel, "Advanced settings", Snackbar.LENGTH_SHORT).show()
    }

    private fun generateTemplateBitmap(): Bitmap {
        val size = 100
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint().apply { color = Color.RED }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        return bmp
    }

    override fun onDestroy() {
        super.onDestroy()
        macroManager.shutdown()
        scope.cancel()
    }
}