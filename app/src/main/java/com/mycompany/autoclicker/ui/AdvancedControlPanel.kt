package com.mycompany.autoclicker.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import com.mycompany.autoclicker.R
import com.mycompany.autoclicker.macro.MacroInfo
import com.mycompany.autoclicker.macro.MacroManager
import com.mycompany.autoclicker.pattern.PatternDefinition
import com.mycompany.autoclicker.pattern.PatternRecognitionEngine
import com.mycompany.autoclicker.pattern.PatternType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AdvancedControlPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var macroManager: MacroManager? = null
    private var patternEngine: PatternRecognitionEngine? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    // UI Components
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var statusCard: CardView
    private lateinit var statusText: TextView
    private lateinit var performanceChart: PerformanceChart

    init {
        initView()
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.advanced_control_panel, this, true)
        
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        statusCard = findViewById(R.id.statusCard)
        statusText = findViewById(R.id.statusText)
        performanceChart = findViewById(R.id.performanceChart)

        setupTabs()
        setupStatusCard()
    }

    private fun setupTabs() {
        val adapter = ControlPanelAdapter()
        viewPager.adapter = adapter
        
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Macros"
                1 -> "Patterns"
                2 -> "Monitor"
                3 -> "Settings"
                else -> "Tab $position"
            }
        }.attach()
    }

    private fun setupStatusCard() {
        statusCard.setCardBackgroundColor(Color.parseColor("#1A1A1A"))
        statusText.setTextColor(Color.WHITE)
    }

    fun setMacroManager(manager: MacroManager) {
        this.macroManager = manager
        scope.launch {
            manager.macroState.collectLatest { macros ->
                // Update macro list if needed
            }
        }
    }

    fun setPatternEngine(engine: PatternRecognitionEngine) {
        this.patternEngine = engine
        // Update pattern list if needed
    }

    fun updateStatus(message: String, isError: Boolean = false) {
        statusText.text = message
        statusCard.setCardBackgroundColor(
            if (isError) Color.parseColor("#FF4444") 
            else Color.parseColor("#1A1A1A")
        )
    }

    fun updatePerformanceMetrics(metrics: PerformanceMetrics) {
        performanceChart.updateMetrics(metrics)
    }

    data class PerformanceMetrics(
        val fps: Float,
        val detectionTime: Long,
        val memoryUsage: Long,
        val activeMacros: Int
    )
}

class MacroControlPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var macroNameInput: TextInputEditText
    private lateinit var prioritySlider: Slider
    private lateinit var maxExecutionsInput: TextInputEditText
    private lateinit var scheduleSwitch: SwitchMaterial
    private lateinit var tagsChipGroup: ChipGroup
    private lateinit var saveButton: MaterialButton

    init {
        initView()
    }

    private fun initView() {
        // Initialize views - simplified for now
        macroNameInput = TextInputEditText(context)
        prioritySlider = Slider(context)
        maxExecutionsInput = TextInputEditText(context)
        scheduleSwitch = SwitchMaterial(context)
        tagsChipGroup = ChipGroup(context)
        saveButton = MaterialButton(context)

        setupControls()
    }

    private fun setupControls() {
        prioritySlider.addOnChangeListener { _, value, _ ->
            // Update priority value
        }

        scheduleSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Handle schedule switch
        }

        saveButton.setOnClickListener {
            // Save macro configuration
        }
    }
}

class PatternControlPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var patternTypeSpinner: Spinner
    private lateinit var thresholdSlider: Slider
    private lateinit var colorRangeSlider: RangeSlider
    private lateinit var textPatternInput: TextInputEditText
    private lateinit var templateImageView: ImageView
    private lateinit var captureTemplateButton: MaterialButton

    init {
        initView()
    }

    private fun initView() {
        // Initialize views - simplified for now
        patternTypeSpinner = Spinner(context)
        thresholdSlider = Slider(context)
        colorRangeSlider = RangeSlider(context)
        textPatternInput = TextInputEditText(context)
        templateImageView = ImageView(context)
        captureTemplateButton = MaterialButton(context)

        setupControls()
    }

    private fun setupControls() {
        val patternTypes = PatternType.values().map { it.name }
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, patternTypes)
        patternTypeSpinner.adapter = adapter

        patternTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updatePatternTypeControls(PatternType.values()[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        captureTemplateButton.setOnClickListener {
            // Capture template from screen
        }
    }

    private fun updatePatternTypeControls(type: PatternType) {
        when (type) {
            PatternType.TEMPLATE_MATCH -> {
                templateImageView.visibility = View.VISIBLE
                captureTemplateButton.visibility = View.VISIBLE
                colorRangeSlider.visibility = View.GONE
                textPatternInput.visibility = View.GONE
            }
            PatternType.COLOR_PATTERN -> {
                templateImageView.visibility = View.GONE
                captureTemplateButton.visibility = View.GONE
                colorRangeSlider.visibility = View.VISIBLE
                textPatternInput.visibility = View.GONE
            }
            PatternType.TEXT_PATTERN -> {
                templateImageView.visibility = View.GONE
                captureTemplateButton.visibility = View.GONE
                colorRangeSlider.visibility = View.GONE
                textPatternInput.visibility = View.VISIBLE
            }
            else -> {
                templateImageView.visibility = View.GONE
                captureTemplateButton.visibility = View.GONE
                colorRangeSlider.visibility = View.GONE
                textPatternInput.visibility = View.GONE
            }
        }
    }
}

class MonitoringPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var logRecyclerView: RecyclerView

    init {
        initView()
    }

    private fun initView() {
        logRecyclerView = RecyclerView(context)
        setupLogView()
    }

    private fun setupLogView() {
        logRecyclerView.layoutManager = LinearLayoutManager(context)
        // Setup log adapter
    }

    fun addLogEntry(entry: LogEntry) {
        // Add new log entry to the list
    }

    data class LogEntry(
        val timestamp: Long,
        val level: LogLevel,
        val message: String
    )

    enum class LogLevel {
        DEBUG, INFO, WARNING, ERROR
    }
}

class PerformanceChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dataPoints = mutableListOf<AdvancedControlPanel.PerformanceMetrics>()
    private val maxDataPoints = 100

    fun updateMetrics(metrics: AdvancedControlPanel.PerformanceMetrics) {
        dataPoints.add(metrics)
        if (dataPoints.size > maxDataPoints) {
            dataPoints.removeAt(0)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (dataPoints.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        
        // Draw performance metrics
        drawFpsLine(canvas, width, height)
        drawMemoryLine(canvas, width, height)
        drawDetectionTimeLine(canvas, width, height)
    }

    private fun drawFpsLine(canvas: Canvas, width: Float, height: Float) {
        paint.color = Color.GREEN
        paint.strokeWidth = 2f
        
        val maxFps = dataPoints.maxOfOrNull { it.fps } ?: 60f
        val points = dataPoints.mapIndexed { index, metrics ->
            Pair(
                (index.toFloat() / dataPoints.size) * width,
                height - (metrics.fps / maxFps) * height
            )
        }
        
        drawLineChart(canvas, points)
    }

    private fun drawMemoryLine(canvas: Canvas, width: Float, height: Float) {
        paint.color = Color.BLUE
        paint.strokeWidth = 2f
        
        val maxMemory = dataPoints.maxOfOrNull { it.memoryUsage } ?: 1L
        val points = dataPoints.mapIndexed { index, metrics ->
            Pair(
                (index.toFloat() / dataPoints.size) * width,
                height - (metrics.memoryUsage.toFloat() / maxMemory) * height
            )
        }
        
        drawLineChart(canvas, points)
    }

    private fun drawDetectionTimeLine(canvas: Canvas, width: Float, height: Float) {
        paint.color = Color.RED
        paint.strokeWidth = 2f
        
        val maxTime = dataPoints.maxOfOrNull { it.detectionTime } ?: 1L
        val points = dataPoints.mapIndexed { index, metrics ->
            Pair(
                (index.toFloat() / dataPoints.size) * width,
                height - (metrics.detectionTime.toFloat() / maxTime) * height
            )
        }
        
        drawLineChart(canvas, points)
    }

    private fun drawLineChart(canvas: Canvas, points: List<Pair<Float, Float>>) {
        if (points.size < 2) return
        
        for (i in 0 until points.size - 1) {
            val start = points[i]
            val end = points[i + 1]
            canvas.drawLine(start.first, start.second, end.first, end.second, paint)
        }
    }
}

// Adapter classes for RecyclerViews
class MacroAdapter(
    private val macros: List<MacroInfo>,
    private val onMacroClick: (MacroInfo) -> Unit
) : RecyclerView.Adapter<MacroAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = TextView(view.context)
        val statusChip: Chip = Chip(view.context)
        val priorityText: TextView = TextView(view.context)
        val executionCountText: TextView = TextView(view.context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = View(parent.context)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val macro = macros[position]
        holder.nameText.text = macro.name
        holder.priorityText.text = "Priority: ${macro.priority}"
        holder.executionCountText.text = "Executions: ${macro.executionCount.get()}"
        
        holder.itemView.setOnClickListener { onMacroClick(macro) }
    }

    override fun getItemCount() = macros.size
}

class PatternAdapter(
    private val patterns: List<PatternDefinition>,
    private val onPatternClick: (PatternDefinition) -> Unit
) : RecyclerView.Adapter<PatternAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = TextView(view.context)
        val typeChip: Chip = Chip(view.context)
        val thresholdText: TextView = TextView(view.context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = View(parent.context)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pattern = patterns[position]
        holder.nameText.text = pattern.name
        holder.typeChip.text = pattern.type.name
        holder.thresholdText.text = "Threshold: ${pattern.threshold}"
        
        holder.itemView.setOnClickListener { onPatternClick(pattern) }
    }

    override fun getItemCount() = patterns.size
}

class ControlPanelAdapter : RecyclerView.Adapter<ControlPanelAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = View(parent.context)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Configure page content based on position
    }

    override fun getItemCount() = 4 // Macros, Patterns, Monitor, Settings
}