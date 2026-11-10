package com.example.parentapp.ui.child

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parentapp.data.local.entity.ChildProfile
import com.example.parentapp.data.repository.ChildRepository
import com.example.parentapp.di.appContainer
import com.example.parentapp.domain.model.ProfileStatus
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.utils.session.UserSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class ChildListUiState(
    val loading: Boolean = true,
    val query: String = "",
    val showArchived: Boolean = false,
    val canAdd: Boolean = false,
    val role: UserRole? = null,
    val children: List<ChildProfile> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class ChildListViewModel(
    private val childRepository: ChildRepository,
    userSession: UserSession,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val showArchived = MutableStateFlow(false)

    private val principal = combine(
        userSession.userId, userSession.userRole, userSession.institutionId,
    ) { id, role, inst -> Triple(id, role, inst) }

    val uiState: StateFlow<ChildListUiState> = principal
        .flatMapLatest { (id, role, inst) ->
            combine(
                childRepository.getChildrenVisibleToUser(role, id, inst),
                query,
                showArchived,
            ) { list, currentQuery, showArch ->
                ChildListUiState(
                    loading = false,
                    query = currentQuery,
                    showArchived = showArch,
                    canAdd = role == UserRole.PARENT_GUARDIAN || role == UserRole.INSTITUTIONAL_USER || role == UserRole.ADMIN,
                    role = role,
                    children = list
                        .filter { showArch || it.profileStatus == ProfileStatus.ACTIVE }
                        .filter { c ->
                            currentQuery.isBlank() ||
                                c.fullName.contains(currentQuery, true) ||
                                c.guardianName.contains(currentQuery, true) ||
                                c.schoolName.contains(currentQuery, true)
                        }
                        .sortedBy { it.fullName },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChildListUiState())

    fun onQueryChange(value: String) { query.value = value }
    fun toggleArchived() { showArchived.value = !showArchived.value }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this.appContainer
                ChildListViewModel(c.childRepository, c.userSession)
            }
        }
    }
}
