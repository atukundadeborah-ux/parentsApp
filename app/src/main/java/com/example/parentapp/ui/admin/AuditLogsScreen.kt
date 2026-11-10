package com.example.parentapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
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
import com.example.parentapp.core.util.DateTimeFormatters
import com.example.parentapp.data.local.entity.AuditLog
import com.example.parentapp.data.repository.AuditLogRepository
import com.example.parentapp.di.appContainer
import com.example.parentapp.domain.model.AuditAction
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.ui.shell.AppShell
import com.example.parentapp.ui.shell.TopLevelDestination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AuditLogsUiState(
    val loading: Boolean = true,
    val filter: AuditAction? = null,
    val logs: List<AuditLog> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class AuditLogsViewModel(repository: AuditLogRepository) : ViewModel() {
    private val filter = MutableStateFlow<AuditAction?>(null)

    val uiState: StateFlow<AuditLogsUiState> = combine(
        repository.getRecentLogs(300), filter,
    ) { logs, f ->
        AuditLogsUiState(
            loading = false,
            filter = f,
            logs = logs.filter { f == null || it.actionType == f },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuditLogsUiState())

    fun onFilter(action: AuditAction?) { filter.value = action }

    companion object {
        val Factory = viewModelFactory {
            initializer { AuditLogsViewModel(this.appContainer.auditLogRepository) }
        }
    }
}

private fun AuditAction.tone() = when (this) {
    AuditAction.CREATE -> BadgeTone.SUCCESS
    AuditAction.DELETE -> BadgeTone.DANGER
    AuditAction.UPDATE, AuditAction.STATUS_CHANGE -> BadgeTone.INFO
    AuditAction.LOGIN, AuditAction.LOGOUT -> BadgeTone.NEUTRAL
}

@Composable
fun AuditLogsScreen(
    onSelectTab: (TopLevelDestination) -> Unit,
    onNavigateUp: (() -> Unit)?,
    onSignOut: () -> Unit,
    viewModel: AuditLogsViewModel = viewModel(factory = AuditLogsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AppShell(
        selected = TopLevelDestination.AUDIT,
        role = UserRole.ADMIN,
        onSelect = onSelectTab,
        title = "Audit logs",
        onNavigateUp = onNavigateUp,
        onSignOut = onSignOut,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            FilterChipRow(
                options = AuditAction.entries,
                selected = state.filter,
                optionLabel = { it.label },
                onSelected = viewModel::onFilter,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            when {
                state.loading -> LoadingState()
                state.logs.isEmpty() -> EmptyState(
                    icon = Icons.Filled.Receipt,
                    title = "No activity logged",
                    description = "Create, update and delete actions across the app are recorded here.",
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.logs, key = { it.id }) { log ->
                        ListItem(
                            overlineContent = {
                                Text("${log.actorName.ifBlank { "User #${log.userId}" }} · ${DateTimeFormatters.formatDateTime(log.createdAt)}")
                            },
                            headlineContent = { Text(log.description) },
                            supportingContent = { Text("${log.affectedEntity} #${log.affectedRecordId}") },
                            trailingContent = { StatusBadge(log.actionType.label, log.actionType.tone()) },
                        )
                    }
                }
            }
        }
    }
}
