package com.primordia.clickai.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.primordia.clickai.accessibility.ClickAiAccessibilityService
import com.primordia.clickai.engine.*
import com.primordia.clickai.patterns.PatternDetector
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MacroEditorViewModel(app: Application) : AndroidViewModel(app) {

    private val _status = MutableLiveData("Idle")
    val status: LiveData<String> = _status

    private var runner: MacroRunner? = null

    private fun screenshotProvider(): Bitmap? {
        return ServiceLocator.service?.captureScreenshotBlocking()
    }

    fun startSampleMacro(context: android.content.Context) {
        val svc = getService() ?: run {
            _status.value = "Accessibility not enabled"
            return
        }

        val ocr = OcrDetector(::screenshotProvider)
        val img = ImageMatchDetector(::screenshotProvider)
        val color = ColorDetector(::screenshotProvider)
        val pattern = PatternDetector(::screenshotProvider, ocr, img) { null }

        val engine = MacroEngine(
            svc = svc,
            ocr = ocr,
            img = img,
            colors = color,
            patterns = pattern
        )
        val macro = Macro(
            name = "Sample",
            steps = listOf(
                Wait(500),
                Click(540f, 1100f),
                Wait(300),
                Swipe(200f, 1000f, 800f, 1000f, 300),
                Loop(count = 2, steps = listOf(Wait(200), Click(540f, 1200f)))
            )
        )
        val r = MacroRunner(engine)
        runner = r
        _status.value = "Running"
        MainScope().launch { r.start(macro) }
    }

    fun stopMacro() {
        runner?.stop()
        _status.value = "Stopped"
    }

    private fun getService(): ClickAiAccessibilityService? = ServiceLocator.service
}