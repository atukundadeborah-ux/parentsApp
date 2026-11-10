package com.example.parentapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parentapp.data.repository.AppSettings
import com.example.parentapp.data.repository.SettingsRepository
import com.example.parentapp.data.repository.ThemeMode
import com.example.parentapp.di.AppContainer
import com.example.parentapp.di.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val container: AppContainer,
) : ViewModel() {

    val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _resetDone = MutableStateFlow(false)
    val resetDone = _resetDone.asStateFlow()

    fun setThemeMode(mode: ThemeMode) = launch { settingsRepository.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = launch { settingsRepository.setDynamicColor(enabled) }
    fun setNotificationsEnabled(enabled: Boolean) = launch { settingsRepository.setNotificationsEnabled(enabled) }
    fun setDefaultLeadTime(minutes: Int) = launch { settingsRepository.setDefaultLeadTime(minutes) }
    fun setQuietHours(enabled: Boolean) = launch { settingsRepository.setQuietHoursEnabled(enabled) }
    fun setAnalytics(enabled: Boolean) = launch { settingsRepository.setAnalyticsEnabled(enabled) }

    fun resetDemoData() = launch {
        container.resetDemoData()
        _resetDone.value = true
    }

    private fun launch(block: suspend () -> Unit) { viewModelScope.launch { block() } }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this.appContainer
                SettingsViewModel(c.settingsRepository, c)
            }
        }
    }
}
