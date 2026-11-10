package com.example.parentapp.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parentapp.data.local.model.CaregiverCompletion
import com.example.parentapp.data.local.model.LabelCount
import com.example.parentapp.data.repository.ActivityRepository
import com.example.parentapp.data.repository.CaregiverTaskRepository
import com.example.parentapp.data.repository.ReminderRepository
import com.example.parentapp.di.appContainer
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.utils.session.UserSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

enum class ReportRange(val label: String, val days: Long) {
    WEEK("Last 7 days", 7),
    MONTH("Last 30 days", 30),
    QUARTER("Last 90 days", 90),
}

data class ReportsUiState(
    val loading: Boolean = true,
    val role: UserRole? = null,
    val range: ReportRange = ReportRange.MONTH,
    val activityTotal: Int = 0,
    val activityByCategory: List<LabelCount> = emptyList(),
    val activityByStatus: List<LabelCount> = emptyList(),
    val missedReminders: Int = 0,
    val reminderByStatus: List<LabelCount> = emptyList(),
    val caregiverCompletion: List<CaregiverCompletion> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    private val activityRepository: ActivityRepository,
    private val reminderRepository: ReminderRepository,
    private val taskRepository: CaregiverTaskRepository,
    userSession: UserSession,
) : ViewModel() {

    private val range = MutableStateFlow(ReportRange.MONTH)

    val uiState: StateFlow<ReportsUiState> = range.flatMapLatest { r ->
        val now = System.currentTimeMillis()
        val start = now - r.days * 86_400_000L

        val activitiesFlow = combine(
            activityRepository.totalInRange(start, now),
            activityRepository.countByCategoryInRange(start, now),
            activityRepository.countByStatusInRange(start, now),
        ) { total, byCat, byStatus -> Triple(total, byCat, byStatus) }

        val remindersFlow = combine(
            reminderRepository.missedCountInRange(start, now),
            reminderRepository.countByStatusInRange(start, now),
        ) { missed, byStatus -> missed to byStatus }

        combine(
            activitiesFlow,
            remindersFlow,
            taskRepository.caregiverCompletionReport(),
            userSession.userRole,
        ) { activities, reminders, completion, role ->
            ReportsUiState(
                loading = false,
                role = role,
                range = r,
                activityTotal = activities.first,
                activityByCategory = activities.second,
                activityByStatus = activities.third,
                missedReminders = reminders.first,
                reminderByStatus = reminders.second,
                caregiverCompletion = completion.filter { it.total > 0 },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    fun onRange(value: ReportRange) { range.value = value }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this.appContainer
                ReportsViewModel(c.activityRepository, c.reminderRepository, c.caregiverTaskRepository, c.userSession)
            }
        }
    }
}
