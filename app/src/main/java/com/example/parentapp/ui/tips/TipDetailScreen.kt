package com.example.parentapp.ui.tips

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parentapp.core.designsystem.component.BadgeTone
import com.example.parentapp.core.designsystem.component.LoadingState
import com.example.parentapp.core.designsystem.component.ParentsAppTopBar
import com.example.parentapp.core.designsystem.component.StatusBadge
import com.example.parentapp.data.local.entity.Tip
import com.example.parentapp.data.repository.TipRepository
import com.example.parentapp.di.appContainer
import com.example.parentapp.ui.common.EmptyHint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TipDetailViewModel(
    private val tipRepository: TipRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val tipId: Long = savedStateHandle.get<Long>("tipId") ?: -1L
    private val _tip = MutableStateFlow<Tip?>(null)
    val tip = _tip.asStateFlow()
    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    init {
        viewModelScope.launch {
            _tip.value = tipRepository.getTipById(tipId)
            _loading.value = false
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { TipDetailViewModel(this.appContainer.tipRepository, createSavedStateHandle()) }
        }
    }
}

@Composable
fun TipDetailScreen(
    onNavigateUp: () -> Unit,
    viewModel: TipDetailViewModel = viewModel(factory = TipDetailViewModel.Factory),
) {
    val tip by viewModel.tip.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { ParentsAppTopBar(title = tip?.category?.label ?: "Tip", onNavigateUp = onNavigateUp) },
    ) { padding ->
        when {
            loading -> LoadingState(Modifier.padding(padding))
            tip == null -> EmptyHint("This tip is no longer available.")
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                StatusBadge(tip!!.category.label, BadgeTone.INFO)
                Text(
                    tip!!.title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                Text(tip!!.content, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
