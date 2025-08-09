package com.primordia.clickai.engine

import kotlinx.coroutines.*

class MacroRunner(
    private val engine: MacroEngine,
) {
    private var job: Job? = null

    fun start(macro: Macro) {
        stop()
        job = CoroutineScope(Dispatchers.Default).launch {
            engine.run(macro) { !isActive }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    val isRunning: Boolean get() = job?.isActive == true
}