package com.autotyper

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.core.widget.doAfterTextChanged
import androidx.localbroadcastmanager.content.LocalBroadcastManager

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
        val switchShowInBackground: SwitchMaterial = findViewById(R.id.switchShowInBackground)
        val etDelay: EditText = findViewById(R.id.etDelay)
        val btnClearAll: Button = findViewById(R.id.btnClearAll)

        switchAutoAdvance.isChecked = sharedPrefsHelper.autoAdvance
        switchShowInBackground.isChecked = sharedPrefsHelper.showOverlayInBackground
        etDelay.setText(sharedPrefsHelper.typingDelay.toString())

        switchAutoAdvance.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefsHelper.autoAdvance = isChecked
        }

        switchShowInBackground.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefsHelper.showOverlayInBackground = isChecked
            // Notify overlay service of visibility change
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent("com.autotyper.ACTION_UPDATE_OVERLAY"))
        }

        etDelay.doAfterTextChanged {
            val delayStr = it?.toString() ?: ""
            if (delayStr.isNotEmpty()) {
                sharedPrefsHelper.typingDelay = delayStr.toLongOrNull() ?: 500L
            }
        }

        btnClearAll.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear All")
                .setMessage("Are you sure you want to delete all items?")
                .setPositiveButton("Yes") { _, _ ->
                    viewModel.clearAll()
                    sharedPrefsHelper.currentSelectedIndex = 0
                    LocalBroadcastManager.getInstance(this).sendBroadcast(Intent("com.autotyper.ACTION_UPDATE_OVERLAY"))
                }
                .setNegativeButton("No", null)
                .show()
        }
    }
}
