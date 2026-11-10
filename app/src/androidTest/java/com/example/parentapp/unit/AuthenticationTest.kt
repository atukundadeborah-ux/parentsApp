package com.example.parentapp.unit

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.parentapp.core.security.PasswordHasher
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.testing.TestEnv
import com.example.parentapp.testing.awaitState
import com.example.parentapp.testing.settle
import com.example.parentapp.ui.auth.AuthField
import com.example.parentapp.ui.auth.AuthViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** UT-01 – UT-08: User Registration and Authentication module. */
@RunWith(AndroidJUnit4::class)
class AuthenticationTest {

    private lateinit var env: TestEnv
    private lateinit var vm: AuthViewModel

    @Before
    fun setUp() = runBlocking {
        env = TestEnv()
        env.session.clearSession()
        vm = AuthViewModel(env.users, env.session, env.audit)
    }

    @After
    fun tearDown() = runBlocking { env.close() }

    private fun fillRegistration(
        email: String = "sarah@example.com",
        password: String = "Password123",
        confirm: String = password,
    ) {
        vm.setCategory("Parent / Guardian")
        vm.onField(AuthField.NAME, "Sarah Mukasa")
        vm.onField(AuthField.EMAIL, email)
        vm.onField(AuthField.PHONE, "+256772123456")
        vm.onField(AuthField.PASSWORD, password)
        vm.onField(AuthField.CONFIRM, confirm)
    }

    private suspend fun registerExisting(email: String, password: String): Long {
        val other = AuthViewModel(env.users, env.session, env.audit)
        other.setCategory("Parent / Guardian")
        other.onField(AuthField.NAME, "Existing User")
        other.onField(AuthField.EMAIL, email)
        other.onField(AuthField.PHONE, "+256772000001")
        other.onField(AuthField.PASSWORD, password)
        other.onField(AuthField.CONFIRM, password)
        other.register()
        other.uiState.awaitState { it.success }
        env.session.clearSession()
        return env.users.getUserByEmail(email)!!.id
    }

    @Test
    fun ut01_validRegistrationCreatesAccount() = runBlocking {
        fillRegistration()
        vm.register()
        vm.uiState.awaitState { it.success }
        val user = env.users.getUserByEmail("sarah@example.com")
        assertNotNull(user)
        assertEquals("Sarah Mukasa", user!!.fullName)
        assertEquals(UserRole.PARENT_GUARDIAN, user.role)
    }

    @Test
    fun ut02_duplicateEmailIsRejected() = runBlocking {
        registerExisting("sarah@example.com", "Password123")
        fillRegistration(email = "sarah@example.com")
        vm.register()
        val state = vm.uiState.awaitState { it.fieldErrors.containsKey(AuthField.EMAIL) }
        assertEquals("An account with this email already exists", state.fieldErrors[AuthField.EMAIL])
        assertFalse(state.success)
        assertEquals(1, env.users.count())
    }

    @Test
    fun ut03_passwordIsStoredAsSaltedPbkdf2HashNotPlainText() = runBlocking {
        fillRegistration(password = "Password123")
        vm.register()
        vm.uiState.awaitState { it.success }
        val stored = env.users.getUserByEmail("sarah@example.com")!!.passwordHash
        assertNotEquals("Password123", stored)
        assertFalse(stored.contains("Password123"))
        assertTrue(stored.startsWith("PBKDF2WithHmac"))
        assertTrue(PasswordHasher.verify("Password123", stored))
    }

    @Test
    fun ut04_passwordShorterThanEightCharactersIsRejected() = runBlocking {
        fillRegistration(password = "Pass12")
        vm.register()
        val state = vm.uiState.awaitState { it.fieldErrors.containsKey(AuthField.PASSWORD) }
        assertEquals("Use at least 8 characters", state.fieldErrors[AuthField.PASSWORD])
        settle()
        assertEquals(0, env.users.count())
    }

    @Test
    fun ut05_invalidEmailFormatIsRejected() = runBlocking {
        fillRegistration(email = "sarah.example.com")
        vm.register()
        val state = vm.uiState.awaitState { it.fieldErrors.containsKey(AuthField.EMAIL) }
        assertEquals("Enter a valid email address", state.fieldErrors[AuthField.EMAIL])
        settle()
        assertEquals(0, env.users.count())
    }

    @Test
    fun ut06_loginSucceedsWithCorrectCredentials() = runBlocking {
        registerExisting("sarah@example.com", "Password123")
        vm.onField(AuthField.EMAIL, "sarah@example.com")
        vm.onField(AuthField.PASSWORD, "Password123")
        vm.login()
        val state = vm.uiState.awaitState { it.success || it.formError != null }
        assertTrue(state.success)
        assertNull(state.formError)
    }

    @Test
    fun ut07_loginFailsWithWrongPasswordAndGenericMessage() = runBlocking {
        registerExisting("sarah@example.com", "Password123")
        vm.onField(AuthField.EMAIL, "sarah@example.com")
        vm.onField(AuthField.PASSWORD, "WrongPass999")
        vm.login()
        val state = vm.uiState.awaitState { it.formError != null || it.success }
        assertFalse(state.success)
        // Generic: does not reveal whether the email or the password was wrong.
        assertEquals("Incorrect email or password", state.formError)
        assertNull(env.session.userId.first())
    }

    @Test
    fun ut08_sessionIsCreatedOnSuccessfulLogin() = runBlocking {
        val id = registerExisting("sarah@example.com", "Password123")
        assertNull(env.session.userId.first())
        vm.onField(AuthField.EMAIL, "sarah@example.com")
        vm.onField(AuthField.PASSWORD, "Password123")
        vm.login()
        vm.uiState.awaitState { it.success }
        assertEquals(id, env.session.userId.first())
        assertEquals(UserRole.PARENT_GUARDIAN, env.session.userRole.first())
        assertEquals("Existing User", env.session.userName.first())
    }
}
