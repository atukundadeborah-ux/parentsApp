package com.example.parentapp.ui.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parentapp.core.security.AccessPolicy
import com.example.parentapp.core.util.Validators
import com.example.parentapp.data.audit.AuditLogger
import com.example.parentapp.data.local.entity.CaregiverTask
import com.example.parentapp.data.local.entity.ChildProfile
import com.example.parentapp.data.local.entity.UserAccount
import com.example.parentapp.data.repository.CaregiverTaskRepository
import com.example.parentapp.data.repository.ChildRepository
import com.example.parentapp.data.repository.UserRepository
import com.example.parentapp.di.appContainer
import com.example.parentapp.domain.model.TaskStatus
import com.example.parentapp.utils.session.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class TaskEditorUiState(
    val editing: Boolean = false,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val children: List<ChildProfile> = emptyList(),
    val caregivers: List<UserAccount> = emptyList(),
    val childId: Long? = null,
    val caregiverId: Long? = null,
    val title: String = "",
    val description: String = "",
    val dueMillis: Long = defaultDue(),
    val errors: Map<String, String> = emptyMap(),
    val done: Boolean = false,
    /** Set when the signed-in role may not assign tasks (D-03). */
    val formError: String? = null,
) {
    companion object {
        fun defaultDue(): Long = com.example.parentapp.core.util.DateTimeFormatters
            .localDateTimeToMillis(LocalDateTime.now().plusHours(3).withMinute(0).withSecond(0).withNano(0))
    }
}

class TaskEditorViewModel(
    private val taskRepository: CaregiverTaskRepository,
    private val childRepository: ChildRepository,
    private val userRepository: UserRepository,
    private val userSession: UserSession,
    private val auditLogger: AuditLogger,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val presetChildId: Long = savedStateHandle.get<Long>("childId") ?: -1L
    private val taskId: Long = savedStateHandle.get<Long>("taskId") ?: -1L

    private val _uiState = MutableStateFlow(TaskEditorUiState(editing = taskId > 0))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val id = userSession.userId.first()
            val role = userSession.userRole.first()
            val inst = userSession.institutionId.first()
            val children = childRepository.getChildrenVisibleToUser(role, id, inst).first()
            val caregivers = userRepository.getActiveCaregivers().first()
            val existing = taskId.takeIf { it > 0 }?.let { taskRepository.getTaskById(it) }
            _uiState.update { s ->
                s.copy(
                    loading = false,
                    children = children,
                    caregivers = caregivers,
                    childId = existing?.childId ?: presetChildId.takeIf { it > 0 } ?: children.firstOrNull()?.id,
                    caregiverId = existing?.assignedToCaregiverId ?: caregivers.firstOrNull()?.id,
                    title = existing?.title ?: "",
                    description = existing?.description ?: "",
                    dueMillis = existing?.dueDateTime ?: s.dueMillis,
                )
            }
        }
    }

    fun onChild(id: Long) = _uiState.update { it.copy(childId = id, errors = it.errors - "child") }
    fun onCaregiver(id: Long) = _uiState.update { it.copy(caregiverId = id, errors = it.errors - "caregiver") }
    fun onTitle(v: String) = _uiState.update { it.copy(title = v, errors = it.errors - "title") }
    fun onDescription(v: String) = _uiState.update { it.copy(description = v) }
    fun onDate(v: Long) = _uiState.update { it.copy(dueMillis = merge(it.dueMillis, v)) }
    fun onTime(v: Long) = _uiState.update { it.copy(dueMillis = v) }

    fun save() {
        val s = _uiState.value
        val errors = buildMap {
            if (s.childId == null) put("child", "Select a child")
            if (s.caregiverId == null) put("caregiver", "Select a caregiver")
            Validators.requiredError(s.title, "Task title")?.let { put("title", it) }
        }
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            if (!AccessPolicy.canAssignTasks(userSession.userRole.first())) {
                _uiState.update { it.copy(saving = false, formError = AccessPolicy.DENIED_MESSAGE) }
                return@launch
            }
            val actorId = userSession.userId.first() ?: 0L
            val actorName = userSession.userName.first().orEmpty()
            val existing = taskId.takeIf { it > 0 }?.let { taskRepository.getTaskById(it) }
            val task = (existing ?: CaregiverTask(
                childId = s.childId!!, assignedToCaregiverId = s.caregiverId!!, assignedByUserId = actorId,
                title = "", description = "", dueDateTime = s.dueMillis, status = TaskStatus.PENDING,
            )).copy(
                childId = s.childId!!,
                assignedToCaregiverId = s.caregiverId!!,
                title = s.title.trim(),
                description = s.description.trim(),
                dueDateTime = s.dueMillis,
                updatedAt = System.currentTimeMillis(),
            )
            if (existing != null) {
                taskRepository.updateTask(task)
                auditLogger.updated(actorId, actorName, "CaregiverTask", task.id, "Updated task '${task.title}'")
            } else {
                val newId = taskRepository.insertTask(task)
                val caregiverName = s.caregivers.firstOrNull { it.id == s.caregiverId }?.fullName ?: "caregiver"
                auditLogger.created(actorId, actorName, "CaregiverTask", newId, "Assigned '${task.title}' to $caregiverName")
            }
            _uiState.update { it.copy(saving = false, done = true) }
        }
    }

    private fun merge(current: Long, dateFrom: Long): Long {
        val date = com.example.parentapp.core.util.DateTimeFormatters.localDate(dateFrom)
        val time = com.example.parentapp.core.util.DateTimeFormatters.localDateTime(current).toLocalTime()
        return com.example.parentapp.core.util.DateTimeFormatters.localDateTimeToMillis(LocalDateTime.of(date, time))
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this.appContainer
                TaskEditorViewModel(
                    c.caregiverTaskRepository, c.childRepository, c.userRepository,
                    c.userSession, c.auditLogger, createSavedStateHandle(),
                )
            }
        }
    }
}
