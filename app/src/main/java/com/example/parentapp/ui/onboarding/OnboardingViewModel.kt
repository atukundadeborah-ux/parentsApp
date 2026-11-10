package com.example.parentapp.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parentapp.data.repository.SettingsRepository
import com.example.parentapp.di.appContainer
import kotlinx.coroutines.launch

class OnboardingViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    fun markComplete() {
        viewModelScope.launch { settingsRepository.setOnboardingComplete(true) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { OnboardingViewModel(this.appContainer.settingsRepository) }
        }
    }
}
