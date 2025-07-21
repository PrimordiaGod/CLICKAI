package com.clickai.macroapp.macro.engine

import kotlinx.coroutines.*

class MacroPlayer(private val service: MacroAccessibilityService) {
    private var job: Job? = null

    fun play(actions: List<MacroAction>, onComplete: (() -> Unit)? = null) {
        job = CoroutineScope(Dispatchers.Main).launch {
            for (action in actions) {
                when (action) {
                    is MacroAction.Tap -> {
                        delay(action.delay)
                        service.performTap(action.x, action.y)
                    }
                    is MacroAction.Swipe -> {
                        delay(action.delay)
                        service.performSwipe(action.x1, action.y1, action.x2, action.y2, action.duration)
                    }
                    is MacroAction.Wait -> {
                        delay(action.duration)
                    }
                }
            }
            onComplete?.invoke()
        }
    }

    fun stop() {
        job?.cancel()
    }
}