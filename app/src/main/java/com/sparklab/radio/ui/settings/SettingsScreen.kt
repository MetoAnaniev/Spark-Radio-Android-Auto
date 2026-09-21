package com.sparklab.radio.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sparklab.radio.domain.model.StreamQuality
import com.sparklab.radio.ui.viewmodel.RadioViewModel
import kotlinx.coroutines.launch

/**
 * Settings — clean One UI style: sectioned cards, switches, and a quality chip row.
 * Only the options required by the spec (theme, quality, autostart, wifi-only).
 */
@Composable
fun SettingsScreen(vm: RadioViewModel) {
    val settings by vm.settingsRepo.settings.collectAsState(initial = com.sparklab.radio.domain.model.UserSettings())
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
        )

        // ── Appearance ───────────────────────────────────────────────────
        SettingsCard {
            SwitchRow(
                title = "Dark theme",
                subtitle = "Recommended for driving at night",
                checked = settings.darkTheme,
                onCheckedChange = { scope.launch { vm.settingsRepo.setDarkTheme(it) } },
            )
        }

        Spacer(Modifier.height(14.dp))

        // ── Playback ─────────────────────────────────────────────────────
        SettingsCard {
            Text("Streaming quality",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text("Higher quality uses more data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Row {
                StreamQuality.entries.forEach { q ->
                    FilterChip(
                        selected = settings.quality == q,
                        onClick = { scope.launch { vm.settingsRepo.setQuality(q) } },
                        label = { Text(q.label) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── Android Auto ─────────────────────────────────────────────────
        SettingsCard {
            SwitchRow(
                title = "Auto-start last station",
                subtitle = "Resume playback when Android Auto connects",
                checked = settings.autoStartLastStation,
                onCheckedChange = { scope.launch { vm.settingsRepo.setAutoStartLastStation(it) } },
            )
            HorizontalDivider(Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            SwitchRow(
                title = "Stream on Wi-Fi only",
                subtitle = "Avoid mobile data charges while streaming",
                checked = settings.wifiOnly,
                onCheckedChange = { scope.launch { vm.settingsRepo.setWifiOnly(it) } },
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) { content() }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
