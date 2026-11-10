package com.example.parentapp.ui.activity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parentapp.data.audit.AuditLogger
import com.example.parentapp.data.local.model.ActivityWithChild
import com.example.parentapp.data.repository.ActivityRepository
import com.example.parentapp.di.appContainer
import com.example.parentapp.domain.model.ActivityStatus
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.utils.session.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActivityDetailUiState(
    val loading: Boolean = true,
    val item: ActivityWithChild? = null,
    val canEdit: Boolean = false,
    val deleted: Boolean = false,
)

class ActivityDetailViewModel(
    private val activityRepository: ActivityRepository,
    private val userSession: UserSession,
    private val auditLogger: AuditLogger,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val activityId: Long = savedStateHandle.get<Long>("activityId") ?: -1L
    private val _uiState = MutableStateFlow(ActivityDetailUiState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    private fun refresh() = viewModelScope.launch {
        val item = activityRepository.getActivityWithChildById(activityId)
        val role = userSession.userRole.first()
        _uiState.update {
            it.copy(
                loading = false,
                item = item,
                canEdit = role != UserRole.CAREGIVER,
            )
        }
    }

    fun setStatus(status: ActivityStatus) {
        val current = _uiState.value.item?.activity ?: return
        viewModelScope.launch {
            activityRepository.updateActivity(current.copy(status = status, updatedAt = System.currentTimeMillis()))
            val actorId = userSession.userId.first() ?: 0L
            val actorName = userSession.userName.first().orEmpty()
            auditLogger.updated(actorId, actorName, "Activity", current.id, "Set '${current.title}' to ${status.label}")
            refresh()
        }
    }

    fun delete() {
        val current = _uiState.value.item?.activity ?: return
        viewModelScope.launch {
            activityRepository.deleteActivity(current)
            val actorId = userSession.userId.first() ?: 0L
            val actorName = userSession.userName.first().orEmpty()
            auditLogger.deleted(actorId, actorName, "Activity", current.id, "Deleted activity '${current.title}'")
            _uiState.update { it.copy(deleted = true) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this.appContainer
                ActivityDetailViewModel(c.activityRepository, c.userSession, c.auditLogger, createSavedStateHandle())
            }
        }
    }
}
