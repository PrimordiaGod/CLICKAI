package com.primordia.clickai

import android.app.Application
import android.util.Log

class ClickAiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("ClickAI", "Application started")
    }
}