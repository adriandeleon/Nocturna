package com.adl.nocturna.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
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
