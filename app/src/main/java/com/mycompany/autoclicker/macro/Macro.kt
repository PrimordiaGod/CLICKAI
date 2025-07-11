package com.mycompany.autoclicker.macro

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Macro(private val name: String) {
    internal val steps = mutableListOf<Step>()

    class Step(val condition: Condition?, val actions: List<Action>)

    fun waitUntil(condition: Condition, actions: List<Action> = emptyList()) {
        steps += Step(condition, actions)
    }

    fun doActions(actions: List<Action>) {
        steps += Step(null, actions)
    }

    suspend fun execute(scope: CoroutineScope, frameProvider: suspend () -> Bitmap, tapper: TapInterface) {
        for (step in steps) {
            if (step.condition != null) {
                while (true) {
                    val frame = frameProvider()
                    if (step.condition.eval(frame)) break
                    delay(200)
                }
            }
            for (action in step.actions) {
                when (action) {
                    is Action.Click -> withContext(Dispatchers.IO) { tapper.tap(action.x, action.y) }
                    is Action.Swipe -> withContext(Dispatchers.IO) { tapper.swipe(action.x1, action.y1, action.x2, action.y2, action.durationMs) }
                    is Action.Wait -> delay(action.millis)
                    is Action.InputText -> withContext(Dispatchers.IO) { tapper.inputText(action.text) }
                }
            }
        }
    }
}

class MacroBuilder(private val name: String) {
    private val macro = Macro(name)

    fun waitUntil(condition: Condition, block: ActionsScope.() -> Unit = {}) {
        val actionsScope = ActionsScope()
        block(actionsScope)
        macro.waitUntil(condition, actionsScope.list)
    }

    fun actions(block: ActionsScope.() -> Unit) {
        val scope = ActionsScope()
        block(scope)
        macro.doActions(scope.list)
    }

    fun build() = macro
}

class ActionsScope {
    val list = mutableListOf<Action>()

    fun click(x: Int, y: Int) = list.add(Action.Click(x, y))
    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, duration: Int = 100) = list.add(Action.Swipe(x1, y1, x2, y2, duration))
    fun waitMs(ms: Long) = list.add(Action.Wait(ms))
    fun inputText(text: String) = list.add(Action.InputText(text))
}

fun macro(name: String, builder: MacroBuilder.() -> Unit): Macro {
    val mb = MacroBuilder(name)
    mb.builder()
    return mb.build()
}

interface TapInterface {
    fun tap(x: Int, y: Int)
    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int)
    fun inputText(text: String)
}