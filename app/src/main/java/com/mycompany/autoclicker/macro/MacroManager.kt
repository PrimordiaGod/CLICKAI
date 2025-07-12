package com.mycompany.autoclicker.macro

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class MacroInfo(
    val id: String,
    val name: String,
    val macro: Macro,
    val isEnabled: Boolean = true,
    val schedule: MacroSchedule? = null,
    val priority: Int = 0,
    val maxExecutions: Int = -1,
    val executionCount: AtomicInteger = AtomicInteger(0),
    val lastExecuted: Long = 0L,
    val tags: Set<String> = emptySet()
)

data class MacroSchedule(
    val startTime: Long? = null,
    val endTime: Long? = null,
    val intervalMs: Long? = null,
    val daysOfWeek: Set<Int> = emptySet(), // 1=Sunday, 2=Monday, etc.
    val conditions: List<Condition> = emptyList()
)

data class MacroExecutionResult(
    val success: Boolean,
    val error: String? = null,
    val executionTime: Long,
    val screenshot: Bitmap? = null
)

class MacroManager(private val context: Context) {
    private val macros = ConcurrentHashMap<String, MacroInfo>()
    private val executionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeExecutions = ConcurrentHashMap<String, Job>()
    
    private val _macroState = MutableStateFlow<Map<String, MacroInfo>>(emptyMap())
    val macroState: StateFlow<Map<String, MacroInfo>> = _macroState
    
    private val _executionHistory = MutableStateFlow<List<MacroExecutionResult>>(emptyList())
    val executionHistory: StateFlow<List<MacroExecutionResult>> = _executionHistory

    fun addMacro(macroInfo: MacroInfo) {
        macros[macroInfo.id] = macroInfo
        updateState()
    }

    fun removeMacro(id: String) {
        stopMacro(id)
        macros.remove(id)
        updateState()
    }

    fun updateMacro(id: String, update: (MacroInfo) -> MacroInfo) {
        macros[id]?.let { macroInfo ->
            macros[id] = update(macroInfo)
            updateState()
        }
    }

    fun executeMacro(id: String, frameProvider: suspend () -> Bitmap, tapper: TapInterface): Job {
        val macroInfo = macros[id] ?: throw IllegalArgumentException("Macro not found: $id")
        
        // Check execution limits
        if (macroInfo.maxExecutions > 0 && macroInfo.executionCount.get() >= macroInfo.maxExecutions) {
            throw IllegalStateException("Macro execution limit reached")
        }

        // Stop existing execution if running
        stopMacro(id)

        val job = executionScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                val screenshot = frameProvider()
                
                macroInfo.macro.execute(this, frameProvider, tapper)
                
                val executionTime = System.currentTimeMillis() - startTime
                val result = MacroExecutionResult(
                    success = true,
                    executionTime = executionTime,
                    screenshot = screenshot
                )
                
                updateMacro(id) { it.copy(
                    executionCount = AtomicInteger(it.executionCount.get() + 1),
                    lastExecuted = System.currentTimeMillis()
                ) }
                
                addExecutionHistory(result)
                
            } catch (e: Exception) {
                val result = MacroExecutionResult(
                    success = false,
                    error = e.message,
                    executionTime = 0
                )
                addExecutionHistory(result)
                throw e
            }
        }
        
        activeExecutions[id] = job
        return job
    }

    fun stopMacro(id: String) {
        activeExecutions[id]?.cancel()
        activeExecutions.remove(id)
    }

    fun stopAllMacros() {
        activeExecutions.values.forEach { it.cancel() }
        activeExecutions.clear()
    }

    fun isMacroRunning(id: String): Boolean {
        return activeExecutions[id]?.isActive == true
    }

    fun getRunningMacros(): Set<String> {
        return activeExecutions.filterValues { it.isActive }.keys
    }

    fun scheduleMacro(id: String, schedule: MacroSchedule) {
        updateMacro(id) { it.copy(schedule = schedule) }
        startScheduler()
    }

    private fun startScheduler() {
        executionScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                
                macros.values
                    .filter { it.isEnabled && it.schedule != null }
                    .forEach { macroInfo ->
                        val schedule = macroInfo.schedule!!
                        
                        // Check if it's time to execute
                        if (shouldExecuteNow(schedule, now)) {
                            // Execute in background
                            launch {
                                try {
                                    // This would need frameProvider and tapper injection
                                    // For now, just log the scheduled execution
                                    println("Scheduled execution of macro: ${macroInfo.name}")
                                } catch (e: Exception) {
                                    println("Scheduled execution failed: ${e.message}")
                                }
                            }
                        }
                    }
                
                delay(1000) // Check every second
            }
        }
    }

    private fun shouldExecuteNow(schedule: MacroSchedule, now: Long): Boolean {
        // Check time constraints
        if (schedule.startTime != null && now < schedule.startTime) return false
        if (schedule.endTime != null && now > schedule.endTime) return false
        
        // Check day of week
        if (schedule.daysOfWeek.isNotEmpty()) {
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = now
            val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
            if (!schedule.daysOfWeek.contains(dayOfWeek)) return false
        }
        
        // Check interval
        if (schedule.intervalMs != null) {
            // This would need more sophisticated interval tracking
            // For now, just return true if conditions are met
        }
        
        return true
    }

    private fun updateState() {
        _macroState.value = macros.toMap()
    }

    private fun addExecutionHistory(result: MacroExecutionResult) {
        val current = _executionHistory.value.toMutableList()
        current.add(0, result) // Add to beginning
        if (current.size > 100) { // Keep only last 100 executions
            current.removeAt(current.size - 1)
        }
        _executionHistory.value = current
    }

    fun getMacroById(id: String): MacroInfo? = macros[id]

    fun getMacrosByTag(tag: String): List<MacroInfo> {
        return macros.values.filter { it.tags.contains(tag) }
    }

    fun getMacrosByPriority(minPriority: Int): List<MacroInfo> {
        return macros.values.filter { it.priority >= minPriority }.sortedByDescending { it.priority }
    }

    fun clearExecutionHistory() {
        _executionHistory.value = emptyList()
    }

    fun shutdown() {
        stopAllMacros()
        executionScope.cancel()
    }
}