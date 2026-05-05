package com.autotyper

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var sharedPrefsHelper: SharedPrefsHelper
    private lateinit var itemDao: ItemDao

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var allItems: List<ItemEntity> = emptyList()

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.autotyper.ACTION_UPDATE_OVERLAY") {
                checkVisibilityAndRefresh()
            } else if (intent?.action == "com.autotyper.ACTION_TYPE_FAILED") {
                flashButtonRed()
            } else if (intent?.action == "com.autotyper.ACTION_TYPE_SUCCESS") {
                if (sharedPrefsHelper.autoAdvance) {
                    moveToNext()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        sharedPrefsHelper = SharedPrefsHelper(this)
        itemDao = ItemDatabase.getDatabase(this).itemDao()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_controls, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        setupDrag(overlayView.findViewById(R.id.dragHandle), params)
        setupButtons()

        windowManager.addView(overlayView, params)

        val filter = IntentFilter().apply {
            addAction("com.autotyper.ACTION_UPDATE_OVERLAY")
            addAction("com.autotyper.ACTION_TYPE_FAILED")
            addAction("com.autotyper.ACTION_TYPE_SUCCESS")
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, filter)

        loadItemsAndRefreshUI()
    }

    private fun setupDrag(dragView: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        dragView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(overlayView, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupButtons() {
        overlayView.findViewById<View>(R.id.btnPrev).setOnClickListener {
            moveToPrev()
        }

        overlayView.findViewById<View>(R.id.btnNext).setOnClickListener {
            moveToNext()
        }

        overlayView.findViewById<View>(R.id.btnClose).setOnClickListener {
            stopSelf()
        }

        overlayView.findViewById<Button>(R.id.btnTypeNow).setOnClickListener {
            val idx = sharedPrefsHelper.currentSelectedIndex
            if (idx in allItems.indices) {
                val textToType = allItems[idx].text
                val intent = Intent("com.autotyper.ACTION_TYPE_NOW")
                intent.putExtra("TEXT_TO_TYPE", textToType)
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            }
        }
    }

    private fun moveToNext() {
        if (allItems.isNotEmpty()) {
            var idx = sharedPrefsHelper.currentSelectedIndex
            if (idx < allItems.size - 1) {
                idx++
            } else {
                idx = 0 // Loop around
            }
            sharedPrefsHelper.currentSelectedIndex = idx
            refreshUI()
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent("com.autotyper.ACTION_UPDATE_OVERLAY"))
        }
    }

    private fun moveToPrev() {
        if (allItems.isNotEmpty()) {
            var idx = sharedPrefsHelper.currentSelectedIndex
            if (idx > 0) {
                idx--
            } else {
                idx = allItems.size - 1 // Loop around
            }
            sharedPrefsHelper.currentSelectedIndex = idx
            refreshUI()
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent("com.autotyper.ACTION_UPDATE_OVERLAY"))
        }
    }

    private fun loadItemsAndRefreshUI() {
        serviceScope.launch {
            itemDao.getAllItems().collect { items ->
                allItems = items
                refreshUI()
            }
        }
    }

    private fun refreshUI() {
        val tvSerial = overlayView.findViewById<TextView>(R.id.tvSerial)
        val tvPreview = overlayView.findViewById<TextView>(R.id.tvPreview)

        if (allItems.isEmpty()) {
            tvSerial.text = "-"
            tvPreview.text = "Empty"
            return
        }

        var idx = sharedPrefsHelper.currentSelectedIndex
        if (idx >= allItems.size) {
            idx = allItems.size - 1
            sharedPrefsHelper.currentSelectedIndex = idx
        }

        val currentItem = allItems[idx]
        tvSerial.text = currentItem.serialNumber.toString()
        tvPreview.text = currentItem.text
    }

    private fun checkVisibilityAndRefresh() {
        // Here we could hide/show overlay based on foreground state,
        // but typically overlay just refreshes.
        refreshUI()
    }

    private fun flashButtonRed() {
        val btn = overlayView.findViewById<Button>(R.id.btnTypeNow)
        val originalColor = btn.backgroundTintList
        btn.setBackgroundColor(Color.RED)

        Handler(Looper.getMainLooper()).postDelayed({
            btn.backgroundTintList = originalColor
        }, 1000)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver)
        if (::windowManager.isInitialized && ::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}
