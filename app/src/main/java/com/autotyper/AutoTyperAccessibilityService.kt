package com.autotyper

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AutoTyperAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var sharedPrefsHelper: SharedPrefsHelper

    private val typeNowReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.autotyper.ACTION_TYPE_NOW") {
                val textToType = intent.getStringExtra("TEXT_TO_TYPE")
                if (!textToType.isNullOrEmpty()) {
                    performAutoType(textToType)
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        sharedPrefsHelper = SharedPrefsHelper(this)

        val filter = IntentFilter("com.autotyper.ACTION_TYPE_NOW")
        LocalBroadcastManager.getInstance(this).registerReceiver(typeNowReceiver, filter)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not actively listening to events for typing, we wait for broadcast triggers.
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(typeNowReceiver)
    }

    private fun performAutoType(text: String) {
        serviceScope.launch {
            val delayMs = sharedPrefsHelper.typingDelay
            if (delayMs > 0) {
                delay(delayMs)
            }

            val rootNode = rootInActiveWindow
            val focusedNode = findFocusedNode(rootNode)

            if (focusedNode != null && focusedNode.isEditable) {
                // Try ACTION_SET_TEXT first
                val arguments = Bundle()
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)

                var success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

                // Fallback to ACTION_PASTE
                if (!success) {
                    val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clipData = android.content.ClipData.newPlainText("AutoTyper", text)
                    clipboardManager.setPrimaryClip(clipData)
                    success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                }

                if (success) {
                    LocalBroadcastManager.getInstance(this@AutoTyperAccessibilityService)
                        .sendBroadcast(Intent("com.autotyper.ACTION_TYPE_SUCCESS"))
                } else {
                    handleTypeFailure()
                }
                focusedNode.recycle()
            } else {
                handleTypeFailure()
            }
            rootNode?.recycle()
        }
    }

    private fun findFocusedNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isFocused && node.isEditable) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val result = findFocusedNode(child)
            if (result != null) {
                // Found a descendant that matches. We cannot recycle child
                // if result is equal to child. We just return the valid node.
                if (result != child) {
                    child?.recycle()
                }
                return result
            }
            child?.recycle()
        }
        return null
    }

    private fun handleTypeFailure() {
        Toast.makeText(this, "Please tap inside a text field first, then press Type Now.", Toast.LENGTH_LONG).show()
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent("com.autotyper.ACTION_TYPE_FAILED"))
    }
}
