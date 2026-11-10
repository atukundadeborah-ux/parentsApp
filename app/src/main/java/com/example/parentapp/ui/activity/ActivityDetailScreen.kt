package com.example.parentapp.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.parentapp.domain.model.ActivityStatus
import com.example.parentapp.ui.common.EmptyHint
import com.example.parentapp.ui.common.tone

@Composable
fun ActivityDetailScreen(
    onNavigateUp: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: ActivityDetailViewModel = viewModel(factory = ActivityDetailViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val activity = state.item?.activity

    LaunchedEffect(state.deleted) { if (state.deleted) onNavigateUp() }

    Scaffold(
        topBar = {
            ParentsAppTopBar(
                title = "Activity",
                onNavigateUp = onNavigateUp,
                actions = {
                    if (state.canEdit && activity != null) {
                        IconButton(onClick = { onEdit(activity.id) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
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
            activity == null -> EmptyHint("This activity is no longer available.")
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text(activity.title, style = MaterialTheme.typography.headlineSmall)
                StatusBadge(activity.status.label, activity.status.tone())

                SectionHeader("Details")
                InfoRow("Child", state.item?.childName ?: "—")
                InfoRow("Category", activity.category.label)
                InfoRow("When", DateTimeFormatters.formatDateTime(activity.activityDateTime))
                if (activity.description.isNotBlank()) {
                    SectionHeader("Notes")
                    Text(activity.description, style = MaterialTheme.typography.bodyMedium)
                }

                if (state.canEdit) {
                    SectionHeader("Change status")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActivityStatus.entries.forEach { status ->
                            AssistChip(
                                onClick = { viewModel.setStatus(status) },
                                label = { Text(status.label) },
                                enabled = status != activity.status,
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete activity?",
            message = "This removes the record permanently.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { confirmDelete = false; viewModel.delete() },
            onDismiss = { confirmDelete = false },
        )
    }
}
