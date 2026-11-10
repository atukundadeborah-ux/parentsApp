package com.example.parentapp.ui.auth

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
import com.example.parentapp.domain.model.AccountStatus
import com.example.parentapp.domain.model.AuditAction
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.utils.session.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val category: String = "",
    val fieldErrors: Map<AuthField, String> = emptyMap(),
    val formError: String? = null,
    val submitting: Boolean = false,
    val success: Boolean = false,
)

enum class AuthField { NAME, EMAIL, PHONE, PASSWORD, CONFIRM }

class AuthViewModel(
    private val userRepository: UserRepository,
    private val userSession: UserSession,
    private val auditLogger: AuditLogger,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun setCategory(category: String) = _uiState.update { it.copy(category = category) }

    fun onField(field: AuthField, value: String) = _uiState.update { state ->
        state.copy(
            fullName = if (field == AuthField.NAME) value else state.fullName,
            email = if (field == AuthField.EMAIL) value else state.email,
            phone = if (field == AuthField.PHONE) value else state.phone,
            password = if (field == AuthField.PASSWORD) value else state.password,
            confirmPassword = if (field == AuthField.CONFIRM) value else state.confirmPassword,
            fieldErrors = state.fieldErrors - field,
            formError = null,
        )
    }

    fun login() {
        val state = _uiState.value
        val errors = buildMap {
            Validators.emailError(state.email)?.let { put(AuthField.EMAIL, it) }
            if (state.password.isEmpty()) put(AuthField.PASSWORD, "Password is required")
        }
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = errors) }
            return
        }
        _uiState.update { it.copy(submitting = true, formError = null) }
        viewModelScope.launch {
            val user = userRepository.getUserByEmail(state.email.trim())
            when {
                user == null || !PasswordHasher.verify(state.password, user.passwordHash) ->
                    _uiState.update { it.copy(submitting = false, formError = "Incorrect email or password") }

                user.status == AccountStatus.SUSPENDED ->
                    _uiState.update { it.copy(submitting = false, formError = "This account has been suspended. Contact an administrator.") }

                else -> {
                    userSession.saveSession(user.id, user.fullName, user.role, user.institutionId)
                    auditLogger.auth(user.id, user.fullName, AuditAction.LOGIN, "Signed in")
                    _uiState.update { it.copy(submitting = false, success = true) }
                }
            }
        }
    }

    fun register() {
        val state = _uiState.value
        val errors = buildMap {
            Validators.nameError(state.fullName)?.let { put(AuthField.NAME, it) }
            Validators.emailError(state.email)?.let { put(AuthField.EMAIL, it) }
            Validators.phoneError(state.phone)?.let { put(AuthField.PHONE, it) }
            Validators.passwordError(state.password)?.let { put(AuthField.PASSWORD, it) }
            Validators.confirmPasswordError(state.password, state.confirmPassword)?.let { put(AuthField.CONFIRM, it) }
        }
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = errors) }
            return
        }
        _uiState.update { it.copy(submitting = true, formError = null) }
        viewModelScope.launch {
            if (userRepository.getUserByEmail(state.email.trim()) != null) {
                _uiState.update {
                    it.copy(submitting = false, fieldErrors = mapOf(AuthField.EMAIL to "An account with this email already exists"))
                }
                return@launch
            }
            val role = UserRole.fromCategory(state.category)
            val user = UserAccount(
                fullName = state.fullName.trim(),
                email = state.email.trim(),
                phone = state.phone.trim(),
                passwordHash = PasswordHasher.hash(state.password),
                role = role,
                userCategory = state.category.ifBlank { role.label },
            )
            val id = userRepository.insertUser(user)
            userSession.saveSession(id, user.fullName, role, null)
            auditLogger.record(id, user.fullName, AuditAction.CREATE, "UserAccount", id, "Registered as ${role.label}")
            _uiState.update { it.copy(submitting = false, success = true) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this.appContainer
                AuthViewModel(c.userRepository, c.userSession, c.auditLogger)
            }
        }
    }
}
