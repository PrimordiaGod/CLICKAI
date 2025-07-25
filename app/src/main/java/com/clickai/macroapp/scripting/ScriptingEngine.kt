package com.clickai.macroapp.scripting

import com.clickai.macroapp.macro.engine.*
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.StringScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import com.clickai.macroapp.vision.ScreenCaptureUtil
import com.clickai.macroapp.vision.ScreenRecognizer
import com.clickai.macroapp.vision.TemplateStorage
import android.app.Activity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.clickai.macroapp.macro.engine.CorrectionStorage
import com.clickai.macroapp.macro.engine.CorrectionEvent
import com.clickai.macroapp.corrections.CorrectionDialog
import android.graphics.Bitmap
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.clickai.macroapp.macro.engine.MacroRecorder

class ScriptingEngine(private val player: MacroPlayer, private val recorder: MacroRecorder) {
    suspend fun runScriptWithVision(
        activity: Activity,
        script: String,
        onComplete: (() -> Unit)? = null,
        onPause: ((String) -> Unit)? = null
    ) {
        val lines = script.lines()
        val actions = mutableListOf<MacroAction>()
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("tap(") -> {
                    val args = trimmed.removePrefix("tap(").removeSuffix(")").split(",").map { it.trim().toFloat() }
                    actions.add(MacroAction.Tap(args[0], args[1]))
                }
                trimmed.startsWith("swipe(") -> {
                    val args = trimmed.removePrefix("swipe(").removeSuffix(")").split(",").map { it.trim().toFloat() }
                    actions.add(MacroAction.Swipe(args[0], args[1], args[2], args[3], args.getOrNull(4)?.toLong() ?: 300))
                }
                trimmed.startsWith("wait(") -> {
                    val ms = trimmed.removePrefix("wait(").removeSuffix(")").toLong()
                    actions.add(MacroAction.Wait(ms))
                }
                trimmed.startsWith("loop(") -> {
                    val args = trimmed.removePrefix("loop(").removeSuffix(")").split(",").map { it.trim().toInt() }
                    actions.add(MacroAction.Loop(args[0], args[1], args[2]))
                }
                trimmed.startsWith("recognizeText(") -> {
                    val text = trimmed.removePrefix("recognizeText(").removeSuffix(")").trim('"')
                    val key = "text:$text"
                    val bitmap = withContext(Dispatchers.Main) { captureScreenSuspend(activity) }
                    if (bitmap != null) {
                        ScreenRecognizer.initTesseract(activity)
                        val ocrResult = ScreenRecognizer.recognizeText(bitmap)
                        val found = ocrResult.contains(text, ignoreCase = true)
                        if (!found) {
                            val correction = CorrectionStorage.getCorrection(activity, key)
                            if (correction != null) {
                                actions.addAll(correction.actions)
                                continue
                            } else {
                                val correctionRecorder = MacroRecorder()
                                val userActions = withContext(Dispatchers.Main) {
                                    suspendCancellableCoroutine<List<MacroAction>?> { cont ->
                                        CorrectionDialog.show(
                                            activity,
                                            bitmap,
                                            ocrResult,
                                            onRecord = { _ -> correctionRecorder.clear() },
                                            onSave = { name, desc, _, recordedActions ->
                                                val event = CorrectionEvent(
                                                    id = key,
                                                    type = "text",
                                                    signature = key,
                                                    screenshotPath = null,
                                                    ocrResult = ocrResult,
                                                    actions = recordedActions,
                                                    name = name,
                                                    description = desc
                                                )
                                                CorrectionStorage.saveCorrection(activity, event)
                                                cont.resume(event.actions)
                                            },
                                            onCancel = { cont.resume(null) }
                                        )
                                    }
                                }
                                if (userActions != null) {
                                    actions.addAll(userActions)
                                    continue
                                }
                                onPause?.invoke("Text '$text' not found. Macro paused.")
                                return
                            }
                        }
                    }
                }
                trimmed.startsWith("matchTemplate(") -> {
                    val name = trimmed.removePrefix("matchTemplate(").removeSuffix(")").trim('"')
                    val key = "template:$name"
                    val bitmap = withContext(Dispatchers.Main) { captureScreenSuspend(activity) }
                    val template = TemplateStorage.loadTemplate(activity, name)
                    if (bitmap != null && template != null) {
                        val found = ScreenRecognizer.matchTemplate(bitmap, template)
                        if (!found) {
                            val correction = CorrectionStorage.getCorrection(activity, key)
                            if (correction != null) {
                                actions.addAll(correction.actions)
                                continue
                            } else {
                                val correctionRecorder = MacroRecorder()
                                val userActions = withContext(Dispatchers.Main) {
                                    suspendCancellableCoroutine<List<MacroAction>?> { cont ->
                                        CorrectionDialog.show(
                                            activity,
                                            bitmap,
                                            null,
                                            onRecord = { _ -> correctionRecorder.clear() },
                                            onSave = { name, desc, _, recordedActions ->
                                                val event = CorrectionEvent(
                                                    id = key,
                                                    type = "template",
                                                    signature = key,
                                                    screenshotPath = null,
                                                    ocrResult = null,
                                                    actions = recordedActions,
                                                    name = name,
                                                    description = desc
                                                )
                                                CorrectionStorage.saveCorrection(activity, event)
                                                cont.resume(event.actions)
                                            },
                                            onCancel = { cont.resume(null) }
                                        )
                                    }
                                }
                                if (userActions != null) {
                                    actions.addAll(userActions)
                                    continue
                                }
                                onPause?.invoke("Template '$name' not found. Macro paused.")
                                return
                            }
                        }
                    }
                }
            }
        }
        player.play(actions, onComplete)
    }
}

suspend fun captureScreenSuspend(activity: Activity): android.graphics.Bitmap? = null // Not implemented in CI stub