package com.autotyper

import android.os.Bundle
import android.widget.Button
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

        switchAutoAdvance.isChecked = sharedPrefsHelper.autoAdvance
        switchAutoAdvance.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefsHelper.autoAdvance = isChecked
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
