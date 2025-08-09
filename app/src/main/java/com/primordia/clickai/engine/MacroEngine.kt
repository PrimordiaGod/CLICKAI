package com.primordia.clickai.engine

import android.os.SystemClock
import android.util.Log
import com.primordia.clickai.accessibility.ClickAiAccessibilityService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

class MacroEngine(
    private val svc: ClickAiAccessibilityService,
    private val ocr: OcrDetector,
    private val img: ImageMatchDetector
) {

    suspend fun run(macro: Macro, isCancelled: () -> Boolean): Boolean {
        try {
            for (s in macro.steps) {
                if (!execute(s, isCancelled)) return false
            }
            return true
        } catch (ce: CancellationException) {
            Log.i("ClickAI", "Macro cancelled")
            return false
        } catch (t: Throwable) {
            Log.e("ClickAI", "Macro error", t)
            return false
        }
    }

    private suspend fun execute(step: Step, isCancelled: () -> Boolean): Boolean {
        if (isCancelled()) return false
        return when (step) {
            is Click -> {
                svc.performTap(step.x, step.y); delay(80); true
            }
            is Swipe -> {
                svc.performSwipe(step.fromX, step.fromY, step.toX, step.toY, step.durationMs); delay(120); true
            }
            is Wait -> {
                delay(step.durationMs); true
            }
            is OcrWait -> {
                val until = SystemClock.uptimeMillis() + step.timeoutMs
                while (SystemClock.uptimeMillis() < until) {
                    if (ocr.containsText(step.text, step.region)) return true
                    delay(150)
                }
                false
            }
            is ImageMatch -> {
                val until = SystemClock.uptimeMillis() + step.timeoutMs
                while (SystemClock.uptimeMillis() < until) {
                    val center = img.findTemplateCenter(step.templateAsset, step.threshold, step.region)
                    if (center != null) return true
                    delay(150)
                }
                false
            }
            is Loop -> {
                repeat(step.count) {
                    for (inner in step.steps) if (!execute(inner, isCancelled)) return false
                }
                true
            }
            is Conditional -> {
                val predicateOk = when (val p = step.predicate) {
                    is OcrWait -> ocr.containsText(p.text, p.region)
                    is ImageMatch -> img.findTemplateCenter(p.templateAsset, p.threshold, p.region) != null
                    else -> false
                }
                val branch = if (predicateOk) step.thenSteps else step.elseSteps
                for (b in branch) if (!execute(b, isCancelled)) return false
                true
            }
        }
    }
}