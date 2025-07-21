package com.clickai.macroapp

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.clickai.macroapp.macro.engine.*

class MainActivity : AppCompatActivity() {
    private val recorder = MacroRecorder()
    private var player: MacroPlayer? = null
    private lateinit var macroList: ListView
    private lateinit var timelineList: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var timelineAdapter: ArrayAdapter<String>
    private var currentMacroName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        macroList = findViewById(R.id.macroList)
        timelineList = findViewById(R.id.timelineList)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, MacroStorage.listMacros(this))
        macroList.adapter = adapter
        timelineAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        timelineList.adapter = timelineAdapter

        findViewById<Button>(R.id.btnRecord).setOnClickListener {
            recorder.clear()
            Toast.makeText(this, "Recording started (use Accessibility button)", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            val service = MacroAccessibilityService() // In real app, get running service instance
            player = MacroPlayer(service)
            player?.play(recorder.getActions()) {
                Toast.makeText(this, "Playback finished", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val macroName = findViewById<EditText>(R.id.editMacroName).text.toString()
            if (macroName.isNotBlank()) {
                MacroStorage.saveMacro(this, macroName, recorder.getActions())
                refreshMacroList()
                Toast.makeText(this, "Macro saved", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.btnLoad).setOnClickListener {
            val macroName = findViewById<EditText>(R.id.editMacroName).text.toString()
            if (macroName.isNotBlank()) {
                val actions = MacroStorage.loadMacro(this, macroName)
                recorder.clear()
                actions.forEach {
                    when (it) {
                        is MacroAction.Tap -> recorder.recordTap(it.x, it.y)
                        is MacroAction.Swipe -> recorder.recordSwipe(it.x1, it.y1, it.x2, it.y2, it.duration)
                        is MacroAction.Wait -> recorder.recordWait(it.duration)
                    }
                }
                refreshTimeline()
                Toast.makeText(this, "Macro loaded", Toast.LENGTH_SHORT).show()
            }
        }
        macroList.setOnItemClickListener { _, _, position, _ ->
            val macroName = adapter.getItem(position) ?: return@setOnItemClickListener
            findViewById<EditText>(R.id.editMacroName).setText(macroName)
        }
        refreshTimeline()
    }

    private fun refreshMacroList() {
        adapter.clear()
        adapter.addAll(MacroStorage.listMacros(this))
        adapter.notifyDataSetChanged()
    }

    private fun refreshTimeline() {
        val actions = recorder.getActions()
        timelineAdapter.clear()
        timelineAdapter.addAll(actions.map { it.toString() })
        timelineAdapter.notifyDataSetChanged()
    }
}