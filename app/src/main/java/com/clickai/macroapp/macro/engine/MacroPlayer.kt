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
                    is MacroAction.Loop -> {
                        for (i in 0 until action.count) {
                            for (j in action.startIndex..action.endIndex) {
                                val loopAction = actions.getOrNull(j) ?: continue
                                when (loopAction) {
                                    is MacroAction.Tap -> service.performTap(loopAction.x, loopAction.y)
                                    is MacroAction.Swipe -> service.performSwipe(loopAction.x1, loopAction.y1, loopAction.x2, loopAction.y2, loopAction.duration)
                                    is MacroAction.Wait -> delay(loopAction.duration)
                                    else -> {}
                                }
                            }
                        }
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