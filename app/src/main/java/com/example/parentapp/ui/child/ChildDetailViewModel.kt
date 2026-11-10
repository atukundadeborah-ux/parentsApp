package com.example.parentapp.ui.child

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parentapp.data.local.entity.ChildProfile
import com.example.parentapp.data.local.model.ActivityWithChild
import com.example.parentapp.data.local.model.ReminderWithDetails
import com.example.parentapp.data.local.model.TaskWithNames
import com.example.parentapp.data.repository.ActivityRepository
import com.example.parentapp.data.repository.CaregiverTaskRepository
import com.example.parentapp.data.repository.ChildRepository
import com.example.parentapp.data.repository.ReminderRepository
import com.example.parentapp.di.appContainer
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.utils.session.UserSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ChildDetailUiState(
    val loading: Boolean = true,
    val child: ChildProfile? = null,
    val canEdit: Boolean = false,
    val activities: List<ActivityWithChild> = emptyList(),
    val reminders: List<ReminderWithDetails> = emptyList(),
    val tasks: List<TaskWithNames> = emptyList(),
)

class ChildDetailViewModel(
    childRepository: ChildRepository,
    activityRepository: ActivityRepository,
    reminderRepository: ReminderRepository,
    taskRepository: CaregiverTaskRepository,
    userSession: UserSession,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val childId: Long = savedStateHandle.get<Long>("childId") ?: -1L

    val uiState: StateFlow<ChildDetailUiState> = combine(
        childRepository.getChildByIdFlow(childId),
        activityRepository.getActivityRecordsByChild(childId),
        reminderRepository.getReminderRecordsByChild(childId),
        taskRepository.getTasksWithNamesForChild(childId),
        userSession.userRole,
    ) { child, activities, reminders, tasks, role ->
        ChildDetailUiState(
            loading = false,
            child = child,
            canEdit = role == UserRole.PARENT_GUARDIAN || role == UserRole.INSTITUTIONAL_USER || role == UserRole.ADMIN,
            activities = activities,
            reminders = reminders,
            tasks = tasks,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChildDetailUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this.appContainer
                ChildDetailViewModel(
                    c.childRepository, c.activityRepository, c.reminderRepository,
                    c.caregiverTaskRepository, c.userSession, createSavedStateHandle(),
                )
            }
        }
    }
}
