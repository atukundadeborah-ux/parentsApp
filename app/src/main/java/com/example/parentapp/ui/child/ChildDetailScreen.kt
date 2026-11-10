package com.example.parentapp.ui.child

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parentapp.core.designsystem.component.InfoRow
import com.example.parentapp.core.designsystem.component.LoadingState
import com.example.parentapp.core.designsystem.component.ParentsAppTopBar
import com.example.parentapp.core.designsystem.component.SectionHeader
import com.example.parentapp.core.util.DateTimeFormatters
import com.example.parentapp.ui.common.ActivityRow
import com.example.parentapp.ui.common.EmptyHint
import com.example.parentapp.ui.common.ReminderRow
import com.example.parentapp.ui.common.TaskRow

private val tabs = listOf("Overview", "Activities", "Reminders", "Tasks")

@Composable
fun ChildDetailScreen(
    onNavigateUp: () -> Unit,
    onEdit: (Long) -> Unit,
    onAddActivity: (Long) -> Unit,
    onOpenActivity: (Long) -> Unit,
    onAddReminder: (Long) -> Unit,
    onOpenReminder: (Long) -> Unit,
    onAddTask: (Long) -> Unit,
    onOpenTask: (Long) -> Unit,
    viewModel: ChildDetailViewModel = viewModel(factory = ChildDetailViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val child = state.child

    Scaffold(
        topBar = {
            ParentsAppTopBar(
                title = child?.fullName ?: "Child profile",
                onNavigateUp = onNavigateUp,
                actions = {
                    if (state.canEdit && child != null) {
                        IconButton(onClick = { onEdit(child.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> LoadingState(Modifier.padding(padding))
            child == null -> EmptyHint("This child profile is no longer available.")
            else -> Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                TabRow(selectedTabIndex = tab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
                    }
                }
                when (tab) {
                    0 -> OverviewTab(state)
                    1 -> ListTab(
                        header = "Activities",
                        onAdd = { onAddActivity(child.id) },
                        empty = "No activities recorded for ${child.fullName}.",
                        isEmpty = state.activities.isEmpty(),
                    ) {
                        items(state.activities, key = { it.activity.id }) {
                            ActivityRow(it, showChild = false) { onOpenActivity(it.activity.id) }
                        }
                    }
                    2 -> ListTab(
                        header = "Reminders",
                        onAdd = { onAddReminder(child.id) },
                        empty = "No reminders for ${child.fullName}.",
                        isEmpty = state.reminders.isEmpty(),
                    ) {
                        items(state.reminders, key = { it.reminder.id }) {
                            ReminderRow(it, showChild = false) { onOpenReminder(it.reminder.id) }
                        }
                    }
                    3 -> ListTab(
                        header = "Caregiver tasks",
                        onAdd = { onAddTask(child.id) },
                        empty = "No tasks assigned for ${child.fullName}.",
                        isEmpty = state.tasks.isEmpty(),
                    ) {
                        items(state.tasks, key = { it.task.id }) {
                            TaskRow(it, showChild = false) { onOpenTask(it.task.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(state: ChildDetailUiState) {
    val child = state.child ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        val age = DateTimeFormatters.ageInYears(child.dateOfBirth)
        InfoRow("Date of birth", child.dateOfBirth + (age?.let { "  ($it yr)" } ?: ""))
        InfoRow("Gender", child.gender)
        InfoRow("Guardian", child.guardianName)
        InfoRow("Guardian contact", child.guardianContact)
        InfoRow("Household", child.householdInstitution)
        InfoRow("School / daycare", child.schoolName)
        InfoRow("Assigned caregiver", child.assignedCaregiver)
        InfoRow("Status", child.profileStatus.label)

        SectionHeader("Medical notes")
        Text(
            child.medicalNotes.ifBlank { "None recorded." },
            style = MaterialTheme.typography.bodyMedium,
        )
        SectionHeader("Care notes")
        Text(
            child.careNotes.ifBlank { "None recorded." },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ListTab(
    header: String,
    onAdd: () -> Unit,
    empty: String,
    isEmpty: Boolean,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            SectionHeader(
                title = header,
                action = {
                    IconButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "Add") }
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        if (isEmpty) {
            item { EmptyHint(empty) }
        } else {
            content()
        }
    }
}
