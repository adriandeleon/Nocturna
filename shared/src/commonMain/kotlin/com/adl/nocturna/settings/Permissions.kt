package com.adl.nocturna.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.adl.nocturna.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * Without notifications Android can't show the alarm's Stop screen over the lock screen: the alarm
 * would ring with nothing to tap. Shown wherever alarms are managed.
 */
@Composable
fun NotificationsOffWarning(modifier: Modifier = Modifier) {
    val platform = LocalSettingsPlatform.current
    Card(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(Res.string.notifications_off_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(Res.string.notifications_off_detail),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(
                onClick = { platform.open(SystemPage.NOTIFICATIONS) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer),
            ) { Text(stringResource(Res.string.notifications_off_fix), textDecoration = TextDecoration.Underline) }
        }
    }
}

/** Something a setting needs that the system hasn't granted, with the button that fixes it. */
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
                stringResource(Res.string.settings_needs_attention),
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
