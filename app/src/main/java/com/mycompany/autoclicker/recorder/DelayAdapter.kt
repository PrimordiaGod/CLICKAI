package com.mycompany.autoclicker.recorder

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mycompany.autoclicker.R
import com.mycompany.autoclicker.macro.Action

class DelayAdapter(private val actions: List<Action>) : RecyclerView.Adapter<DelayAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvLabel: TextView = v.findViewById(R.id.tvLabel)
        val etDelay: EditText = v.findViewById(R.id.etDelay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_delay, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = actions.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val act = actions[position]
        holder.tvLabel.text = when (act) {
            is Action.Click -> "Tap (${act.x},${act.y})"
            is Action.Swipe -> "Swipe (${act.x1},${act.y1}→${act.x2},${act.y2})"
            is Action.Wait -> "Wait"
            else -> act.javaClass.simpleName
        }
        val currentDelay = when (act) {
            is Action.Click -> act.delayMs
            is Action.Swipe -> act.delayMs
            else -> 0L
        }
        holder.etDelay.setText(currentDelay.toString())
        holder.etDelay.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val v = s?.toString()?.toLongOrNull() ?: 0L
                when (act) {
                    is Action.Click -> act.delayMs = v
                    is Action.Swipe -> act.delayMs = v
                }
            }
        })
    }
}