package com.example.parentapp.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parentapp.core.designsystem.component.ConfirmDialog
import com.example.parentapp.core.designsystem.component.ParentsAppDropdownField
import com.example.parentapp.core.designsystem.component.ParentsAppTopBar
import com.example.parentapp.core.designsystem.component.SectionHeader
import com.example.parentapp.data.repository.ThemeMode
import com.example.parentapp.domain.model.ReminderLeadTime

@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onDataReset: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val resetDone by viewModel.resetDone.collectAsStateWithLifecycle()
    var confirmReset by remember { mutableStateOf(false) }

    LaunchedEffect(resetDone) { if (resetDone) onDataReset() }

    Scaffold(
        topBar = { ParentsAppTopBar(title = "Settings", onNavigateUp = onNavigateUp) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader("Appearance", modifier = Modifier.padding(horizontal = 16.dp))
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("Theme", style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                        ) { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    }
                }
            }
            SwitchRow(
                title = "Dynamic colour",
                subtitle = "Use Android 12+ wallpaper colours instead of the ParentsApp palette",
                checked = settings.dynamicColor,
                onCheckedChange = viewModel::setDynamicColor,
            )

            HorizontalDivider()
            SectionHeader("Notifications", modifier = Modifier.padding(horizontal = 16.dp))
            SwitchRow(
                title = "Reminder notifications",
                subtitle = "Deliver alerts for scheduled reminders",
                checked = settings.notificationsEnabled,
                onCheckedChange = viewModel::setNotificationsEnabled,
            )
            Column(Modifier.padding(16.dp)) {
                ParentsAppDropdownField(
                    label = "Default alert timing for new reminders",
                    selected = ReminderLeadTime.fromMinutes(settings.defaultLeadTimeMinutes),
                    options = ReminderLeadTime.entries,
                    optionLabel = { it.label },
                    onSelected = { viewModel.setDefaultLeadTime(it.minutes) },
                )
            }
            SwitchRow(
                title = "Quiet hours",
                subtitle = "Silence non-urgent reminders overnight",
                checked = settings.quietHoursEnabled,
                onCheckedChange = viewModel::setQuietHours,
            )

            HorizontalDivider()
            SectionHeader("Privacy", modifier = Modifier.padding(horizontal = 16.dp))
            SwitchRow(
                title = "Usage analytics",
                subtitle = "Off by default. No data leaves this device regardless.",
                checked = settings.analyticsEnabled,
                onCheckedChange = viewModel::setAnalytics,
            )
            ListItem(
                headlineContent = { Text("Reset demo data", color = MaterialTheme.colorScheme.error) },
                supportingContent = { Text("Wipes all local records and restores the sample dataset. You will be signed out.") },
                modifier = Modifier.padding(top = 4.dp),
                trailingContent = {
                    TextButton(onClick = { confirmReset = true }) { Text("Reset") }
                },
            )

            HorizontalDivider()
            SectionHeader("About", modifier = Modifier.padding(horizontal = 16.dp))
            ListItem(
                headlineContent = { Text("ParentsApp") },
                supportingContent = { Text("Version 1.0 · Secure, role-based child-care coordination. All data is stored locally on this device.") },
            )
        }
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "Reset demo data?",
            message = "Every child, activity, reminder and task on this device will be deleted and replaced with the sample dataset. This cannot be undone.",
            confirmLabel = "Reset",
            destructive = true,
            onConfirm = { confirmReset = false; viewModel.resetDemoData() },
            onDismiss = { confirmReset = false },
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
    )
}
