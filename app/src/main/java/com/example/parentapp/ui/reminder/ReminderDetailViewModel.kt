package com.example.parentapp.ui.reminder

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parentapp.data.audit.AuditLogger
import com.example.parentapp.data.local.model.ReminderWithDetails
import com.example.parentapp.data.repository.ReminderRepository
import com.example.parentapp.di.appContainer
import com.example.parentapp.domain.model.ReminderStatus
import com.example.parentapp.notifications.NotificationHelper
import com.example.parentapp.notifications.ReminderScheduler
import com.example.parentapp.utils.session.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReminderDetailUiState(
    val loading: Boolean = true,
    val item: ReminderWithDetails? = null,
    val canManage: Boolean = false,
    val deleted: Boolean = false,
)

class ReminderDetailViewModel(
    private val app: Application,
    private val reminderRepository: ReminderRepository,
    private val userSession: UserSession,
    private val auditLogger: AuditLogger,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val reminderId: Long = savedStateHandle.get<Long>("reminderId") ?: -1L
    private val _uiState = MutableStateFlow(ReminderDetailUiState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    private fun refresh() = viewModelScope.launch {
        val item = reminderRepository.getReminderWithDetailsById(reminderId)
        _uiState.update { it.copy(loading = false, item = item, canManage = true) }
    }

    fun complete() = transition(ReminderStatus.COMPLETED, "Completed")
    fun dismiss() = transition(ReminderStatus.DISMISSED, "Dismissed")

    fun snooze(minutes: Long = 60) {
        val reminder = _uiState.value.item?.reminder ?: return
        viewModelScope.launch {
            ReminderScheduler.cancel(app, reminder)
            NotificationHelper.cancel(app, reminder.id)
            val newWhen = System.currentTimeMillis() + minutes * 60_000L
            val updated = reminder.copy(
                reminderDateTime = newWhen,
                leadTimeMinutes = 0,
                status = ReminderStatus.SNOOZED,
                updatedAt = System.currentTimeMillis(),
            )
            reminderRepository.updateReminder(updated)
            ReminderScheduler.schedule(app, updated)
            audit("Snoozed reminder '${reminder.title}' for ${minutes}m")
            refresh()
        }
    }

    private fun transition(status: ReminderStatus, verb: String) {
        val reminder = _uiState.value.item?.reminder ?: return
        viewModelScope.launch {
            ReminderScheduler.cancel(app, reminder)
            NotificationHelper.cancel(app, reminder.id)
            reminderRepository.updateStatus(reminder.id, status)
            audit("$verb reminder '${reminder.title}'")
            refresh()
        }
    }

    fun delete() {
        val reminder = _uiState.value.item?.reminder ?: return
        viewModelScope.launch {
            ReminderScheduler.cancel(app, reminder)
            NotificationHelper.cancel(app, reminder.id)
            reminderRepository.deleteReminder(reminder)
            audit("Deleted reminder '${reminder.title}'")
            _uiState.update { it.copy(deleted = true) }
        }
    }

    private suspend fun audit(message: String) {
        val actorId = userSession.userId.first() ?: 0L
        val actorName = userSession.userName.first().orEmpty()
        auditLogger.updated(actorId, actorName, "Reminder", reminderId, message)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as Application
                val c = this.appContainer
                ReminderDetailViewModel(app, c.reminderRepository, c.userSession, c.auditLogger, createSavedStateHandle())
            }
        }
    }
}
