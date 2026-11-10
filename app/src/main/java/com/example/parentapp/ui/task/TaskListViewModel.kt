package com.example.parentapp.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parentapp.data.local.model.TaskWithNames
import com.example.parentapp.data.repository.CaregiverTaskRepository
import com.example.parentapp.di.appContainer
import com.example.parentapp.domain.model.TaskStatus
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.utils.session.UserSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TaskScope { TO_ME, BY_ME, ALL }
enum class TaskFilter { OPEN, DONE, ALL }

data class TaskListUiState(
    val loading: Boolean = true,
    val role: UserRole? = null,
    val canAssign: Boolean = false,
    val availableScopes: List<TaskScope> = emptyList(),
    val scope: TaskScope = TaskScope.TO_ME,
    val filter: TaskFilter = TaskFilter.OPEN,
    val items: List<TaskWithNames> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModel(
    private val taskRepository: CaregiverTaskRepository,
    userSession: UserSession,
) : ViewModel() {

    private val scope = MutableStateFlow(TaskScope.TO_ME)
    private val filter = MutableStateFlow(TaskFilter.OPEN)

    private val principal = combine(
        userSession.userId, userSession.userRole, userSession.institutionId,
    ) { id, role, inst -> Triple(id, role, inst) }

    init {
        viewModelScope.launch { taskRepository.markPastPendingTasksMissed() }
    }

    val uiState: StateFlow<TaskListUiState> = principal.flatMapLatest { (id, role, inst) ->
        val scopes = when (role) {
            UserRole.CAREGIVER -> listOf(TaskScope.TO_ME)
            UserRole.PARENT_GUARDIAN -> listOf(TaskScope.BY_ME)
            UserRole.INSTITUTIONAL_USER -> listOf(TaskScope.BY_ME, TaskScope.ALL)
            UserRole.ADMIN -> listOf(TaskScope.ALL)
            null -> listOf(TaskScope.ALL)
        }
        if (scope.value !in scopes) scope.value = scopes.first()

        combine(scope, filter) { sc, fl -> sc to fl }.flatMapLatest { (sc, fl) ->
            val source = when (sc) {
                TaskScope.TO_ME -> taskRepository.getTasksAssignedToCaregiver(id ?: -1L)
                TaskScope.BY_ME -> taskRepository.getTasksAssignedByUser(id ?: -1L)
                TaskScope.ALL -> taskRepository.getTasksVisibleToUser(role, id, inst)
            }
            source.map { list ->
                TaskListUiState(
                    loading = false,
                    role = role,
                    canAssign = role != UserRole.CAREGIVER,
                    availableScopes = scopes,
                    scope = sc,
                    filter = fl,
                    items = list
                        .filter { t ->
                            when (fl) {
                                TaskFilter.OPEN -> t.task.status == TaskStatus.PENDING || t.task.status == TaskStatus.REQUIRES_FOLLOW_UP
                                TaskFilter.DONE -> t.task.status == TaskStatus.COMPLETED
                                TaskFilter.ALL -> true
                            }
                        }
                        .sortedBy { it.task.dueDateTime },
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskListUiState())

    fun onScope(value: TaskScope) { scope.value = value }
    fun onFilter(value: TaskFilter) { filter.value = value }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this.appContainer
                TaskListViewModel(c.caregiverTaskRepository, c.userSession)
            }
        }
    }
}
