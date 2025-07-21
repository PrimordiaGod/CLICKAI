package com.clickai.macroapp

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.clickai.macroapp.macro.engine.*
import android.app.AlertDialog
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorListenerAdapter
import com.clickai.macroapp.scripting.*

class MainActivity : AppCompatActivity() {
    private val recorder = MacroRecorder()
    private var player: MacroPlayer? = null
    private lateinit var macroList: ListView
    private lateinit var timelineList: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var timelineAdapter: TimelineAdapter
    private var currentMacroName: String = ""
    private lateinit var scriptList: ListView
    private lateinit var scriptAdapter: ArrayAdapter<String>
    private lateinit var editScript: EditText
    private lateinit var scriptingEngine: ScriptingEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        macroList = findViewById(R.id.macroList)
        timelineList = findViewById(R.id.timelineList)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, MacroStorage.listMacros(this))
        macroList.adapter = adapter
        timelineAdapter = TimelineAdapter(this, recorder)
        timelineList.adapter = timelineAdapter

        scriptingEngine = ScriptingEngine(player ?: MacroPlayer(MacroAccessibilityService()), recorder)
        scriptList = findViewById(R.id.scriptList)
        scriptAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ScriptStorage.listScripts(this))
        scriptList.adapter = scriptAdapter
        editScript = findViewById(R.id.editScript)
        findViewById<Button>(R.id.btnRunScript).setOnClickListener {
            scriptingEngine.runScript(editScript.text.toString()) {
                Toast.makeText(this, "Script finished", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.btnSaveScript).setOnClickListener {
            val name = findViewById<EditText>(R.id.editMacroName).text.toString()
            if (name.isNotBlank()) {
                ScriptStorage.saveScript(this, name, editScript.text.toString())
                refreshScriptList()
                Toast.makeText(this, "Script saved", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.btnLoadScript).setOnClickListener {
            val name = findViewById<EditText>(R.id.editMacroName).text.toString()
            if (name.isNotBlank()) {
                editScript.setText(ScriptStorage.loadScript(this, name))
                Toast.makeText(this, "Script loaded", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.btnScriptHelp).setOnClickListener {
            showScriptHelpDialog()
        }
        scriptList.setOnItemClickListener { _, _, position, _ ->
            val scriptName = scriptAdapter.getItem(position) ?: return@setOnItemClickListener
            findViewById<EditText>(R.id.editMacroName).setText(scriptName)
        }

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
        timelineList.setOnItemClickListener { _, _, position, _ ->
            showEditActionDialog(position)
        }
        refreshTimeline()
        val loopBtn = Button(this)
        loopBtn.text = "Add Loop"
        loopBtn.setOnClickListener { showAddLoopDialog() }
        val layout = findViewById<LinearLayout>(R.id.mainLayout)
        layout.addView(loopBtn, 2) // Insert after macro controls
    }

    private fun refreshMacroList() {
        adapter.clear()
        adapter.addAll(MacroStorage.listMacros(this))
        adapter.notifyDataSetChanged()
    }

    private fun refreshTimeline() {
        timelineAdapter.notifyDataSetChanged()
    }

    private fun refreshScriptList() {
        scriptAdapter.clear()
        scriptAdapter.addAll(ScriptStorage.listScripts(this))
        scriptAdapter.notifyDataSetChanged()
    }

    // --- Timeline Editing ---
    private fun showEditActionDialog(position: Int) {
        val actions = recorder.getActions().toMutableList()
        val action = actions[position]
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Action")
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val delayInput = EditText(this)
        delayInput.inputType = InputType.TYPE_CLASS_NUMBER
        delayInput.hint = "Delay (ms)"
        when (action) {
            is MacroAction.Tap -> delayInput.setText(action.delay.toString())
            is MacroAction.Swipe -> delayInput.setText(action.delay.toString())
            is MacroAction.Wait -> delayInput.setText(action.duration.toString())
        }
        layout.addView(delayInput)
        builder.setView(layout)
        builder.setPositiveButton("Save") { _, _ ->
            val delay = delayInput.text.toString().toLongOrNull() ?: 0L
            when (action) {
                is MacroAction.Tap -> actions[position] = action.copy(delay = delay)
                is MacroAction.Swipe -> actions[position] = action.copy(delay = delay)
                is MacroAction.Wait -> actions[position] = action.copy(duration = delay)
            }
            recorder.clear()
            actions.forEach {
                when (it) {
                    is MacroAction.Tap -> recorder.recordTap(it.x, it.y)
                    is MacroAction.Swipe -> recorder.recordSwipe(it.x1, it.y1, it.x2, it.y2, it.duration)
                    is MacroAction.Wait -> recorder.recordWait(it.duration)
                }
            }
            refreshTimeline()
        }
        builder.setNegativeButton("Cancel", null)
        builder.setNeutralButton("Delete") { _, _ ->
            actions.removeAt(position)
            recorder.clear()
            actions.forEach {
                when (it) {
                    is MacroAction.Tap -> recorder.recordTap(it.x, it.y)
                    is MacroAction.Swipe -> recorder.recordSwipe(it.x1, it.y1, it.x2, it.y2, it.duration)
                    is MacroAction.Wait -> recorder.recordWait(it.duration)
                }
            }
            refreshTimeline()
        }
        builder.show()
    }

    private fun showAddLoopDialog() {
        val actions = recorder.getActions().toMutableList()
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Loop")
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val startInput = EditText(this)
        startInput.inputType = InputType.TYPE_CLASS_NUMBER
        startInput.hint = "Start index (0-based)"
        val endInput = EditText(this)
        endInput.inputType = InputType.TYPE_CLASS_NUMBER
        endInput.hint = "End index (0-based)"
        val countInput = EditText(this)
        countInput.inputType = InputType.TYPE_CLASS_NUMBER
        countInput.hint = "Repeat count"
        layout.addView(startInput)
        layout.addView(endInput)
        layout.addView(countInput)
        builder.setView(layout)
        builder.setPositiveButton("Add") { _, _ ->
            val start = startInput.text.toString().toIntOrNull() ?: 0
            val end = endInput.text.toString().toIntOrNull() ?: 0
            val count = countInput.text.toString().toIntOrNull() ?: 1
            actions.add(MacroAction.Loop(start, end, count))
            recorder.clear()
            actions.forEach {
                when (it) {
                    is MacroAction.Tap -> recorder.recordTap(it.x, it.y)
                    is MacroAction.Swipe -> recorder.recordSwipe(it.x1, it.y1, it.x2, it.y2, it.duration)
                    is MacroAction.Wait -> recorder.recordWait(it.duration)
                    is MacroAction.Loop -> {} // Already added
                }
            }
            refreshTimeline()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showScriptHelpDialog() {
        AlertDialog.Builder(this)
            .setTitle("Scripting API Help")
            .setMessage("""Available APIs:\n- tap(x, y)\n- swipe(x1, y1, x2, y2, duration)\n- wait(ms)\n- loop(startIndex, endIndex, count)\n""")
            .setPositiveButton("OK", null)
            .show()
    }
}

class TimelineAdapter(private val context: Context, private val recorder: MacroRecorder) : BaseAdapter() {
    override fun getCount(): Int = recorder.getActions().size
    override fun getItem(position: Int): Any = recorder.getActions()[position]
    override fun getItemId(position: Int): Long = position.toLong()
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val action = recorder.getActions()[position]
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.HORIZONTAL
        val text = TextView(context)
        text.text = action.toString()
        layout.addView(text, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val upBtn = ImageButton(context)
        upBtn.setImageResource(android.R.drawable.arrow_up_float)
        upBtn.setOnClickListener {
            if (position > 0) {
                swapActions(position, position - 1)
            }
        }
        layout.addView(upBtn)
        val downBtn = ImageButton(context)
        downBtn.setImageResource(android.R.drawable.arrow_down_float)
        downBtn.setOnClickListener {
            if (position < recorder.getActions().size - 1) {
                swapActions(position, position + 1)
            }
        }
        layout.addView(downBtn)
        return layout
    }
    private fun swapActions(pos1: Int, pos2: Int) {
        val actions = recorder.getActions().toMutableList()
        val tmp = actions[pos1]
        actions[pos1] = actions[pos2]
        actions[pos2] = tmp
        recorder.clear()
        actions.forEach {
            when (it) {
                is MacroAction.Tap -> recorder.recordTap(it.x, it.y)
                is MacroAction.Swipe -> recorder.recordSwipe(it.x1, it.y1, it.x2, it.y2, it.duration)
                is MacroAction.Wait -> recorder.recordWait(it.duration)
            }
        }
        notifyDataSetChanged()
    }
}