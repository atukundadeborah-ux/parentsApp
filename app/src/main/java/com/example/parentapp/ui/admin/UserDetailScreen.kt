package com.example.parentapp.ui.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parentapp.core.designsystem.component.InfoRow
import com.example.parentapp.core.designsystem.component.LoadingState
import com.example.parentapp.core.designsystem.component.ParentsAppDropdownField
import com.example.parentapp.core.designsystem.component.ParentsAppTopBar
import com.example.parentapp.core.designsystem.component.PrimaryButton
import com.example.parentapp.core.designsystem.component.SectionHeader
import com.example.parentapp.data.audit.AuditLogger
import com.example.parentapp.data.local.entity.UserAccount
import com.example.parentapp.data.repository.UserRepository
import com.example.parentapp.di.appContainer
import com.example.parentapp.domain.model.AccountStatus
import com.example.parentapp.domain.model.AuditAction
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.ui.common.EmptyHint
import com.example.parentapp.utils.session.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserDetailUiState(
    val loading: Boolean = true,
    val user: UserAccount? = null,
    val isSelf: Boolean = false,
    val message: String? = null,
)

class UserDetailViewModel(
    private val userRepository: UserRepository,
    private val userSession: UserSession,
    private val auditLogger: AuditLogger,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val userId: Long = savedStateHandle.get<Long>("userId") ?: -1L
    private val _uiState = MutableStateFlow(UserDetailUiState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    private fun refresh() = viewModelScope.launch {
        val user = userRepository.getUserById(userId)
        val me = userSession.userId.first()
        _uiState.update { it.copy(loading = false, user = user, isSelf = user?.id == me) }
    }

    fun setStatus(status: AccountStatus) = act { user ->
        userRepository.setStatus(user.id, status)
        audit(AuditAction.STATUS_CHANGE, "Set ${user.fullName} to ${status.label}")
        "Account ${status.label.lowercase()}"
    }

    fun setRole(role: UserRole) = act { user ->
        userRepository.setRole(user.id, role)
        audit(AuditAction.UPDATE, "Changed ${user.fullName}'s role to ${role.label}")
        "Role updated to ${role.label}"
    }

    private fun act(block: suspend (UserAccount) -> String) {
        val user = _uiState.value.user ?: return
        viewModelScope.launch {
            val msg = block(user)
            refresh()
            _uiState.update { it.copy(message = msg) }
        }
    }

    private suspend fun audit(action: AuditAction, description: String) {
        val actorId = userSession.userId.first() ?: 0L
        val actorName = userSession.userName.first().orEmpty()
        auditLogger.record(actorId, actorName, action, "UserAccount", userId, description)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this.appContainer
                UserDetailViewModel(c.userRepository, c.userSession, c.auditLogger, createSavedStateHandle())
            }
        }
    }
}

@Composable
fun UserDetailScreen(
    onNavigateUp: () -> Unit,
    viewModel: UserDetailViewModel = viewModel(factory = UserDetailViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val user = state.user

    Scaffold(
        topBar = { ParentsAppTopBar(title = user?.fullName ?: "User", onNavigateUp = onNavigateUp) },
    ) { padding ->
        when {
            state.loading -> LoadingState(Modifier.padding(padding))
            user == null -> EmptyHint("This account no longer exists.")
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InfoRow("Email", user.email)
                InfoRow("Phone", user.phone)
                InfoRow("Category", user.userCategory)
                InfoRow("Status", user.status.label)

                if (state.isSelf) {
                    Text(
                        "You cannot change your own role or status.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    SectionHeader("Role")
                    ParentsAppDropdownField(
                        label = "Role",
                        selected = user.role,
                        options = UserRole.entries,
                        optionLabel = { it.label },
                        onSelected = viewModel::setRole,
                    )
                    SectionHeader("Account status")
                    if (user.status == AccountStatus.ACTIVE) {
                        OutlinedButton(
                            onClick = { viewModel.setStatus(AccountStatus.SUSPENDED) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Suspend account", color = MaterialTheme.colorScheme.error) }
                    } else {
                        PrimaryButton(
                            "Reactivate account",
                            onClick = { viewModel.setStatus(AccountStatus.ACTIVE) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                state.message?.let {
                    Spacer(Modifier.size(4.dp))
                    Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
