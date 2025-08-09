package com.primordia.clickai.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.primordia.clickai.databinding.ActivityClickaiMainBinding

class MainActivity : ComponentActivity() {
    private lateinit var binding: ActivityClickaiMainBinding
    private val vm: MacroEditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClickaiMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnEnableAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.btnStart.setOnClickListener { vm.startSampleMacro(this) }
        binding.btnStop.setOnClickListener { vm.stopMacro() }

        vm.status.observe(this) { binding.txtStatus.text = "Status: $it" }

        // Open pattern editor (reusing stop button long-press for simplicity)
        binding.btnStop.setOnLongClickListener {
            startActivity(Intent(this, PatternEditorActivity::class.java)); true
        }
    }
}