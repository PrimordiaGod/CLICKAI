package com.mycompany.autoclicker.macro

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

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
        repeat(repeatCount) {
            for (step in steps) {
                // --- wait for condition if present ---
                if (step.condition != null) {
                    while (true) {
                        val frame = frameProvider()
                        if (step.condition.eval(frame)) break
                        delay(200)
                    }
                }

                // --- execute actions ---
                for (action in step.actions) {
                    // compute dynamic delay with variance (if supported)
                    val extraDelay = when (action) {
                        is Action.Click -> if (action.delayVarianceMs != 0L) Random.nextLong(-action.delayVarianceMs, action.delayVarianceMs + 1) else 0L
                        is Action.Swipe -> if (action.delayVarianceMs != 0L) Random.nextLong(-action.delayVarianceMs, action.delayVarianceMs + 1) else 0L
                        else -> 0L
                    }
                    val baseDelay = when (action) {
                        is Action.Click -> action.delayMs
                        is Action.Swipe -> action.delayMs
                        else -> 0L
                    }
                    if (baseDelay + extraDelay > 0) delay(baseDelay + extraDelay)

                    when (action) {
                        is Action.Click -> {
                            val (tx, ty) = randomizedPoint(action.x, action.y, action.jitterPx)
                            withContext(Dispatchers.IO) { tapper.tap(tx, ty) }
                        }
                        is Action.Swipe -> {
                            val (sx1, sy1) = randomizedPoint(action.x1, action.y1, action.jitterPx)
                            val (sx2, sy2) = randomizedPoint(action.x2, action.y2, action.jitterPx)
                            withContext(Dispatchers.IO) { tapper.swipe(sx1, sy1, sx2, sy2, action.durationMs) }
                        }
                        is Action.Wait -> delay(action.millis)
                        is Action.InputText -> withContext(Dispatchers.IO) { tapper.inputText(action.text) }
                        is Action.ClickNorm -> {
                            val pt = util.ScreenAdapter.toScreen(action.n, screenMeta ?: util.ScreenAdapter.currentMeta(scope.coroutineContext[androidx.lifecycle.LifecycleOwner]?.javaClass?.kotlin?.objectInstance as android.content.Context))
                            withContext(Dispatchers.IO) { tapper.tap(pt.x, pt.y) }
                        }
                        is Action.SwipeNorm -> {
                            val meta = screenMeta ?: util.ScreenAdapter.currentMeta(scope.coroutineContext[androidx.lifecycle.LifecycleOwner]?.javaClass?.kotlin?.objectInstance as android.content.Context)
                            val s = util.ScreenAdapter.toScreen(action.start, meta)
                            val e = util.ScreenAdapter.toScreen(action.end, meta)
                            withContext(Dispatchers.IO) { tapper.swipe(s.x, s.y, e.x, e.y, action.durationMs) }
                        }
                    }
                }
            }
        }
    }

    var repeatCount: Int = 1

    var screenMeta: util.ScreenMeta? = null

    private fun randomizedPoint(x: Int, y: Int, jitter: Int): Pair<Int, Int> {
        if (jitter <= 0) return Pair(x, y)
        val dx = (-jitter..jitter).random()
        val dy = (-jitter..jitter).random()
        return Pair(x + dx, y + dy)
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

    // Advanced helpers
    fun clickRandom(x: Int, y: Int, jitterPx: Int, delayMs: Long = 0L, delayVarianceMs: Long = 0L) =
        list.add(Action.Click(x, y, delayMs = delayMs, jitterPx = jitterPx, delayVarianceMs = delayVarianceMs))

    fun swipeRandom(
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int,
        duration: Int = 100,
        jitterPx: Int,
        delayMs: Long = 0L,
        delayVarianceMs: Long = 0L
    ) = list.add(
        Action.Swipe(
            x1,
            y1,
            x2,
            y2,
            durationMs = duration,
            delayMs = delayMs,
            jitterPx = jitterPx,
            delayVarianceMs = delayVarianceMs
        )
    )
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