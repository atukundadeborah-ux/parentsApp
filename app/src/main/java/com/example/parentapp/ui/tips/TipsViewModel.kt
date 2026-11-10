package com.example.parentapp.ui.tips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parentapp.data.local.entity.Tip
import com.example.parentapp.data.repository.TipRepository
import com.example.parentapp.di.appContainer
import com.example.parentapp.domain.model.TipCategory
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.utils.session.UserSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class TipsUiState(
    val loading: Boolean = true,
    val role: UserRole? = null,
    val query: String = "",
    val category: TipCategory? = null,
    val tips: List<Tip> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class TipsViewModel(
    private val tipRepository: TipRepository,
    userSession: UserSession,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val category = MutableStateFlow<TipCategory?>(null)

    val uiState: StateFlow<TipsUiState> = combine(query, category) { q, c -> q to c }
        .flatMapLatest { (q, c) ->
            val source = if (q.isNotBlank()) tipRepository.searchTips(q) else tipRepository.getAllTips()
            combine(source, userSession.userRole) { tips, role ->
                TipsUiState(
                    loading = false,
                    role = role,
                    query = q,
                    category = c,
                    tips = tips.filter { c == null || it.category == c },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TipsUiState())

    fun onQuery(value: String) { query.value = value }
    fun onCategory(value: TipCategory?) { category.value = value }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this.appContainer
                TipsViewModel(c.tipRepository, c.userSession)
            }
        }
    }
}
