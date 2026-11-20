package com.adl.nocturna.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.adl.nocturna.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun AboutScreen(padding: PaddingValues) {
    val uriHandler = LocalUriHandler.current
    fun open(url: String) {
        runCatching { uriHandler.openUri(url) }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp)) {
                Text("Nocturna", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text(LocalSettingsPlatform.current.versionLabel, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(Res.string.about_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }

        item { NavRow(stringResource(Res.string.about_source), REPOSITORY_URL.removePrefix("https://")) { open(REPOSITORY_URL) } }
        item { NavRow(stringResource(Res.string.about_report), stringResource(Res.string.about_report_detail)) { open("$REPOSITORY_URL/issues/new/choose") } }
        item { NavRow(stringResource(Res.string.about_licence), "MIT · $COPYRIGHT") { open("$REPOSITORY_URL/blob/main/LICENSE") } }
        item { NavRow(stringResource(Res.string.about_made_by), HOMEPAGE_URL.removePrefix("https://www.")) { open(HOMEPAGE_URL) } }

        item { BackupRows() }

        item { SectionHeader(stringResource(Res.string.about_libraries)) }
        items(LIBRARY_CREDITS) { CreditRow(it) }

        item { SectionHeader(stringResource(Res.string.about_data)) }
        items(DATA_CREDITS) { CreditRow(it) }
        item {
            Text(
                stringResource(Res.string.about_radio_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        item { SectionHeader(stringResource(Res.string.about_sounds)) }
        items(SOUND_CREDITS) { CreditRow(it) }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun CreditRow(credit: Credit) {
    val uriHandler = LocalUriHandler.current
    NavRow(credit.name, "${stringResource(credit.detail)} · ${credit.licence}") { runCatching { uriHandler.openUri(credit.url) } }
}
