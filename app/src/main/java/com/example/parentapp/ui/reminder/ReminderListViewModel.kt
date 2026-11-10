package com.example.parentapp.ui.reminder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parentapp.data.local.model.ReminderWithDetails
import com.example.parentapp.data.repository.ReminderRepository
import com.example.parentapp.di.appContainer
import com.example.parentapp.domain.model.ReminderStatus
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.utils.session.UserSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ReminderTab { UPCOMING, MISSED, ALL }

data class ReminderListUiState(
    val loading: Boolean = true,
    val scopedToChild: Boolean = false,
    val role: UserRole? = null,
    val tab: ReminderTab = ReminderTab.UPCOMING,
    val items: List<ReminderWithDetails> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderListViewModel(
    private val reminderRepository: ReminderRepository,
    userSession: UserSession,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val childId: Long = savedStateHandle.get<Long>("childId") ?: -1L
    private val tab = MutableStateFlow(ReminderTab.UPCOMING)

    private val principal = combine(
        userSession.userId, userSession.userRole, userSession.institutionId,
    ) { id, role, inst -> Triple(id, role, inst) }

    init {
        viewModelScope.launch { reminderRepository.markPastPendingRemindersMissed() }
    }

    val uiState: StateFlow<ReminderListUiState> = principal.flatMapLatest { (id, role, inst) ->
        val source = if (childId > 0) {
            reminderRepository.getReminderRecordsByChild(childId)
        } else {
            reminderRepository.getRemindersVisibleToUser(role, id, inst)
        }
        combine(source, tab) { list, currentTab ->
            val now = System.currentTimeMillis()
            val filtered = when (currentTab) {
                ReminderTab.UPCOMING -> list.filter {
                    it.reminder.status == ReminderStatus.PENDING || it.reminder.status == ReminderStatus.SNOOZED
                }.sortedBy { it.reminder.reminderDateTime }
                ReminderTab.MISSED -> list.filter { it.reminder.status == ReminderStatus.MISSED }
                    .sortedByDescending { it.reminder.reminderDateTime }
                ReminderTab.ALL -> list.sortedByDescending { it.reminder.reminderDateTime }
            }
            ReminderListUiState(
                loading = false,
                scopedToChild = childId > 0,
                role = role,
                tab = currentTab,
                items = filtered,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderListUiState())

    fun onTab(value: ReminderTab) { tab.value = value }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this.appContainer
                ReminderListViewModel(c.reminderRepository, c.userSession, createSavedStateHandle())
            }
        }
    }
}
