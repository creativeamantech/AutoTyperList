package com.autotyper.ime

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.autotyper.ItemDao
import com.autotyper.ItemDatabase
import com.autotyper.ItemEntity
import com.autotyper.R
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
        return try {
            val view = layoutInflater.inflate(R.layout.ime_layout, null)

            tvImeStatus = view.findViewById(R.id.tvImeStatus)
            val btnSwitchIme = view.findViewById<Button>(R.id.btnSwitchIme)
            val btnPrev = view.findViewById<ImageButton>(R.id.btnImePrev)
            val btnNext = view.findViewById<ImageButton>(R.id.btnImeNext)
            val btnTypeNow = view.findViewById<Button>(R.id.btnImeTypeNow)

            btnSwitchIme?.setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    switchToPreviousInputMethod()
                } else {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showInputMethodPicker()
                }
            }

            btnPrev?.setOnClickListener { moveToPrev() }
            btnNext?.setOnClickListener { moveToNext() }

            btnTypeNow?.setOnClickListener {
                val ic = currentInputConnection
                if (ic == null) {
                    Toast.makeText(this, "Please tap inside a text field first.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
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

            view
        } catch (e: Exception) {
            Log.e("AutoTyperIME", "onCreateInputView failed", e)
            LinearLayout(this)
        }
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
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
        // Save the clamped index back to shared prefs
        sharedPrefsHelper.currentSelectedIndex = idx

        val currentItem = allItems[idx]
        statusTextView.text = "#${idx + 1} of ${allItems.size} | ${currentItem.text}"
    }

    override fun onDestroy() {
        super.onDestroy()
        imeScope.cancel()
    }
}
