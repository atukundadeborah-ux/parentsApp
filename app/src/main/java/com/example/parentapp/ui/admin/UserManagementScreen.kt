package com.example.parentapp.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parentapp.core.designsystem.component.BadgeTone
import com.example.parentapp.core.designsystem.component.EmptyState
import com.example.parentapp.core.designsystem.component.FilterChipRow
import com.example.parentapp.core.designsystem.component.LoadingState
import com.example.parentapp.core.designsystem.component.StatusBadge
import com.example.parentapp.domain.model.AccountStatus
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.ui.shell.AppShell
import com.example.parentapp.ui.shell.TopLevelDestination

@Composable
fun UserManagementScreen(
    onSelectTab: (TopLevelDestination) -> Unit,
    onOpenUser: (Long) -> Unit,
    onNavigateUp: (() -> Unit)?,
    onSignOut: () -> Unit,
    viewModel: UserManagementViewModel = viewModel(factory = UserManagementViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AppShell(
        selected = TopLevelDestination.USERS,
        role = UserRole.ADMIN,
        onSelect = onSelectTab,
        title = "User management",
        onNavigateUp = onNavigateUp,
        onSignOut = onSignOut,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            FilterChipRow(
                options = UserRole.entries,
                selected = state.roleFilter,
                optionLabel = { it.label },
                onSelected = viewModel::onRoleFilter,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            when {
                state.loading -> LoadingState()
                state.users.isEmpty() -> EmptyState(
                    icon = Icons.Filled.Group,
                    title = "No users",
                    description = "No accounts match this filter.",
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.users, key = { it.id }) { user ->
                        ListItem(
                            headlineContent = { Text(user.fullName) },
                            supportingContent = { Text("${user.email} · ${user.role.label}") },
                            trailingContent = {
                                StatusBadge(
                                    user.status.label,
                                    if (user.status == AccountStatus.ACTIVE) BadgeTone.SUCCESS else BadgeTone.DANGER,
                                )
                            },
                            modifier = Modifier.clickable { onOpenUser(user.id) },
                        )
                    }
                }
            }
        }
    }
}
