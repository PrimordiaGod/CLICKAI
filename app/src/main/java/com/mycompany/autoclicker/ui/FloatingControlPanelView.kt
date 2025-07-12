package com.mycompany.autoclicker.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.mycompany.autoclicker.macro.*
import kotlin.math.*

/**
 * Advanced floating control panel with professional UI and extensive customization
 */
class FloatingControlPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    // Configuration
    private var uiConfig = UIConfig()
    private var isRunning = false
    private var clickCount = 0L
    private var sessionStartTime = 0L
    private var currentStats = mutableMapOf<String, Any>()
    
    // UI State
    private var isDragging = false
    private var isResizing = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var startX = 0f
    private var startY = 0f
    
    // Animation
    private var minimizeAnimator: ValueAnimator? = null
    private var pulseAnimator: ValueAnimator? = null
    private var pulseScale = 1.0f
    
    // Paint objects for different themes
    private val paintBackground = Paint().apply {
        isAntiAlias = true
    }
    private val paintBorder = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val paintText = Paint().apply {
        isAntiAlias = true
        textSize = 32f
        color = Color.WHITE
    }
    private val paintIcon = Paint().apply {
        isAntiAlias = true
    }
    
    // Control elements
    private val controlButtons = mutableListOf<ControlButton>()
    private val statusIndicators = mutableListOf<StatusIndicator>()
    
    // Callbacks
    private var onStartStopListener: ((Boolean) -> Unit)? = null
    private var onPauseResumeListener: ((Boolean) -> Unit)? = null
    private var onSettingsListener: (() -> Unit)? = null
    private var onStatsListener: (() -> Unit)? = null
    
    init {
        setWillNotDraw(false)
        initializeControls()
        updateTheme()
        startPulseAnimation()
    }
    
    /**
     * Update UI configuration
     */
    fun updateConfig(config: UIConfig) {
        this.uiConfig = config
        updateTheme()
        updateLayout()
        invalidate()
    }
    
    /**
     * Update running state
     */
    fun setRunning(running: Boolean) {
        this.isRunning = running
        if (running) {
            sessionStartTime = System.currentTimeMillis()
            clickCount = 0
            startPulseAnimation()
        } else {
            stopPulseAnimation()
        }
        updateControlButtons()
        invalidate()
    }
    
    /**
     * Update click statistics
     */
    fun updateStats(count: Long, stats: Map<String, Any>) {
        this.clickCount = count
        this.currentStats = stats.toMutableMap()
        if (uiConfig.showStatistics) {
            invalidate()
        }
    }
    
    /**
     * Set event listeners
     */
    fun setOnStartStopListener(listener: (Boolean) -> Unit) {
        this.onStartStopListener = listener
    }
    
    fun setOnPauseResumeListener(listener: (Boolean) -> Unit) {
        this.onPauseResumeListener = listener
    }
    
    fun setOnSettingsListener(listener: () -> Unit) {
        this.onSettingsListener = listener
    }
    
    fun setOnStatsListener(listener: () -> Unit) {
        this.onStatsListener = listener
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Apply transparency
        canvas.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(), (uiConfig.transparency * 255).toInt())
        
        // Draw background with theme
        drawBackground(canvas)
        
        // Draw control buttons
        drawControlButtons(canvas)
        
        // Draw status indicators
        drawStatusIndicators(canvas)
        
        // Draw statistics if enabled
        if (uiConfig.showStatistics) {
            drawStatistics(canvas)
        }
        
        // Draw resize handle
        drawResizeHandle(canvas)
        
        canvas.restore()
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                startX = x
                startY = y
                
                // Check if touching resize handle
                if (isTouchingResizeHandle(event.x, event.y)) {
                    isResizing = true
                    return true
                }
                
                // Check if touching control buttons
                val buttonClicked = checkButtonTouch(event.x, event.y)
                if (buttonClicked) {
                    return true
                }
                
                // Start dragging
                isDragging = true
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.x - lastTouchX
                val deltaY = event.y - lastTouchY
                
                if (isResizing) {
                    // Handle resizing
                    handleResize(deltaX, deltaY)
                } else if (isDragging) {
                    // Handle dragging
                    handleDrag(deltaX, deltaY)
                }
                
                return true
            }
            
            MotionEvent.ACTION_UP -> {
                isDragging = false
                isResizing = false
                
                // Save new position
                uiConfig = uiConfig.copy(position = PointF(x, y))
                
                return true
            }
        }
        
        return super.onTouchEvent(event)
    }
    
    /**
     * Initialize control buttons and indicators
     */
    private fun initializeControls() {
        controlButtons.clear()
        statusIndicators.clear()
        
        // Main control buttons
        controlButtons.add(ControlButton(
            id = "start_stop",
            icon = if (isRunning) "stop" else "play",
            label = if (isRunning) "Stop" else "Start",
            color = if (isRunning) Color.RED else Color.GREEN,
            onClick = { onStartStopListener?.invoke(!isRunning) }
        ))
        
        controlButtons.add(ControlButton(
            id = "pause_resume",
            icon = "pause",
            label = "Pause",
            color = Color.YELLOW,
            onClick = { onPauseResumeListener?.invoke(true) }
        ))
        
        controlButtons.add(ControlButton(
            id = "settings",
            icon = "settings",
            label = "Settings",
            color = Color.BLUE,
            onClick = { onSettingsListener?.invoke() }
        ))
        
        controlButtons.add(ControlButton(
            id = "stats",
            icon = "stats",
            label = "Stats",
            color = Color.CYAN,
            onClick = { onStatsListener?.invoke() }
        ))
        
        // Status indicators
        statusIndicators.add(StatusIndicator(
            id = "running_status",
            label = "Status",
            value = if (isRunning) "Running" else "Stopped",
            color = if (isRunning) Color.GREEN else Color.RED
        ))
        
        statusIndicators.add(StatusIndicator(
            id = "click_count",
            label = "Clicks",
            value = clickCount.toString(),
            color = Color.WHITE
        ))
    }
    
    /**
     * Update theme based on configuration
     */
    private fun updateTheme() {
        when (uiConfig.skinTheme) {
            SkinTheme.DEFAULT -> applyDefaultTheme()
            SkinTheme.NEON -> applyNeonTheme()
            SkinTheme.MINIMAL -> applyMinimalTheme()
            SkinTheme.GAMER -> applyGamerTheme()
            SkinTheme.PROFESSIONAL -> applyProfessionalTheme()
            SkinTheme.CUSTOM -> applyCustomTheme()
        }
    }
    
    /**
     * Apply default theme
     */
    private fun applyDefaultTheme() {
        paintBackground.apply {
            color = Color.parseColor("#CC000000")
            shader = null
        }
        paintBorder.color = Color.WHITE
        paintText.color = Color.WHITE
    }
    
    /**
     * Apply neon theme
     */
    private fun applyNeonTheme() {
        val gradient = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(Color.parseColor("#FF00FF"), Color.parseColor("#00FFFF")),
            null, Shader.TileMode.CLAMP
        )
        paintBackground.apply {
            color = Color.parseColor("#DD000000")
            shader = gradient
        }
        paintBorder.color = Color.parseColor("#00FFFF")
        paintText.color = Color.parseColor("#00FFFF")
    }
    
    /**
     * Apply minimal theme
     */
    private fun applyMinimalTheme() {
        paintBackground.apply {
            color = Color.parseColor("#EEFFFFFF")
            shader = null
        }
        paintBorder.color = Color.parseColor("#CCCCCCCC")
        paintText.color = Color.parseColor("#333333")
    }
    
    /**
     * Apply gamer theme
     */
    private fun applyGamerTheme() {
        val gradient = RadialGradient(
            width / 2f, height / 2f, max(width, height) / 2f,
            intArrayOf(Color.parseColor("#FF4500"), Color.parseColor("#8B0000")),
            null, Shader.TileMode.CLAMP
        )
        paintBackground.apply {
            color = Color.parseColor("#DD000000")
            shader = gradient
        }
        paintBorder.color = Color.parseColor("#FF4500")
        paintText.color = Color.parseColor("#FFD700")
    }
    
    /**
     * Apply professional theme
     */
    private fun applyProfessionalTheme() {
        paintBackground.apply {
            color = Color.parseColor("#E0263238")
            shader = null
        }
        paintBorder.color = Color.parseColor("#607D8B")
        paintText.color = Color.parseColor("#ECEFF1")
    }
    
    /**
     * Apply custom theme
     */
    private fun applyCustomTheme() {
        // Custom theme implementation
        applyDefaultTheme()
    }
    
    /**
     * Update layout based on orientation
     */
    private fun updateLayout() {
        val layoutParams = layoutParams
        if (uiConfig.orientation == Orientation.HORIZONTAL) {
            layoutParams.width = 400
            layoutParams.height = 120
        } else {
            layoutParams.width = 120
            layoutParams.height = 400
        }
        setLayoutParams(layoutParams)
    }
    
    /**
     * Draw background with current theme
     */
    private fun drawBackground(canvas: Canvas) {
        val cornerRadius = 20f
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        
        // Apply pulse scale
        canvas.save()
        canvas.scale(pulseScale, pulseScale, width / 2f, height / 2f)
        
        // Draw background
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paintBackground)
        
        // Draw border
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paintBorder)
        
        canvas.restore()
    }
    
    /**
     * Draw control buttons
     */
    private fun drawControlButtons(canvas: Canvas) {
        val buttonSize = 40f
        val spacing = 10f
        val startX = if (uiConfig.orientation == Orientation.HORIZONTAL) 20f else (width - buttonSize) / 2f
        val startY = if (uiConfig.orientation == Orientation.HORIZONTAL) (height - buttonSize) / 2f else 20f
        
        controlButtons.forEachIndexed { index, button ->
            val x = if (uiConfig.orientation == Orientation.HORIZONTAL) {
                startX + (buttonSize + spacing) * index
            } else {
                startX
            }
            val y = if (uiConfig.orientation == Orientation.HORIZONTAL) {
                startY
            } else {
                startY + (buttonSize + spacing) * index
            }
            
            button.bounds = RectF(x, y, x + buttonSize, y + buttonSize)
            drawButton(canvas, button)
        }
    }
    
    /**
     * Draw individual button
     */
    private fun drawButton(canvas: Canvas, button: ControlButton) {
        val bounds = button.bounds
        
        // Draw button background
        paintIcon.color = button.color
        paintIcon.alpha = 180
        canvas.drawRoundRect(bounds, 8f, 8f, paintIcon)
        
        // Draw button border
        paintBorder.color = button.color
        paintBorder.alpha = 255
        canvas.drawRoundRect(bounds, 8f, 8f, paintBorder)
        
        // Draw button icon (simplified)
        paintIcon.color = Color.WHITE
        paintIcon.alpha = 255
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        val iconSize = 12f
        
        when (button.icon) {
            "play" -> drawPlayIcon(canvas, centerX, centerY, iconSize)
            "stop" -> drawStopIcon(canvas, centerX, centerY, iconSize)
            "pause" -> drawPauseIcon(canvas, centerX, centerY, iconSize)
            "settings" -> drawSettingsIcon(canvas, centerX, centerY, iconSize)
            "stats" -> drawStatsIcon(canvas, centerX, centerY, iconSize)
        }
    }
    
    /**
     * Draw status indicators
     */
    private fun drawStatusIndicators(canvas: Canvas) {
        if (uiConfig.isMinimized) return
        
        val textSize = 24f
        paintText.textSize = textSize
        val lineHeight = textSize + 4f
        val startX = 20f
        val startY = height - (statusIndicators.size * lineHeight) - 10f
        
        statusIndicators.forEachIndexed { index, indicator ->
            val y = startY + (lineHeight * index)
            
            // Update indicator values
            when (indicator.id) {
                "running_status" -> {
                    indicator.value = if (isRunning) "Running" else "Stopped"
                    indicator.color = if (isRunning) Color.GREEN else Color.RED
                }
                "click_count" -> {
                    indicator.value = clickCount.toString()
                }
            }
            
            // Draw indicator
            paintText.color = indicator.color
            val text = "${indicator.label}: ${indicator.value}"
            canvas.drawText(text, startX, y, paintText)
        }
    }
    
    /**
     * Draw statistics panel
     */
    private fun drawStatistics(canvas: Canvas) {
        if (uiConfig.isMinimized || !uiConfig.showStatistics) return
        
        val statsRect = RectF(width - 200f, 10f, width - 10f, height - 10f)
        
        // Draw stats background
        paintBackground.alpha = 200
        canvas.drawRoundRect(statsRect, 8f, 8f, paintBackground)
        
        // Draw stats text
        paintText.textSize = 20f
        paintText.color = Color.WHITE
        val sessionTime = (System.currentTimeMillis() - sessionStartTime) / 1000
        val cps = if (sessionTime > 0) clickCount / sessionTime else 0
        
        val statsText = listOf(
            "Session: ${sessionTime}s",
            "Clicks: $clickCount",
            "CPS: $cps"
        )
        
        statsText.forEachIndexed { index, text ->
            canvas.drawText(text, statsRect.left + 10f, statsRect.top + 30f + (index * 25f), paintText)
        }
    }
    
    /**
     * Draw resize handle
     */
    private fun drawResizeHandle(canvas: Canvas) {
        if (uiConfig.isMinimized) return
        
        val handleSize = 20f
        val handleX = width - handleSize
        val handleY = height - handleSize
        
        paintIcon.color = Color.GRAY
        paintIcon.alpha = 128
        canvas.drawRect(handleX, handleY, width.toFloat(), height.toFloat(), paintIcon)
        
        // Draw resize lines
        paintIcon.color = Color.WHITE
        paintIcon.alpha = 255
        paintIcon.strokeWidth = 2f
        canvas.drawLine(handleX + 5f, handleY + 15f, handleX + 15f, handleY + 15f, paintIcon)
        canvas.drawLine(handleX + 5f, handleY + 10f, handleX + 15f, handleY + 10f, paintIcon)
        canvas.drawLine(handleX + 10f, handleY + 5f, handleX + 10f, handleY + 15f, paintIcon)
        canvas.drawLine(handleX + 15f, handleY + 5f, handleX + 15f, handleY + 15f, paintIcon)
    }
    
    /**
     * Simple icon drawing methods
     */
    private fun drawPlayIcon(canvas: Canvas, x: Float, y: Float, size: Float) {
        val path = Path()
        path.moveTo(x - size/2, y - size)
        path.lineTo(x + size, y)
        path.lineTo(x - size/2, y + size)
        path.close()
        canvas.drawPath(path, paintIcon)
    }
    
    private fun drawStopIcon(canvas: Canvas, x: Float, y: Float, size: Float) {
        canvas.drawRect(x - size, y - size, x + size, y + size, paintIcon)
    }
    
    private fun drawPauseIcon(canvas: Canvas, x: Float, y: Float, size: Float) {
        canvas.drawRect(x - size, y - size, x - size/3, y + size, paintIcon)
        canvas.drawRect(x + size/3, y - size, x + size, y + size, paintIcon)
    }
    
    private fun drawSettingsIcon(canvas: Canvas, x: Float, y: Float, size: Float) {
        canvas.drawCircle(x, y, size, paintIcon)
        canvas.drawCircle(x, y, size/2, paintBackground)
    }
    
    private fun drawStatsIcon(canvas: Canvas, x: Float, y: Float, size: Float) {
        canvas.drawRect(x - size, y + size/2, x - size/2, y + size, paintIcon)
        canvas.drawRect(x - size/2, y, x, y + size, paintIcon)
        canvas.drawRect(x, y - size/2, x + size/2, y + size, paintIcon)
        canvas.drawRect(x + size/2, y - size, x + size, y + size, paintIcon)
    }
    
    /**
     * Check if touch is on control button
     */
    private fun checkButtonTouch(x: Float, y: Float): Boolean {
        for (button in controlButtons) {
            if (button.bounds.contains(x, y)) {
                button.onClick()
                return true
            }
        }
        return false
    }
    
    /**
     * Check if touch is on resize handle
     */
    private fun isTouchingResizeHandle(x: Float, y: Float): Boolean {
        val handleSize = 20f
        return x >= width - handleSize && y >= height - handleSize
    }
    
    /**
     * Handle dragging
     */
    private fun handleDrag(deltaX: Float, deltaY: Float) {
        val newX = (startX + deltaX).coerceIn(0f, (parent as ViewGroup).width - width.toFloat())
        val newY = (startY + deltaY).coerceIn(0f, (parent as ViewGroup).height - height.toFloat())
        
        x = newX
        y = newY
    }
    
    /**
     * Handle resizing
     */
    private fun handleResize(deltaX: Float, deltaY: Float) {
        val newWidth = (width + deltaX).coerceIn(200f, 800f)
        val newHeight = (height + deltaY).coerceIn(100f, 600f)
        
        layoutParams.width = newWidth.toInt()
        layoutParams.height = newHeight.toInt()
        requestLayout()
    }
    
    /**
     * Update control button states
     */
    private fun updateControlButtons() {
        controlButtons.find { it.id == "start_stop" }?.apply {
            icon = if (isRunning) "stop" else "play"
            label = if (isRunning) "Stop" else "Start"
            color = if (isRunning) Color.RED else Color.GREEN
        }
    }
    
    /**
     * Start pulse animation
     */
    private fun startPulseAnimation() {
        if (!isRunning) return
        
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofFloat(1.0f, 1.05f, 1.0f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                pulseScale = animation.animatedValue as Float
                invalidate()
            }
        }
        pulseAnimator?.start()
    }
    
    /**
     * Stop pulse animation
     */
    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseScale = 1.0f
        invalidate()
    }
    
    /**
     * Toggle minimized state
     */
    fun toggleMinimized() {
        uiConfig = uiConfig.copy(isMinimized = !uiConfig.isMinimized)
        
        minimizeAnimator?.cancel()
        minimizeAnimator = ValueAnimator.ofFloat(
            if (uiConfig.isMinimized) 1.0f else 0.3f,
            if (uiConfig.isMinimized) 0.3f else 1.0f
        ).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val scale = animation.animatedValue as Float
                scaleX = scale
                scaleY = scale
                alpha = if (uiConfig.isMinimized) 0.7f else 1.0f
            }
        }
        minimizeAnimator?.start()
    }
    
    /**
     * Data classes for UI elements
     */
    data class ControlButton(
        val id: String,
        var icon: String,
        var label: String,
        var color: Int,
        val onClick: () -> Unit,
        var bounds: RectF = RectF()
    )
    
    data class StatusIndicator(
        val id: String,
        val label: String,
        var value: String,
        var color: Int
    )
}