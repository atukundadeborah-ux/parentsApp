package com.example.parentapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parentapp.data.repository.SettingsRepository
import com.example.parentapp.di.appContainer
import com.example.parentapp.utils.session.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class StartRoute { LOADING, ONBOARDING, LOGIN, DASHBOARD }

class SplashViewModel(
    private val settingsRepository: SettingsRepository,
    private val userSession: UserSession,
) : ViewModel() {

    private val _route = MutableStateFlow(StartRoute.LOADING)
    val route = _route.asStateFlow()

    init {
        viewModelScope.launch {
            val onboarded = settingsRepository.settings.first().onboardingComplete
            val userId = userSession.userId.first()
            _route.value = when {
                userId != null -> StartRoute.DASHBOARD
                !onboarded -> StartRoute.ONBOARDING
                else -> StartRoute.LOGIN
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this.appContainer
                SplashViewModel(c.settingsRepository, c.userSession)
            }
        }
    }
}
