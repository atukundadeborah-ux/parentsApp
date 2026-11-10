package com.example.parentapp.ui.reminder

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parentapp.core.util.DateTimeFormatters
import com.example.parentapp.core.util.Validators
import com.example.parentapp.data.audit.AuditLogger
import com.example.parentapp.data.local.entity.ChildProfile
import com.example.parentapp.data.local.entity.Reminder
import com.example.parentapp.data.repository.ChildRepository
import com.example.parentapp.data.repository.ReminderRepository
import com.example.parentapp.data.repository.SettingsRepository
import com.example.parentapp.di.appContainer
import com.example.parentapp.domain.model.Priority
import com.example.parentapp.domain.model.ReminderCategory
import com.example.parentapp.domain.model.ReminderLeadTime
import com.example.parentapp.domain.model.ReminderStatus
import com.example.parentapp.notifications.ReminderScheduler
import com.example.parentapp.utils.session.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class ReminderEditorUiState(
    val editing: Boolean = false,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val children: List<ChildProfile> = emptyList(),
    val childId: Long? = null,
    val title: String = "",
    val category: ReminderCategory = ReminderCategory.MEDICATION,
    val dateTimeMillis: Long = defaultWhen(),
    val leadTime: ReminderLeadTime = ReminderLeadTime.MIN_30,
    val priority: Priority = Priority.MEDIUM,
    val notificationEnabled: Boolean = true,
    val exactAlarmsBlocked: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val done: Boolean = false,
) {
    companion object {
        fun defaultWhen(): Long = DateTimeFormatters.localDateTimeToMillis(
            LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0),
        )
    }
}

class ReminderEditorViewModel(
    private val app: Application,
    private val reminderRepository: ReminderRepository,
    private val childRepository: ChildRepository,
    private val settingsRepository: SettingsRepository,
    private val userSession: UserSession,
    private val auditLogger: AuditLogger,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val presetChildId: Long = savedStateHandle.get<Long>("childId") ?: -1L
    private val reminderId: Long = savedStateHandle.get<Long>("reminderId") ?: -1L

    private val _uiState = MutableStateFlow(ReminderEditorUiState(editing = reminderId > 0))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val id = userSession.userId.first()
            val role = userSession.userRole.first()
            val inst = userSession.institutionId.first()
            val children = childRepository.getChildrenVisibleToUser(role, id, inst).first()
            val defaultLead = ReminderLeadTime.fromMinutes(settingsRepository.settings.first().defaultLeadTimeMinutes)
            val existing = reminderId.takeIf { it > 0 }?.let { reminderRepository.getReminderById(it) }
            _uiState.update { s ->
                s.copy(
                    loading = false,
                    children = children,
                    childId = existing?.childId ?: presetChildId.takeIf { it > 0 } ?: children.firstOrNull()?.id,
                    title = existing?.title ?: "",
                    category = existing?.category ?: s.category,
                    dateTimeMillis = existing?.reminderDateTime ?: s.dateTimeMillis,
                    leadTime = existing?.let { ReminderLeadTime.fromMinutes(it.leadTimeMinutes) } ?: defaultLead,
                    priority = existing?.priority ?: s.priority,
                    notificationEnabled = existing?.notificationEnabled ?: true,
                    exactAlarmsBlocked = !ReminderScheduler.canScheduleExact(app),
                )
            }
        }
    }

    fun onChild(id: Long) = _uiState.update { it.copy(childId = id, errors = it.errors - "child") }
    fun onTitle(v: String) = _uiState.update { it.copy(title = v, errors = it.errors - "title") }
    fun onCategory(v: ReminderCategory) = _uiState.update { it.copy(category = v) }
    fun onDate(v: Long) = _uiState.update { it.copy(dateTimeMillis = merge(it.dateTimeMillis, dateFrom = v)) }
    fun onTime(v: Long) = _uiState.update { it.copy(dateTimeMillis = v, errors = it.errors - "when") }
    fun onLeadTime(v: ReminderLeadTime) = _uiState.update { it.copy(leadTime = v) }
    fun onPriority(v: Priority) = _uiState.update { it.copy(priority = v) }
    fun onNotificationEnabled(v: Boolean) = _uiState.update { it.copy(notificationEnabled = v) }

    fun save() {
        val s = _uiState.value
        val errors = buildMap {
            if (s.childId == null) put("child", "Select a child")
            Validators.requiredError(s.title, "Title")?.let { put("title", it) }
            if (!s.editing && s.dateTimeMillis <= System.currentTimeMillis()) put("when", "Pick a time in the future")
        }
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val actorId = userSession.userId.first() ?: 0L
            val actorName = userSession.userName.first().orEmpty()
            val existing = reminderId.takeIf { it > 0 }?.let { reminderRepository.getReminderById(it) }

            val base = existing ?: Reminder(
                childId = s.childId!!, title = "", category = s.category,
                reminderDateTime = s.dateTimeMillis, priority = s.priority,
                status = ReminderStatus.PENDING, notificationEnabled = s.notificationEnabled,
                createdByUserId = actorId,
            )
            val draft = base.copy(
                childId = s.childId!!,
                title = s.title.trim(),
                category = s.category,
                reminderDateTime = s.dateTimeMillis,
                leadTimeMinutes = s.leadTime.minutes,
                priority = s.priority,
                notificationEnabled = s.notificationEnabled,
                status = if (existing != null && existing.status == ReminderStatus.MISSED && s.dateTimeMillis > System.currentTimeMillis()) {
                    ReminderStatus.PENDING
                } else {
                    base.status
                },
                updatedAt = if (existing != null) System.currentTimeMillis() else null,
            )

            val savedId: Long
            if (existing != null) {
                ReminderScheduler.cancel(app, existing)
                val withCode = draft.copy(notificationRequestCode = existing.notificationRequestCode ?: existing.id.toInt())
                reminderRepository.updateReminder(withCode)
                savedId = withCode.id
                ReminderScheduler.schedule(app, withCode)
                auditLogger.updated(actorId, actorName, "Reminder", savedId, "Updated reminder '${withCode.title}'")
            } else {
                val newId = reminderRepository.insertReminder(draft)
                val withCode = draft.copy(id = newId, notificationRequestCode = newId.toInt())
                reminderRepository.updateReminder(withCode)
                savedId = newId
                ReminderScheduler.schedule(app, withCode)
                auditLogger.created(actorId, actorName, "Reminder", newId, "Created reminder '${withCode.title}'")
            }
            _uiState.update { it.copy(saving = false, done = true) }
        }
    }

    private fun merge(current: Long, dateFrom: Long): Long {
        val date = DateTimeFormatters.localDate(dateFrom)
        val time = DateTimeFormatters.localDateTime(current).toLocalTime()
        return DateTimeFormatters.localDateTimeToMillis(LocalDateTime.of(date, time))
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as Application
                val c = this.appContainer
                ReminderEditorViewModel(
                    app, c.reminderRepository, c.childRepository, c.settingsRepository,
                    c.userSession, c.auditLogger, createSavedStateHandle(),
                )
            }
        }
    }
}
