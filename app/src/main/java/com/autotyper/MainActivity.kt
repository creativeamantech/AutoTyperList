package com.autotyper

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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

    private lateinit var tvImeStatus: TextView

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
        updateImeStatus()
    }

    private fun setupPermissionsUI() {
        tvImeStatus = findViewById(R.id.tvImeStatus)

        findViewById<Button>(R.id.btnEnableKeyboard).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        findViewById<Button>(R.id.btnSwitchKeyboard).setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }
    }

    private fun updateImeStatus() {
        val isEnabled = isImeEnabled()
        val isDefault = isImeDefault()

        if (isDefault) {
            tvImeStatus.text = "Keyboard Status: Active"
            tvImeStatus.setTextColor(android.graphics.Color.parseColor("#388E3C")) // Green
        } else if (isEnabled) {
            tvImeStatus.text = "Keyboard Status: Enabled (Not Active)"
            tvImeStatus.setTextColor(android.graphics.Color.parseColor("#F57C00")) // Orange
        } else {
            tvImeStatus.text = "Keyboard Status: Disabled"
            tvImeStatus.setTextColor(android.graphics.Color.parseColor("#D32F2F")) // Red
        }
    }

    private fun isImeEnabled(): Boolean {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val imes = imm.enabledInputMethodList
        return imes.any { it.packageName == packageName }
    }

    private fun isImeDefault(): Boolean {
        val defaultIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return defaultIme?.contains(packageName) == true
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        adapter = ItemListAdapter(
            onDeleteClick = { item -> viewModel.delete(item) },
            onItemClick = { position ->
                sharedPrefsHelper.currentSelectedIndex = position
                adapter.selectedIndex = position
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
