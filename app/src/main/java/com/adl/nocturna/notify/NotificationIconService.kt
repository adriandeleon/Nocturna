package com.adl.nocturna.notify

import android.app.Notification
import android.content.Context
import android.graphics.drawable.Icon
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotifIcon(val key: String, val packageName: String, val icon: Icon)

/**
 * Mirrors the small icons of the user's pending notifications so the clock can show them.
 * Only active once the user grants notification access in system settings.
 */
class NotificationIconService : NotificationListenerService() {

    override fun onListenerConnected() = publish()
    override fun onListenerDisconnected() {
        state.value = emptyList()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = publish()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = publish()

    private fun publish() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        state.value = active
            .asSequence()
            .filter { it.packageName != packageName && !it.isOngoing }
            .filter { it.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0 }
            .sortedByDescending { it.postTime }
            .distinctBy { it.packageName }
            .mapNotNull { sbn -> sbn.notification.smallIcon?.let { NotifIcon(sbn.key, sbn.packageName, it) } }
            .take(MAX_ICONS)
            .toList()
    }

    companion object {
        private const val MAX_ICONS = 8
        private val state = MutableStateFlow<List<NotifIcon>>(emptyList())
        val icons: StateFlow<List<NotifIcon>> = state.asStateFlow()

        fun isEnabled(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }
}
