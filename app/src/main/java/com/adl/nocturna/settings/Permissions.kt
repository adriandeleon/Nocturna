package com.adl.nocturna.settings

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Alignment
import androidx.core.net.toUri
import com.adl.nocturna.day.CalendarEvents
import com.adl.nocturna.notify.NotificationIconService
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/** Whether Nocturna may post notifications; re-checked whenever the screen comes back to the front. */
@Composable
fun rememberNotificationsAllowed(): Boolean {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    return remember(refresh) { NotificationManagerCompat.from(context).areNotificationsEnabled() }
}

fun openNotificationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    )
}

/**
 * Without notifications Android can't show the alarm's Stop screen over the lock screen: the alarm
 * would ring with nothing to tap. Shown wherever alarms are managed.
 */
@Composable
fun NotificationsOffWarning(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Card(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Notifications are off", style = MaterialTheme.typography.titleSmall)
            Text(
                "Alarms will still sound, but on a locked phone you won't get a screen to stop them. " +
                    "Turn notifications on for Nocturna.",
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(
                onClick = { openNotificationSettings(context) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer),
            ) { Text("Turn on notifications", textDecoration = TextDecoration.Underline) }
        }
    }
}

/** What Android lets Nocturna do, from settings the user changes outside the app; re-read on every resume. */
data class SystemAccess(
    val notificationAccess: Boolean,
    val overlay: Boolean,
    val fullScreen: Boolean,
    val calendar: Boolean,
)

@Composable
fun rememberSystemAccess(): SystemAccess {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    return remember(refresh) {
        SystemAccess(
            notificationAccess = NotificationIconService.isEnabled(context),
            overlay = android.provider.Settings.canDrawOverlays(context),
            fullScreen = Build.VERSION.SDK_INT < 34 ||
                context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent(),
            calendar = CalendarEvents.hasPermission(context),
        )
    }
}

fun openOverlaySettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()))
}

fun openFullScreenSettings(context: Context) {
    if (Build.VERSION.SDK_INT < 34) return
    context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, "package:${context.packageName}".toUri()))
}

fun openNotificationAccessSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
}

/** Something a setting needs that Android hasn't granted, with the button that fixes it. */
class SettingsIssue(val title: String, val detail: String, val fix: String, val onFix: () -> Unit)

/** Everything on the hub's "Needs your attention" card, in order of how much it matters. */
@Composable
fun AttentionCard(issues: List<SettingsIssue>) {
    if (issues.isEmpty()) return
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Text(
                "Needs your attention",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            issues.forEachIndexed { i, issue ->
                if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f))
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(issue.title, style = MaterialTheme.typography.bodyLarge)
                        Text(issue.detail, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(
                        onClick = issue.onFix,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    ) { Text(issue.fix, textDecoration = TextDecoration.Underline) }
                }
            }
        }
    }
}
