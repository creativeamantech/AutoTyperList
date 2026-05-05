package com.autotyper

import android.content.Context
import android.content.SharedPreferences

class SharedPrefsHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("autotyper_prefs", Context.MODE_PRIVATE)

    var currentSelectedIndex: Int
        get() = prefs.getInt("current_selected_index", 0)
        set(value) = prefs.edit().putInt("current_selected_index", value).apply()

    var autoAdvance: Boolean
        get() = prefs.getBoolean("auto_advance", false)
        set(value) = prefs.edit().putBoolean("auto_advance", value).apply()

    var showOverlayInBackground: Boolean
        get() = prefs.getBoolean("show_overlay_in_background", true)
        set(value) = prefs.edit().putBoolean("show_overlay_in_background", value).apply()

    var typingDelay: Long
        get() = prefs.getLong("typing_delay", 500L)
        set(value) = prefs.edit().putLong("typing_delay", value).apply()
}
