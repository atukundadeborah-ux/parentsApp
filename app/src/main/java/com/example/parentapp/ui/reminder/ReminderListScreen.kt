package com.example.parentapp.ui.reminder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parentapp.core.designsystem.component.EmptyState
import com.example.parentapp.core.designsystem.component.LoadingState
import com.example.parentapp.core.designsystem.component.ParentsAppTopBar
import com.example.parentapp.core.designsystem.component.SignOutAction
import com.example.parentapp.ui.common.ReminderRow
import com.example.parentapp.ui.shell.AppShell
import com.example.parentapp.ui.shell.TopLevelDestination

private val tabLabels = mapOf(
    ReminderTab.UPCOMING to "Upcoming",
    ReminderTab.MISSED to "Missed",
    ReminderTab.ALL to "All",
)

@Composable
fun ReminderListScreen(
    onSelectTab: (TopLevelDestination) -> Unit,
    onNavigateUp: (() -> Unit)?,
    onSignOut: () -> Unit,
    onOpenReminder: (Long) -> Unit,
    onAddReminder: () -> Unit,
    viewModel: ReminderListViewModel = viewModel(factory = ReminderListViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val body: @Composable (PaddingValues) -> Unit = { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = state.tab.ordinal) {
                ReminderTab.entries.forEach { t ->
                    Tab(
                        selected = state.tab == t,
                        onClick = { viewModel.onTab(t) },
                        text = { Text(tabLabels.getValue(t)) },
                    )
                }
            }
            when {
                state.loading -> LoadingState()
                state.items.isEmpty() -> EmptyState(
                    icon = Icons.Filled.NotificationsNone,
                    title = when (state.tab) {
                        ReminderTab.UPCOMING -> "Nothing scheduled"
                        ReminderTab.MISSED -> "No missed reminders"
                        ReminderTab.ALL -> "No reminders yet"
                    },
                    description = "Create reminders for medication, appointments, feeding and routines.",
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    items(state.items, key = { it.reminder.id }) { item ->
                        ReminderRow(item, showChild = !state.scopedToChild) { onOpenReminder(item.reminder.id) }
                    }
                }
            }
        }
    }

    if (state.scopedToChild) {
        Scaffold(
            topBar = {
                ParentsAppTopBar(
                    title = "Reminders",
                    onNavigateUp = onNavigateUp,
                    actions = { SignOutAction(onSignOut) },
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onAddReminder) { Icon(Icons.Filled.Add, contentDescription = "New reminder") }
            },
            content = body,
        )
    } else {
        AppShell(
            selected = TopLevelDestination.REMINDERS,
            role = state.role,
            onSelect = onSelectTab,
            title = "Reminders",
            onNavigateUp = onNavigateUp,
            onSignOut = onSignOut,
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("New") },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    onClick = onAddReminder,
                )
            },
            content = body,
        )
    }
}
