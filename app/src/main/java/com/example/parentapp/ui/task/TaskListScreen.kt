package com.example.parentapp.ui.task

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parentapp.core.designsystem.component.EmptyState
import com.example.parentapp.core.designsystem.component.FilterChipRow
import com.example.parentapp.core.designsystem.component.LoadingState
import com.example.parentapp.ui.common.TaskRow
import com.example.parentapp.ui.shell.AppShell
import com.example.parentapp.ui.shell.TopLevelDestination

private fun scopeLabel(s: TaskScope) = when (s) {
    TaskScope.TO_ME -> "Assigned to me"
    TaskScope.BY_ME -> "Assigned by me"
    TaskScope.ALL -> "All tasks"
}

private fun filterLabel(f: TaskFilter) = when (f) {
    TaskFilter.OPEN -> "Open"
    TaskFilter.DONE -> "Completed"
    TaskFilter.ALL -> "All"
}

@Composable
fun TaskListScreen(
    onSelectTab: (TopLevelDestination) -> Unit,
    onOpenTask: (Long) -> Unit,
    onAssignTask: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: TaskListViewModel = viewModel(factory = TaskListViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AppShell(
        selected = TopLevelDestination.TASKS,
        role = state.role,
        onSelect = onSelectTab,
        title = "Caregiver tasks",
        onSignOut = onSignOut,
        floatingActionButton = {
            if (state.canAssign) {
                ExtendedFloatingActionButton(
                    text = { Text("Assign") },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    onClick = onAssignTask,
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.availableScopes.size > 1) {
                SecondaryTabRow(selectedTabIndex = state.availableScopes.indexOf(state.scope).coerceAtLeast(0)) {
                    state.availableScopes.forEach { s ->
                        Tab(
                            selected = state.scope == s,
                            onClick = { viewModel.onScope(s) },
                            text = { Text(scopeLabel(s)) },
                        )
                    }
                }
            }
            FilterChipRow(
                options = listOf(TaskFilter.OPEN, TaskFilter.DONE),
                selected = state.filter.takeIf { it != TaskFilter.ALL },
                optionLabel = { filterLabel(it) },
                onSelected = { viewModel.onFilter(it ?: TaskFilter.ALL) },
                allLabel = "All",
                modifier = Modifier.padding(vertical = 8.dp),
            )
            when {
                state.loading -> LoadingState()
                state.items.isEmpty() -> EmptyState(
                    icon = Icons.Filled.TaskAlt,
                    title = "No tasks here",
                    description = "Assigned caregiver tasks and their status appear on this screen.",
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    items(state.items, key = { it.task.id }) { item ->
                        TaskRow(item, showChild = true) { onOpenTask(item.task.id) }
                    }
                }
            }
        }
    }
}
