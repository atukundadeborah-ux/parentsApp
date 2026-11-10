package com.example.parentapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parentapp.core.security.PasswordHasher
import com.example.parentapp.core.util.Validators
import com.example.parentapp.data.audit.AuditLogger
import com.example.parentapp.data.local.entity.UserAccount
import com.example.parentapp.data.repository.UserRepository
import com.example.parentapp.di.appContainer
import com.example.parentapp.utils.session.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = true,
    val account: UserAccount? = null,
    val fullName: String = "",
    val phone: String = "",
    val profileErrors: Map<String, String> = emptyMap(),
    val profileMessage: String? = null,
    val savingProfile: Boolean = false,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordErrors: Map<String, String> = emptyMap(),
    val passwordMessage: String? = null,
    val savingPassword: Boolean = false,
)

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val userSession: UserSession,
    private val auditLogger: AuditLogger,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val id = userSession.userId.first()
            val account = id?.let { userRepository.getUserById(it) }
            _uiState.update {
                it.copy(
                    loading = false,
                    account = account,
                    fullName = account?.fullName.orEmpty(),
                    phone = account?.phone.orEmpty(),
                )
            }
        }
    }

    fun onFullName(v: String) = _uiState.update { it.copy(fullName = v, profileErrors = it.profileErrors - "name", profileMessage = null) }
    fun onPhone(v: String) = _uiState.update { it.copy(phone = v, profileErrors = it.profileErrors - "phone", profileMessage = null) }
    fun onCurrentPassword(v: String) = _uiState.update { it.copy(currentPassword = v, passwordErrors = it.passwordErrors - "current", passwordMessage = null) }
    fun onNewPassword(v: String) = _uiState.update { it.copy(newPassword = v, passwordErrors = it.passwordErrors - "new", passwordMessage = null) }
    fun onConfirmPassword(v: String) = _uiState.update { it.copy(confirmPassword = v, passwordErrors = it.passwordErrors - "confirm", passwordMessage = null) }

    fun saveProfile() {
        val s = _uiState.value
        val account = s.account ?: return
        val errors = buildMap {
            Validators.nameError(s.fullName)?.let { put("name", it) }
            Validators.phoneError(s.phone)?.let { put("phone", it) }
        }
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(profileErrors = errors) }
            return
        }
        _uiState.update { it.copy(savingProfile = true) }
        viewModelScope.launch {
            val updated = account.copy(fullName = s.fullName.trim(), phone = s.phone.trim())
            userRepository.updateUser(updated)
            userSession.saveSession(updated.id, updated.fullName, updated.role, updated.institutionId)
            auditLogger.updated(account.id, updated.fullName, "UserAccount", account.id, "Updated own profile")
            _uiState.update {
                it.copy(savingProfile = false, account = updated, profileMessage = "Profile updated")
            }
        }
    }

    fun changePassword() {
        val s = _uiState.value
        val account = s.account ?: return
        val errors = buildMap {
            if (!PasswordHasher.verify(s.currentPassword, account.passwordHash)) put("current", "Current password is incorrect")
            Validators.passwordError(s.newPassword)?.let { put("new", it) }
            Validators.confirmPasswordError(s.newPassword, s.confirmPassword)?.let { put("confirm", it) }
        }
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(passwordErrors = errors) }
            return
        }
        _uiState.update { it.copy(savingPassword = true) }
        viewModelScope.launch {
            userRepository.setPassword(account.id, PasswordHasher.hash(s.newPassword))
            auditLogger.updated(account.id, account.fullName, "UserAccount", account.id, "Changed own password")
            _uiState.update {
                it.copy(
                    savingPassword = false,
                    currentPassword = "", newPassword = "", confirmPassword = "",
                    passwordMessage = "Password changed",
                )
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this.appContainer
                ProfileViewModel(c.userRepository, c.userSession, c.auditLogger)
            }
        }
    }
}
