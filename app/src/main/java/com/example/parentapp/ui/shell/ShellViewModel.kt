package com.example.parentapp.ui.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parentapp.data.audit.AuditLogger
import com.example.parentapp.di.appContainer
import com.example.parentapp.domain.model.AuditAction
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.utils.session.UserSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SessionInfo(
    val userId: Long? = null,
    val name: String = "",
    val role: UserRole? = null,
)

/** Activity-scoped session holder shared by the app shell screens. */
class ShellViewModel(
    private val userSession: UserSession,
    private val auditLogger: AuditLogger,
) : ViewModel() {

    val session = combine(userSession.userId, userSession.userName, userSession.userRole) { id, name, role ->
        SessionInfo(id, name.orEmpty(), role)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionInfo())

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            session.value.let { s ->
                if (s.userId != null) auditLogger.auth(s.userId, s.name, AuditAction.LOGOUT, "Signed out")
            }
            userSession.clearSession()
            onDone()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this.appContainer
                ShellViewModel(c.userSession, c.auditLogger)
            }
        }
    }
}
