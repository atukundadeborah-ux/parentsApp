package com.example.parentapp.ui.tips

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parentapp.core.designsystem.component.EmptyState
import com.example.parentapp.core.designsystem.component.FilterChipRow
import com.example.parentapp.core.designsystem.component.LoadingState
import com.example.parentapp.core.designsystem.component.StatusBadge
import com.example.parentapp.core.designsystem.component.BadgeTone
import com.example.parentapp.domain.model.TipCategory
import com.example.parentapp.ui.shell.AppShell
import com.example.parentapp.ui.shell.TopLevelDestination

@Composable
fun TipsScreen(
    onSelectTab: (TopLevelDestination) -> Unit,
    onOpenTip: (Long) -> Unit,
    onNavigateUp: (() -> Unit)?,
    onSignOut: () -> Unit,
    viewModel: TipsViewModel = viewModel(factory = TipsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AppShell(
        selected = TopLevelDestination.TIPS,
        role = state.role,
        onSelect = onSelectTab,
        title = "Parenting & caregiver tips",
        onNavigateUp = onNavigateUp,
        onSignOut = onSignOut,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQuery,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Search tips") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            FilterChipRow(
                options = TipCategory.entries,
                selected = state.category,
                optionLabel = { it.label },
                onSelected = viewModel::onCategory,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            when {
                state.loading -> LoadingState()
                state.tips.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.Lightbulb,
                    title = "No tips found",
                    description = "Try another category or search term.",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                ) {
                    items(state.tips, key = { it.id }) { tip ->
                        ElevatedCard(onClick = { onOpenTip(tip.id) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                StatusBadge(tip.category.label, BadgeTone.INFO)
                                Text(
                                    tip.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                Text(
                                    tip.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
