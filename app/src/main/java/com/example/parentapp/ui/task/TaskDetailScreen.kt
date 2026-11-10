package com.example.parentapp.ui.task

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
import androidx.compose.material3.OutlinedTextField
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
import com.example.parentapp.domain.model.TaskStatus
import com.example.parentapp.ui.common.EmptyHint
import com.example.parentapp.ui.common.tone

@Composable
fun TaskDetailScreen(
    onNavigateUp: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: TaskDetailViewModel = viewModel(factory = TaskDetailViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    var followUpNotes by remember { mutableStateOf("") }
    val task = state.item?.task

    LaunchedEffect(task?.followUpNotes) { followUpNotes = task?.followUpNotes.orEmpty() }
    LaunchedEffect(state.deleted) { if (state.deleted) onNavigateUp() }

    Scaffold(
        topBar = {
            ParentsAppTopBar(
                title = "Task",
                onNavigateUp = onNavigateUp,
                actions = {
                    if (state.isAssigner && task != null) {
                        IconButton(onClick = { onEdit(task.id) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
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
            task == null -> EmptyHint("This task is no longer available.")
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text(task.title, style = MaterialTheme.typography.headlineSmall)
                StatusBadge(task.status.label, task.status.tone())

                SectionHeader("Details")
                InfoRow("Child", state.item?.childName ?: "—")
                InfoRow("Caregiver", state.item?.caregiverName ?: "—")
                InfoRow("Assigned by", state.item?.assignedByName ?: "—")
                InfoRow("Due", DateTimeFormatters.formatDateTime(task.dueDateTime))
                if (task.description.isNotBlank()) {
                    SectionHeader("Instructions")
                    Text(task.description, style = MaterialTheme.typography.bodyMedium)
                }

                val canUpdate = state.isAssignee || state.isAssigner
                if (canUpdate && task.status != TaskStatus.COMPLETED) {
                    SectionHeader("Update status")
                    OutlinedTextField(
                        value = followUpNotes,
                        onValueChange = { followUpNotes = it },
                        label = { Text("Follow-up notes (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Button(onClick = { viewModel.setStatus(TaskStatus.COMPLETED, followUpNotes) }) { Text("Complete") }
                        OutlinedButton(onClick = { viewModel.setStatus(TaskStatus.REQUIRES_FOLLOW_UP, followUpNotes) }) {
                            Text("Needs follow-up")
                        }
                        OutlinedButton(onClick = { viewModel.setStatus(TaskStatus.MISSED, followUpNotes) }) { Text("Missed") }
                    }
                } else if (!task.followUpNotes.isNullOrBlank()) {
                    SectionHeader("Follow-up notes")
                    Text(task.followUpNotes!!, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete task?",
            message = "This removes the task for the caregiver too.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { confirmDelete = false; viewModel.delete() },
            onDismiss = { confirmDelete = false },
        )
    }
}
