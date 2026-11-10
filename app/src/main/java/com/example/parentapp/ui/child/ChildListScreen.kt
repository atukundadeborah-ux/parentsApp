package com.example.parentapp.ui.child

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parentapp.core.designsystem.component.EmptyState
import com.example.parentapp.core.designsystem.component.LoadingState
import com.example.parentapp.core.designsystem.component.StatusBadge
import com.example.parentapp.core.designsystem.component.BadgeTone
import com.example.parentapp.core.util.DateTimeFormatters
import com.example.parentapp.data.local.entity.ChildProfile
import com.example.parentapp.domain.model.ProfileStatus
import com.example.parentapp.ui.shell.AppShell
import com.example.parentapp.ui.shell.TopLevelDestination

@Composable
fun ChildListScreen(
    onSelectTab: (TopLevelDestination) -> Unit,
    onOpenChild: (Long) -> Unit,
    onAddChild: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: ChildListViewModel = viewModel(factory = ChildListViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AppShell(
        selected = TopLevelDestination.CHILDREN,
        role = state.role,
        onSelect = onSelectTab,
        title = "Children",
        onSignOut = onSignOut,
        actions = {
            IconButton(onClick = viewModel::toggleArchived) {
                Icon(
                    Icons.Filled.ChildCare,
                    contentDescription = if (state.showArchived) "Hide archived" else "Show archived",
                    tint = if (state.showArchived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        floatingActionButton = {
            if (state.canAdd) {
                ExtendedFloatingActionButton(
                    text = { Text("Add child") },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    onClick = onAddChild,
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Search by name, guardian or school") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            when {
                state.loading -> LoadingState()
                state.children.isEmpty() -> EmptyState(
                    icon = Icons.Filled.ChildCare,
                    title = if (state.query.isBlank()) "No children yet" else "No matches",
                    description = if (state.query.isBlank()) {
                        "Add a child profile to start tracking activities, reminders and tasks."
                    } else {
                        "Try a different search term."
                    },
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.children, key = { it.id }) { child ->
                        ChildCard(child = child, onClick = { onOpenChild(child.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ChildCard(child: ChildProfile, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = child.fullName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(child.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val age = DateTimeFormatters.ageInYears(child.dateOfBirth)
                Text(
                    buildString {
                        if (age != null) append("$age yr · ")
                        append(child.gender.ifBlank { "—" })
                        if (child.schoolName.isNotBlank()) append(" · ${child.schoolName}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (child.assignedCaregiver.isNotBlank()) {
                    Text(
                        "Caregiver: ${child.assignedCaregiver}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (child.profileStatus == ProfileStatus.ARCHIVED) {
                StatusBadge("Archived", BadgeTone.NEUTRAL)
            }
        }
    }
}
