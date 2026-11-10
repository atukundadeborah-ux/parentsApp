package com.example.parentapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parentapp.data.local.entity.UserAccount
import com.example.parentapp.data.repository.UserRepository
import com.example.parentapp.di.appContainer
import com.example.parentapp.domain.model.UserRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class UserManagementUiState(
    val loading: Boolean = true,
    val roleFilter: UserRole? = null,
    val users: List<UserAccount> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class UserManagementViewModel(
    userRepository: UserRepository,
) : ViewModel() {

    private val roleFilter = MutableStateFlow<UserRole?>(null)

    val uiState: StateFlow<UserManagementUiState> = combine(
        userRepository.getAllUsers(), roleFilter,
    ) { users, filter ->
        UserManagementUiState(
            loading = false,
            roleFilter = filter,
            users = users.filter { filter == null || it.role == filter },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserManagementUiState())

    fun onRoleFilter(role: UserRole?) { roleFilter.value = role }

    companion object {
        val Factory = viewModelFactory {
            initializer { UserManagementViewModel(this.appContainer.userRepository) }
        }
    }
}
