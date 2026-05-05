package com.autotyper.ime

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.autotyper.ItemDao
import com.autotyper.ItemDatabase
import com.autotyper.ItemEntity
import com.autotyper.SharedPrefsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AutoTyperIME : InputMethodService() {

    private lateinit var sharedPrefsHelper: SharedPrefsHelper
    private lateinit var itemDao: ItemDao

    private val imeScope = CoroutineScope(Dispatchers.Main + Job())
    private var dbJob: Job? = null
    private var allItems: List<ItemEntity> = emptyList()

    private var tvImeStatus: TextView? = null

    override fun onCreate() {
        super.onCreate()
        sharedPrefsHelper = SharedPrefsHelper(this)
        itemDao = ItemDatabase.getDatabase(this).itemDao()
    }

    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                480 // hardcoded pixels
            )
        }

        // Status Row
        tvImeStatus = TextView(this).apply {
            text = "#0 of 0 | Empty"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 14f
            setPadding(16, 16, 16, 16)
        }
        root.addView(tvImeStatus)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }

        val btnPrev = Button(this).apply {
            text = "◀"
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            setOnClickListener { moveToPrev() }
        }

        val btnTypeLayout = Button(this).apply {
            text = "⌨ TYPE NOW"
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 2f)
            setOnClickListener { typeCurrentItem() }
        }

        val btnNext = Button(this).apply {
            text = "▶"
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            setOnClickListener { moveToNext() }
        }

        btnRow.addView(btnPrev)
        btnRow.addView(btnTypeLayout)
        btnRow.addView(btnNext)
        root.addView(btnRow)

        val btnSwitch = Button(this).apply {
            text = "Switch Keyboard"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    switchToPreviousInputMethod()
                } else {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showInputMethodPicker()
                }
            }
        }
        root.addView(btnSwitch)

        // Ensure UI is up to date immediately if items exist
        refreshUI()

        return root
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        return false // ALWAYS return false — never go fullscreen
    }

    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        return true // force the input view to always show
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        setInputView(onCreateInputView()) // force re-attach the view
        loadItems()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        dbJob?.cancel()
    }

    private fun loadItems() {
        dbJob?.cancel()
        dbJob = imeScope.launch(Dispatchers.IO) {
            itemDao.getAllItems().collect { items ->
                withContext(Dispatchers.Main) {
                    allItems = items
                    refreshUI()
                }
            }
        }
    }

    private fun getClampedIndex(): Int {
        val stored = sharedPrefsHelper.currentSelectedIndex
        return stored.coerceIn(0, (allItems.size - 1).coerceAtLeast(0))
    }

    private fun typeCurrentItem() {
        val ic = currentInputConnection
        if (ic == null) {
            Toast.makeText(this, "Please tap inside a text field first.", Toast.LENGTH_SHORT).show()
            return
        }

        val idx = getClampedIndex()
        if (idx in allItems.indices) {
            val textToType = allItems[idx].text
            ic.commitText(textToType, 1)

            if (sharedPrefsHelper.autoAdvance) {
                moveToNext()
            }
        }
    }

    private fun moveToNext() {
        if (allItems.isNotEmpty()) {
            var idx = getClampedIndex()
            if (idx < allItems.size - 1) {
                idx++
            } else {
                idx = 0 // Loop
            }
            sharedPrefsHelper.currentSelectedIndex = idx
            refreshUI()
        }
    }

    private fun moveToPrev() {
        if (allItems.isNotEmpty()) {
            var idx = getClampedIndex()
            if (idx > 0) {
                idx--
            } else {
                idx = allItems.size - 1 // Loop
            }
            sharedPrefsHelper.currentSelectedIndex = idx
            refreshUI()
        }
    }

    private fun refreshUI() {
        val statusTextView = tvImeStatus ?: return

        if (allItems.isEmpty()) {
            statusTextView.text = "Empty list"
            return
        }

        val idx = getClampedIndex()
        sharedPrefsHelper.currentSelectedIndex = idx

        val currentItem = allItems[idx]
        statusTextView.text = "#${idx + 1} of ${allItems.size} | ${currentItem.text}"
    }

    override fun onDestroy() {
        super.onDestroy()
        imeScope.cancel()
    }
}
