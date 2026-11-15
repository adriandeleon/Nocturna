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
                    "A bedside clock for Android: a big, dimmable clock for the nightstand while your phone charges.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }

        item { NavRow("Source code", REPOSITORY_URL.removePrefix("https://")) { open(REPOSITORY_URL) } }
        item { NavRow("Report a problem", "Opens an issue on GitHub") { open("$REPOSITORY_URL/issues/new/choose") } }
        item { NavRow("Licence", "MIT · $COPYRIGHT") { open("$REPOSITORY_URL/blob/main/LICENSE") } }
        item { NavRow("Made by Adrián De León", HOMEPAGE_URL.removePrefix("https://www.")) { open(HOMEPAGE_URL) } }

        item { BackupRows() }

        item { SectionHeader("Open-source libraries") }
        items(LIBRARY_CREDITS) { CreditRow(it) }

        item { SectionHeader("Data") }
        items(DATA_CREDITS) { CreditRow(it) }
        item {
            Text(
                "Radio streams play from the stations' own servers; Nocturna carries none of their content.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun CreditRow(credit: Credit) {
    val uriHandler = LocalUriHandler.current
    NavRow(credit.name, "${credit.detail} · ${credit.licence}") { runCatching { uriHandler.openUri(credit.url) } }
}
