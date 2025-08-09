package com.primordia.clickai.ui

import com.primordia.clickai.accessibility.ClickAiAccessibilityService

object ServiceLocator {
    @Volatile
    var service: ClickAiAccessibilityService? = null
}