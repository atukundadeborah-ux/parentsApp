package com.example.parentapp.ui.reminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parentapp.core.designsystem.component.DatePickerField
import com.example.parentapp.core.designsystem.component.LoadingState
import com.example.parentapp.core.designsystem.component.ParentsAppDropdownField
import com.example.parentapp.core.designsystem.component.ParentsAppTextField
import com.example.parentapp.core.designsystem.component.ParentsAppTopBar
import com.example.parentapp.core.designsystem.component.PrimaryButton
import com.example.parentapp.core.designsystem.component.TimePickerField
import com.example.parentapp.domain.model.Priority
import com.example.parentapp.domain.model.ReminderCategory
import com.example.parentapp.domain.model.ReminderLeadTime

@Composable
fun ReminderEditorScreen(
    onDone: () -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: ReminderEditorViewModel = viewModel(factory = ReminderEditorViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.done) { if (state.done) onDone() }

    Scaffold(
        topBar = {
            ParentsAppTopBar(
                title = if (state.editing) "Edit reminder" else "New reminder",
                onNavigateUp = onNavigateUp,
            )
        },
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ParentsAppDropdownField(
                label = "Child",
                selected = state.children.firstOrNull { it.id == state.childId },
                options = state.children,
                optionLabel = { it.fullName },
                onSelected = { viewModel.onChild(it.id) },
                isError = state.errors.containsKey("child"),
                errorText = state.errors["child"],
            )
            ParentsAppTextField(
                value = state.title,
                onValueChange = viewModel::onTitle,
                label = "What is the reminder for?",
                isError = state.errors.containsKey("title"),
                errorText = state.errors["title"],
            )
            ParentsAppDropdownField(
                label = "Category",
                selected = state.category,
                options = ReminderCategory.entries,
                optionLabel = { it.label },
                onSelected = viewModel::onCategory,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DatePickerField(
                    label = "Date",
                    value = state.dateTimeMillis,
                    onValueChange = viewModel::onDate,
                    modifier = Modifier.weight(1f),
                )
                TimePickerField(
                    label = "Time",
                    value = state.dateTimeMillis,
                    onValueChange = viewModel::onTime,
                    modifier = Modifier.weight(1f),
                    isError = state.errors.containsKey("when"),
                    errorText = state.errors["when"],
                )
            }
            ParentsAppDropdownField(
                label = "Alert me",
                selected = state.leadTime,
                options = ReminderLeadTime.entries,
                optionLabel = { it.label },
                onSelected = viewModel::onLeadTime,
            )
            ParentsAppDropdownField(
                label = "Priority",
                selected = state.priority,
                options = Priority.entries,
                optionLabel = { it.label },
                onSelected = viewModel::onPriority,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Send a notification", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Turn off to keep this as a checklist item only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.notificationEnabled, onCheckedChange = viewModel::onNotificationEnabled)
            }
            if (state.notificationEnabled && state.exactAlarmsBlocked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Text(
                        "Exact alarms are disabled for this app, so alerts may be delayed. Enable them in system settings for on-time reminders.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.size(8.dp))
            PrimaryButton(
                text = if (state.editing) "Save changes" else "Create reminder",
                onClick = viewModel::save,
                loading = state.saving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
