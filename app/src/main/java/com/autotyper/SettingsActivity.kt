package com.autotyper

import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPrefsHelper: SharedPrefsHelper
    private val viewModel: ListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPrefsHelper = SharedPrefsHelper(this)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val switchAutoAdvance: SwitchMaterial = findViewById(R.id.switchAutoAdvance)
        val btnClearAll: Button = findViewById(R.id.btnClearAll)
        val rgTypingDelay: RadioGroup = findViewById(R.id.rgTypingDelay)

        switchAutoAdvance.isChecked = sharedPrefsHelper.autoAdvance
        switchAutoAdvance.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefsHelper.autoAdvance = isChecked
        }

        when (sharedPrefsHelper.typingDelay) {
            0L -> rgTypingDelay.check(R.id.rbDelay0)
            50L -> rgTypingDelay.check(R.id.rbDelay50)
            100L -> rgTypingDelay.check(R.id.rbDelay100)
            200L -> rgTypingDelay.check(R.id.rbDelay200)
            else -> rgTypingDelay.check(R.id.rbDelay50) // Default
        }

        rgTypingDelay.setOnCheckedChangeListener { _, checkedId ->
            sharedPrefsHelper.typingDelay = when (checkedId) {
                R.id.rbDelay0 -> 0L
                R.id.rbDelay50 -> 50L
                R.id.rbDelay100 -> 100L
                R.id.rbDelay200 -> 200L
                else -> 50L
            }
        }

        btnClearAll.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear All")
                .setMessage("Are you sure you want to delete all items?")
                .setPositiveButton("Yes") { _, _ ->
                    viewModel.clearAll()
                    sharedPrefsHelper.currentSelectedIndex = 0
                }
                .setNegativeButton("No", null)
                .show()
        }
    }
}
