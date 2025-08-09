package com.primordia.clickai.engine

import org.junit.Assert.assertTrue
import org.junit.Test

class MacroEngineTest {

    @Test
    fun testWaitOnlyMacro() {
        val fakeSvc = object : com.primordia.clickai.accessibility.ClickAiAccessibilityService() {
            override fun onInterrupt() {}
            override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {}
        }
        val fakeOcr = OcrDetector { null }
        val fakeImg = ImageMatchDetector { null }
        val engine = MacroEngine(fakeSvc, fakeOcr, fakeImg)
        val ok = kotlinx.coroutines.runBlocking {
            engine.run(Macro(name = "t", steps = listOf(Wait(10)))) { false }
        }
        assertTrue(ok)
    }
}