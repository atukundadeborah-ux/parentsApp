package com.example.parentapp.ui.referral

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parentapp.data.local.entity.ReferralContact
import com.example.parentapp.data.repository.ReferralContactRepository
import com.example.parentapp.di.appContainer
import com.example.parentapp.domain.model.ReferralCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ReferralUiState(
    val loading: Boolean = true,
    val query: String = "",
    val category: ReferralCategory? = null,
    val emergency: List<ReferralContact> = emptyList(),
    val others: List<ReferralContact> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReferralViewModel(
    private val repository: ReferralContactRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val category = MutableStateFlow<ReferralCategory?>(null)

    val uiState: StateFlow<ReferralUiState> = combine(query, category) { q, c -> q to c }
        .flatMapLatest { (q, c) ->
            val source = when {
                q.isNotBlank() -> repository.searchContacts(q)
                c != null -> repository.getContactsByCategory(c)
                else -> repository.getAllContacts()
            }
            source.map { list ->
                ReferralUiState(
                    loading = false,
                    query = q,
                    category = c,
                    emergency = list.filter { it.isEmergency },
                    others = list.filter { !it.isEmergency },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReferralUiState())

    fun onQuery(value: String) { query.value = value }
    fun onCategory(value: ReferralCategory?) { category.value = value }

    companion object {
        val Factory = viewModelFactory {
            initializer { ReferralViewModel(this.appContainer.referralContactRepository) }
        }
    }
}
