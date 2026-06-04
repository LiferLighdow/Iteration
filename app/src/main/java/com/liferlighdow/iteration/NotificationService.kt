package com.liferlighdow.iteration

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationService : NotificationListenerService() {

    companion object {
        private val _notifications = MutableStateFlow<Map<String, Int>>(emptyMap())
        val notifications = _notifications.asStateFlow()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        updateCounts()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        updateCounts()
    }

    override fun onListenerConnected() {
        updateCounts()
    }

    private fun updateCounts() {
        val activeNotifications = activeNotifications
        val counts = activeNotifications
            .filter { !it.isOngoing && it.packageName != packageName } // 過濾掉持續性通知（如音樂播放）和自己
            .groupBy { it.packageName }
            .mapValues { it.value.size }
        _notifications.value = counts
    }
}
