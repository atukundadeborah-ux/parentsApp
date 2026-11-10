package com.example.parentapp.ui.activity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parentapp.core.util.Validators
import com.example.parentapp.data.audit.AuditLogger
import com.example.parentapp.data.local.entity.Activity
import com.example.parentapp.data.local.entity.ChildProfile
import com.example.parentapp.data.repository.ActivityRepository
import com.example.parentapp.data.repository.ChildRepository
import com.example.parentapp.di.appContainer
import com.example.parentapp.domain.model.ActivityCategory
import com.example.parentapp.domain.model.ActivityStatus
import com.example.parentapp.utils.session.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActivityEditorUiState(
    val editing: Boolean = false,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val children: List<ChildProfile> = emptyList(),
    val childId: Long? = null,
    val category: ActivityCategory = ActivityCategory.SCHOOL,
    val title: String = "",
    val description: String = "",
    val dateTimeMillis: Long = System.currentTimeMillis(),
    val status: ActivityStatus = ActivityStatus.COMPLETED,
    val errors: Map<String, String> = emptyMap(),
    val done: Boolean = false,
)

class ActivityEditorViewModel(
    private val activityRepository: ActivityRepository,
    private val childRepository: ChildRepository,
    private val userSession: UserSession,
    private val auditLogger: AuditLogger,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val presetChildId: Long = savedStateHandle.get<Long>("childId") ?: -1L
    private val activityId: Long = savedStateHandle.get<Long>("activityId") ?: -1L

    private val _uiState = MutableStateFlow(ActivityEditorUiState(editing = activityId > 0))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val id = userSession.userId.first()
            val role = userSession.userRole.first()
            val inst = userSession.institutionId.first()
            val children = childRepository.getChildrenVisibleToUser(role, id, inst).first()
            val existing = activityId.takeIf { it > 0 }?.let { activityRepository.getActivityById(it) }
            _uiState.update { s ->
                s.copy(
                    loading = false,
                    children = children,
                    childId = existing?.childId ?: presetChildId.takeIf { it > 0 } ?: children.firstOrNull()?.id,
                    category = existing?.category ?: s.category,
                    title = existing?.title ?: "",
                    description = existing?.description ?: "",
                    dateTimeMillis = existing?.activityDateTime ?: s.dateTimeMillis,
                    status = existing?.status ?: s.status,
                )
            }
        }
    }

    fun onChild(id: Long) = _uiState.update { it.copy(childId = id, errors = it.errors - "child") }
    fun onCategory(v: ActivityCategory) = _uiState.update { it.copy(category = v) }
    fun onTitle(v: String) = _uiState.update { it.copy(title = v, errors = it.errors - "title") }
    fun onDescription(v: String) = _uiState.update { it.copy(description = v) }
    fun onDate(v: Long) = _uiState.update { it.copy(dateTimeMillis = mergeDate(it.dateTimeMillis, v)) }
    fun onTime(v: Long) = _uiState.update { it.copy(dateTimeMillis = v) }
    fun onStatus(v: ActivityStatus) = _uiState.update { it.copy(status = v) }

    fun save() {
        val s = _uiState.value
        val errors = buildMap {
            if (s.childId == null) put("child", "Select a child")
            Validators.requiredError(s.title, "Title")?.let { put("title", it) }
        }
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val actorId = userSession.userId.first() ?: 0L
            val actorName = userSession.userName.first().orEmpty()
            val existing = activityId.takeIf { it > 0 }?.let { activityRepository.getActivityById(it) }
            val activity = (existing ?: Activity(
                childId = s.childId!!, category = s.category, title = "", description = "",
                activityDateTime = s.dateTimeMillis, status = s.status, createdByUserId = actorId,
            )).copy(
                childId = s.childId!!,
                category = s.category,
                title = s.title.trim(),
                description = s.description.trim(),
                activityDateTime = s.dateTimeMillis,
                status = s.status,
                updatedAt = if (existing != null) System.currentTimeMillis() else null,
            )
            if (existing != null) {
                activityRepository.updateActivity(activity)
                auditLogger.updated(actorId, actorName, "Activity", activity.id, "Updated activity '${activity.title}'")
            } else {
                val newId = activityRepository.insertActivity(activity)
                auditLogger.created(actorId, actorName, "Activity", newId, "Recorded activity '${activity.title}'")
            }
            _uiState.update { it.copy(saving = false, done = true) }
        }
    }

    private fun mergeDate(current: Long, newDate: Long): Long {
        val date = com.example.parentapp.core.util.DateTimeFormatters.localDateTime(newDate).toLocalDate()
        val time = com.example.parentapp.core.util.DateTimeFormatters.localDateTime(current).toLocalTime()
        return com.example.parentapp.core.util.DateTimeFormatters.localDateTimeToMillis(
            java.time.LocalDateTime.of(date, time),
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this.appContainer
                ActivityEditorViewModel(
                    c.activityRepository, c.childRepository, c.userSession, c.auditLogger,
                    createSavedStateHandle(),
                )
            }
        }
    }
}
