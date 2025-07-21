package com.clickai.macroapp.scripting

import com.clickai.macroapp.macro.engine.*
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.StringScriptSource
import kotlin.script.experimental.jvm.JvmScriptCompiler
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

class ScriptingEngine(private val player: MacroPlayer, private val recorder: MacroRecorder) {
    fun runScript(script: String, onComplete: (() -> Unit)? = null) {
        // Expose macro APIs as simple functions in the script context
        // For now, just parse and execute a limited set of commands
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
            }
        }
        player.play(actions, onComplete)
    }
}