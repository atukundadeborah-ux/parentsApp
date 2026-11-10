package com.example.parentapp.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parentapp.core.designsystem.component.EmptyState
import com.example.parentapp.core.designsystem.component.LoadingState
import com.example.parentapp.core.designsystem.component.SectionHeader
import com.example.parentapp.core.designsystem.component.StatCard
import com.example.parentapp.core.util.DateTimeFormatters
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.ui.shell.AppShell
import com.example.parentapp.ui.shell.TopLevelDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onSelectTab: (TopLevelDestination) -> Unit,
    onOpenReminder: (Long) -> Unit,
    onOpenTask: (Long) -> Unit,
    onOpenActivity: (Long) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }

    AppShell(
        selected = TopLevelDestination.HOME,
        role = state.role,
        onSelect = onSelectTab,
        title = "Hello${state.userName.takeIf { it.isNotBlank() }?.let { ", ${it.substringBefore(' ')}" } ?: ""}",
        subtitle = state.role?.label,
        onSignOut = onLogout,
        actions = {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More options")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Profile") }, onClick = { menuOpen = false; onOpenProfile() })
                DropdownMenuItem(text = { Text("Settings") }, onClick = { menuOpen = false; onOpenSettings() })
            }
        },
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@AppShell
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        label = if (state.role == UserRole.PARENT_GUARDIAN) "My children" else "Children",
                        value = state.childCount.toString(),
                        icon = Icons.Filled.ChildCare,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectTab(TopLevelDestination.CHILDREN) },
                    )
                    StatCard(
                        label = "Due today",
                        value = state.todayReminderCount.toString(),
                        icon = Icons.Filled.NotificationsActive,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        label = "Missed reminders",
                        value = state.missedReminderCount.toString(),
                        icon = Icons.Filled.ReportProblem,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Open tasks",
                        value = state.pendingTaskCount.toString(),
                        icon = Icons.Filled.TaskAlt,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectTab(TopLevelDestination.TASKS) },
                    )
                }
            }

            item { SectionHeader("Upcoming reminders") }
            if (state.upcomingReminders.isEmpty()) {
                item { MutedRow("Nothing scheduled — you're all caught up.") }
            } else {
                items(state.upcomingReminders, key = { "reminder-${it.reminder.id}" }) { r ->
                    ListItem(
                        headlineContent = { Text(r.reminder.title) },
                        supportingContent = { Text("${r.childName} · ${DateTimeFormatters.formatDateTime(r.reminder.reminderDateTime)}") },
                        trailingContent = { Text(DateTimeFormatters.relative(r.reminder.reminderDateTime)) },
                        modifier = Modifier.clickableRow { onOpenReminder(r.reminder.id) },
                    )
                }
            }

            item { SectionHeader("Open tasks") }
            if (state.openTasks.isEmpty()) {
                item { MutedRow("No tasks need attention right now.") }
            } else {
                items(state.openTasks, key = { "task-${it.task.id}" }) { t ->
                    ListItem(
                        headlineContent = { Text(t.task.title) },
                        supportingContent = { Text("${t.childName} · due ${DateTimeFormatters.formatDateTime(t.task.dueDateTime)}") },
                        modifier = Modifier.clickableRow { onOpenTask(t.task.id) },
                    )
                }
            }

            item { SectionHeader("Recent activity") }
            if (state.recentActivities.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Event,
                        title = "No activity recorded yet",
                        description = "Logged activities for your children will show up here.",
                    )
                }
            } else {
                items(state.recentActivities, key = { "activity-${it.activity.id}" }) { a ->
                    ListItem(
                        headlineContent = { Text(a.activity.title) },
                        supportingContent = { Text("${a.childName} · ${a.activity.category.label} · ${DateTimeFormatters.formatDate(a.activity.activityDateTime)}") },
                        modifier = Modifier.clickableRow { onOpenActivity(a.activity.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MutedRow(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)
