package com.example.parentapp.ui.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import com.example.parentapp.core.designsystem.component.ParentsAppTopBar
import com.example.parentapp.core.designsystem.component.SignOutAction
import com.example.parentapp.domain.model.ActivityCategory
import com.example.parentapp.domain.model.ActivityStatus
import com.example.parentapp.ui.common.ActivityRow
import com.example.parentapp.ui.shell.AppShell
import com.example.parentapp.ui.shell.TopLevelDestination

@Composable
fun ActivityListScreen(
    onSelectTab: (TopLevelDestination) -> Unit,
    onNavigateUp: (() -> Unit)?,
    onSignOut: () -> Unit,
    onOpenActivity: (Long) -> Unit,
    onAddActivity: () -> Unit,
    viewModel: ActivityListViewModel = viewModel(factory = ActivityListViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val body: @Composable (PaddingValues) -> Unit = { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            FilterChipRow(
                options = ActivityCategory.entries,
                selected = state.category,
                optionLabel = { it.label },
                onSelected = viewModel::onCategory,
                modifier = Modifier.padding(top = 8.dp),
            )
            FilterChipRow(
                options = ActivityStatus.entries,
                selected = state.status,
                optionLabel = { it.label },
                onSelected = viewModel::onStatus,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            when {
                state.loading -> LoadingState()
                state.items.isEmpty() -> EmptyState(
                    icon = Icons.Filled.Event,
                    title = "No activities",
                    description = "Record school, health, feeding, medication and play activities to build a history.",
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    items(state.items, key = { it.activity.id }) { item ->
                        ActivityRow(item, showChild = !state.scopedToChild) { onOpenActivity(item.activity.id) }
                    }
                }
            }
        }
    }

    if (state.scopedToChild) {
        Scaffold(
            topBar = {
                ParentsAppTopBar(
                    title = "Activities",
                    onNavigateUp = onNavigateUp,
                    actions = { SignOutAction(onSignOut) },
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onAddActivity) { Icon(Icons.Filled.Add, contentDescription = "Record activity") }
            },
            content = body,
        )
    } else {
        AppShell(
            selected = TopLevelDestination.ACTIVITIES,
            role = state.role,
            onSelect = onSelectTab,
            title = "Activities",
            onNavigateUp = onNavigateUp,
            onSignOut = onSignOut,
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("Record") },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    onClick = onAddActivity,
                )
            },
            content = body,
        )
    }
}
