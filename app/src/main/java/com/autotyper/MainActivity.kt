package com.autotyper

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: ListViewModel by viewModels()
    private lateinit var adapter: ItemListAdapter
    private lateinit var sharedPrefsHelper: SharedPrefsHelper

    private lateinit var tvOverlayStatus: TextView
    private lateinit var tvAccessibilityStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPrefsHelper = SharedPrefsHelper(this)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.inflateMenu(R.menu.main_menu)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.action_import -> {
                    showImportDialog()
                    true
                }
                else -> false
            }
        }

        setupPermissionsUI()
        setupRecyclerView()
        setupAddButton()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allItems.collect { items ->
                    adapter.submitList(items)
                    // Bound check the selected index
                    var idx = sharedPrefsHelper.currentSelectedIndex
                    if (idx >= items.size) {
                        idx = maxOf(0, items.size - 1)
                        sharedPrefsHelper.currentSelectedIndex = idx
                    }
                    adapter.selectedIndex = idx
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatuses()
        checkAndStartOverlay()
    }

    private fun setupPermissionsUI() {
        tvOverlayStatus = findViewById(R.id.tvOverlayStatus)
        tvAccessibilityStatus = findViewById(R.id.tvAccessibilityStatus)

        findViewById<Button>(R.id.btnOverlayPermission).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            }
        }

        findViewById<Button>(R.id.btnAccessibilityPermission).setOnClickListener {
            if (!isAccessibilityServiceEnabled()) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
        }
    }

    private fun updatePermissionStatuses() {
        val hasOverlay = Settings.canDrawOverlays(this)
        tvOverlayStatus.text = "Display Over Other Apps: " + (if (hasOverlay) "Enabled" else "Disabled")

        val hasAccessibility = isAccessibilityServiceEnabled()
        tvAccessibilityStatus.text = "Accessibility Service: " + (if (hasAccessibility) "Enabled" else "Disabled")
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        var isEnabled = false
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (service in enabledServices) {
            if (service.resolveInfo.serviceInfo.packageName == packageName) {
                isEnabled = true
                break
            }
        }
        return isEnabled
    }

    private fun checkAndStartOverlay() {
        if (Settings.canDrawOverlays(this)) {
            startService(Intent(this, FloatingOverlayService::class.java))
        }
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        adapter = ItemListAdapter(
            onDeleteClick = { item -> viewModel.delete(item) },
            onItemClick = { position ->
                sharedPrefsHelper.currentSelectedIndex = position
                adapter.selectedIndex = position
                // Notify overlay of selection change
                LocalBroadcastManager.getInstance(this).sendBroadcast(Intent("com.autotyper.ACTION_UPDATE_OVERLAY"))
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val item = adapter.currentList[position]
                viewModel.delete(item)
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
    }

    private fun setupAddButton() {
        val etNewItem: TextInputEditText = findViewById(R.id.etNewItem)
        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            val text = etNewItem.text?.toString()?.trim()
            if (!text.isNullOrEmpty()) {
                viewModel.insert(text)
                etNewItem.text?.clear()
            }
        }
    }

    private fun showImportDialog() {
        val editText = TextInputEditText(this)
        editText.hint = "Paste multiple lines here"

        AlertDialog.Builder(this)
            .setTitle("Import List")
            .setView(editText)
            .setPositiveButton("Import") { _, _ ->
                val lines = editText.text?.toString()?.split("\n")?.map { it.trim() }?.filter { it.isNotEmpty() }
                if (!lines.isNullOrEmpty()) {
                    viewModel.insertMultiple(lines)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
