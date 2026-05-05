package com.autotyper

import android.app.Application

class AutoTyperApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize DB here so IME can access it safely
        ItemDatabase.getDatabase(this)
    }
}
