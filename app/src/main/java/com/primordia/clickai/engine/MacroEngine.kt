package com.primordia.clickai.engine

import android.os.SystemClock
import android.util.Log
import com.primordia.clickai.accessibility.ClickAiAccessibilityService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

class MacroEngine(
    private val svc: ClickAiAccessibilityService,
    private val ocr: OcrDetector,
    private val img: ImageMatchDetector,
    private val colors: ColorDetector = ColorDetector { null },
    private val patterns: PatternDetector? = null
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
            is Click -> { svc.performTap(step.x, step.y); delay(80); true }
            is Swipe -> { svc.performSwipe(step.fromX, step.fromY, step.toX, step.toY, step.durationMs); delay(120); true }
            is Wait -> { delay(step.durationMs); true }
            is InputText -> { svc.enterText(step.text) }
            is OcrWait -> {
                val until = SystemClock.uptimeMillis() + step.timeoutMs
                while (SystemClock.uptimeMillis() < until) {
                    if (ocr.containsText(step.textOrRegex, step.region, step.isRegex)) return true
                    delay(150)
                }
                false
            }
            is ImageMatch -> {
                val until = SystemClock.uptimeMillis() + step.timeoutMs
                while (SystemClock.uptimeMillis() < until) {
                    val center = img.findTemplateCenter(step.templateAsset, step.threshold, step.region, step.multiScale)
                    if (center != null) return true
                    delay(150)
                }
                false
            }
            is ColorWait -> {
                val until = SystemClock.uptimeMillis() + step.timeoutMs
                while (SystemClock.uptimeMillis() < until) {
                    if (colors.containsColor(step.region, step.color, step.tolerance)) return true
                    delay(120)
                }
                false
            }
            is PatternWait -> {
                val det = patterns ?: return false
                val until = SystemClock.uptimeMillis() + step.timeoutMs
                while (SystemClock.uptimeMillis() < until) {
                    if (det.detectByName(step.patternName)) return true
                    delay(250)
                }
                false
            }
            is Loop -> { repeat(step.count) { for (inner in step.steps) if (!execute(inner, isCancelled)) return false }; true }
            is Conditional -> {
                val predicateOk = when (val p = step.predicate) {
                    is OcrWait -> ocr.containsText(p.textOrRegex, p.region, p.isRegex)
                    is ImageMatch -> img.findTemplateCenter(p.templateAsset, p.threshold, p.region, p.multiScale) != null
                    is ColorWait -> colors.containsColor(p.region, p.color, p.tolerance)
                    is PatternWait -> patterns?.detectByName(p.patternName) == true
                    else -> false
                }
                val branch = if (predicateOk) step.thenSteps else step.elseSteps
                for (b in branch) if (!execute(b, isCancelled)) return false
                true
            }
        }
    }
}