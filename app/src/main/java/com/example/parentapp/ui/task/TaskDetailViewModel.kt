package com.example.parentapp.ui.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parentapp.data.audit.AuditLogger
import com.example.parentapp.data.local.model.TaskWithNames
import com.example.parentapp.data.repository.CaregiverTaskRepository
import com.example.parentapp.di.appContainer
import com.example.parentapp.domain.model.TaskStatus
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.utils.session.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskDetailUiState(
    val loading: Boolean = true,
    val item: TaskWithNames? = null,
    val isAssignee: Boolean = false,
    val isAssigner: Boolean = false,
    val deleted: Boolean = false,
)

class TaskDetailViewModel(
    private val taskRepository: CaregiverTaskRepository,
    private val userSession: UserSession,
    private val auditLogger: AuditLogger,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val taskId: Long = savedStateHandle.get<Long>("taskId") ?: -1L
    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    private fun refresh() = viewModelScope.launch {
        val item = taskRepository.getTaskWithNamesById(taskId)
        val uid = userSession.userId.first()
        val role = userSession.userRole.first()
        _uiState.update {
            it.copy(
                loading = false,
                item = item,
                isAssignee = item?.task?.assignedToCaregiverId == uid,
                isAssigner = item?.task?.assignedByUserId == uid || role == UserRole.ADMIN,
            )
        }
    }

    fun setStatus(status: TaskStatus, notes: String?) {
        val task = _uiState.value.item?.task ?: return
        viewModelScope.launch {
            val completedAt = if (status == TaskStatus.COMPLETED) System.currentTimeMillis() else null
            taskRepository.updateStatus(task.id, status, notes?.ifBlank { null }, completedAt)
            val actorId = userSession.userId.first() ?: 0L
            val actorName = userSession.userName.first().orEmpty()
            auditLogger.updated(actorId, actorName, "CaregiverTask", task.id, "Set '${task.title}' to ${status.label}")
            refresh()
        }
    }

    fun delete() {
        val task = _uiState.value.item?.task ?: return
        viewModelScope.launch {
            taskRepository.deleteTask(task)
            val actorId = userSession.userId.first() ?: 0L
            val actorName = userSession.userName.first().orEmpty()
            auditLogger.deleted(actorId, actorName, "CaregiverTask", task.id, "Deleted task '${task.title}'")
            _uiState.update { it.copy(deleted = true) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this.appContainer
                TaskDetailViewModel(c.caregiverTaskRepository, c.userSession, c.auditLogger, createSavedStateHandle())
            }
        }
    }
}
