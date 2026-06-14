package com.liferlighdow.iteration

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class IterationAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    companion object {
        private const val TAG = "IterationAccessibility"
        var instance: IterationAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        android.util.Log.d(TAG, "Service Connected")
        instance = this
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        android.util.Log.d(TAG, "Service Unbound")
        instance = null
        return super.onUnbind(intent)
    }

    fun lockScreen() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val success = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            android.util.Log.d(TAG, "Lock Screen action performed: $success")
        }
    }
}
