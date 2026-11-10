package com.example.parentapp.ui.activity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parentapp.data.local.model.ActivityWithChild
import com.example.parentapp.data.repository.ActivityRepository
import com.example.parentapp.di.appContainer
import com.example.parentapp.domain.model.ActivityCategory
import com.example.parentapp.domain.model.ActivityStatus
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.utils.session.UserSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class ActivityListUiState(
    val loading: Boolean = true,
    val scopedToChild: Boolean = false,
    val role: UserRole? = null,
    val category: ActivityCategory? = null,
    val status: ActivityStatus? = null,
    val items: List<ActivityWithChild> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityListViewModel(
    private val activityRepository: ActivityRepository,
    userSession: UserSession,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val childId: Long = savedStateHandle.get<Long>("childId") ?: -1L
    private val category = MutableStateFlow<ActivityCategory?>(null)
    private val status = MutableStateFlow<ActivityStatus?>(null)

    private val principal = combine(
        userSession.userId, userSession.userRole, userSession.institutionId,
    ) { id, role, inst -> Triple(id, role, inst) }

    val uiState: StateFlow<ActivityListUiState> = principal.flatMapLatest { (id, role, inst) ->
        val source = if (childId > 0) {
            activityRepository.getActivityRecordsByChild(childId)
        } else {
            activityRepository.getActivitiesVisibleToUser(role, id, inst)
        }
        combine(source, category, status) { list, cat, st ->
            ActivityListUiState(
                loading = false,
                scopedToChild = childId > 0,
                role = role,
                category = cat,
                status = st,
                items = list
                    .filter { cat == null || it.activity.category == cat }
                    .filter { st == null || it.activity.status == st },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityListUiState())

    fun onCategory(value: ActivityCategory?) { category.value = value }
    fun onStatus(value: ActivityStatus?) { status.value = value }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this.appContainer
                ActivityListViewModel(c.activityRepository, c.userSession, createSavedStateHandle())
            }
        }
    }
}
