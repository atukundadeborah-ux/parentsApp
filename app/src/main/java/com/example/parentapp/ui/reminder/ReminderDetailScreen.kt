package com.example.parentapp.ui.reminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.parentapp.core.designsystem.component.InfoRow
import com.example.parentapp.core.designsystem.component.LoadingState
import com.example.parentapp.core.designsystem.component.ParentsAppTopBar
import com.example.parentapp.core.designsystem.component.SectionHeader
import com.example.parentapp.core.designsystem.component.StatusBadge
import com.example.parentapp.core.util.DateTimeFormatters
import com.example.parentapp.domain.model.ReminderLeadTime
import com.example.parentapp.domain.model.ReminderStatus
import com.example.parentapp.ui.common.EmptyHint
import com.example.parentapp.ui.common.tone

@Composable
fun ReminderDetailScreen(
    onNavigateUp: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: ReminderDetailViewModel = viewModel(factory = ReminderDetailViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val reminder = state.item?.reminder

    LaunchedEffect(state.deleted) { if (state.deleted) onNavigateUp() }

    Scaffold(
        topBar = {
            ParentsAppTopBar(
                title = "Reminder",
                onNavigateUp = onNavigateUp,
                actions = {
                    if (reminder != null) {
                        IconButton(onClick = { onEdit(reminder.id) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> LoadingState(Modifier.padding(padding))
            reminder == null -> EmptyHint("This reminder is no longer available.")
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text(reminder.title, style = MaterialTheme.typography.headlineSmall)
                StatusBadge(reminder.status.label, reminder.status.tone())

                SectionHeader("Details")
                InfoRow("Child", state.item?.childName ?: "—")
                InfoRow("Category", reminder.category.label)
                InfoRow("Event time", DateTimeFormatters.formatDateTime(reminder.reminderDateTime))
                InfoRow("Alert", ReminderLeadTime.fromMinutes(reminder.leadTimeMinutes).label)
                InfoRow("Priority", reminder.priority.label)
                InfoRow("Notification", if (reminder.notificationEnabled) "On" else "Off")

                if (state.canManage && reminder.status !in setOf(ReminderStatus.COMPLETED, ReminderStatus.DISMISSED)) {
                    SectionHeader("Actions")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::complete) { Text("Mark done") }
                        OutlinedButton(onClick = { viewModel.snooze(60) }) { Text("Snooze 1h") }
                        OutlinedButton(onClick = viewModel::dismiss) { Text("Dismiss") }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete reminder?",
            message = "This also cancels its scheduled alert.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { confirmDelete = false; viewModel.delete() },
            onDismiss = { confirmDelete = false },
        )
    }
}
