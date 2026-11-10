package com.example.parentapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parentapp.core.util.DateTimeFormatters
import com.example.parentapp.data.local.model.ActivityWithChild
import com.example.parentapp.data.local.model.ReminderWithDetails
import com.example.parentapp.data.local.model.TaskWithNames
import com.example.parentapp.data.repository.ActivityRepository
import com.example.parentapp.data.repository.CaregiverTaskRepository
import com.example.parentapp.data.repository.ChildRepository
import com.example.parentapp.data.repository.ReminderRepository
import com.example.parentapp.di.appContainer
import com.example.parentapp.domain.model.ReminderStatus
import com.example.parentapp.domain.model.TaskStatus
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.utils.session.UserSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class DashboardUiState(
    val loading: Boolean = true,
    val userName: String = "",
    val role: UserRole? = null,
    val childCount: Int = 0,
    val todayReminderCount: Int = 0,
    val missedReminderCount: Int = 0,
    val pendingTaskCount: Int = 0,
    val upcomingReminders: List<ReminderWithDetails> = emptyList(),
    val openTasks: List<TaskWithNames> = emptyList(),
    val recentActivities: List<ActivityWithChild> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val userSession: UserSession,
    private val childRepository: ChildRepository,
    private val reminderRepository: ReminderRepository,
    private val taskRepository: CaregiverTaskRepository,
    private val activityRepository: ActivityRepository,
) : ViewModel() {

    private data class Principal(val userId: Long?, val role: UserRole?, val name: String, val institutionId: Long?)

    private val principal = combine(
        userSession.userId,
        userSession.userRole,
        userSession.userName,
        userSession.institutionId,
    ) { id, role, name, inst -> Principal(id, role, name.orEmpty(), inst) }

    val uiState: StateFlow<DashboardUiState> = principal.flatMapLatest { p ->
        if (p.userId == null) return@flatMapLatest flowOf(DashboardUiState(loading = false))

        val children = childRepository.getChildrenVisibleToUser(p.role, p.userId, p.institutionId)
        val reminders = reminderRepository.getRemindersVisibleToUser(p.role, p.userId, p.institutionId)
        val tasks = taskRepository.getTasksVisibleToUser(p.role, p.userId, p.institutionId)
        val activities = activityRepository.getActivitiesVisibleToUser(p.role, p.userId, p.institutionId)

        combine(children, reminders, tasks, activities) { childList, reminderList, taskList, activityList ->
            val now = System.currentTimeMillis()
            val dayStart = DateTimeFormatters.localDateToMillis(LocalDate.now())
            val dayEnd = dayStart + 86_400_000L

            DashboardUiState(
                loading = false,
                userName = p.name,
                role = p.role,
                childCount = childList.size,
                todayReminderCount = reminderList.count {
                    it.reminder.reminderDateTime in dayStart until dayEnd && it.reminder.status == ReminderStatus.PENDING
                },
                missedReminderCount = reminderList.count { it.reminder.status == ReminderStatus.MISSED },
                pendingTaskCount = taskList.count { it.task.status == TaskStatus.PENDING },
                upcomingReminders = reminderList
                    .filter { it.reminder.status == ReminderStatus.PENDING && it.reminder.reminderDateTime >= now }
                    .sortedBy { it.reminder.reminderDateTime }
                    .take(5),
                openTasks = taskList
                    .filter { it.task.status == TaskStatus.PENDING || it.task.status == TaskStatus.REQUIRES_FOLLOW_UP }
                    .sortedBy { it.task.dueDateTime }
                    .take(5),
                recentActivities = activityList.take(5),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this.appContainer
                DashboardViewModel(
                    c.userSession,
                    c.childRepository,
                    c.reminderRepository,
                    c.caregiverTaskRepository,
                    c.activityRepository,
                )
            }
        }
    }
}
