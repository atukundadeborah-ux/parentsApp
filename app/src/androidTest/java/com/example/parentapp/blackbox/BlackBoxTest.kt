package com.example.parentapp.blackbox

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.parentapp.MainActivity
import com.example.parentapp.ParentsApp
import com.example.parentapp.data.local.entity.CaregiverTask
import com.example.parentapp.di.DefaultAppContainer
import com.example.parentapp.domain.model.TaskStatus
import com.example.parentapp.testing.DAY
import com.example.parentapp.testing.activeNotifications
import com.example.parentapp.testing.appContext
import com.example.parentapp.testing.clearNotifications
import com.example.parentapp.testing.grantNotificationPermission
import com.example.parentapp.testing.title
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * BB-01 – BB-07, BB-09, BB-10, BB-12: black box tests driven through the real user interface
 * of the installed app (MainActivity, the app's own database seeded with the demo data).
 * BB-08 (notification at the configured time) and BB-11 (phone dialler opens) were executed
 * manually on the emulator; see Appendix XI.
 */
@RunWith(AndroidJUnit4::class)
class BlackBoxTest {

    private val container get() = (appContext as ParentsApp).container as DefaultAppContainer

    /** Signed out, onboarding done, demo data present — i.e. the app opens on the login screen. */
    private val freshStart = object : ExternalResource() {
        override fun before() = runBlocking {
            grantNotificationPermission()
            // The app seeds its database on start-up; wait for that instead of racing it.
            kotlinx.coroutines.withTimeout(15_000) {
                while (container.referralContactRepository.count() == 0) kotlinx.coroutines.delay(200)
            }
            container.settingsRepository.setOnboardingComplete(true)
            container.userSession.clearSession()
        }
    }

    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(freshStart).around(compose)

    @After
    fun signOut() = runBlocking { container.userSession.clearSession() }

    // ---------- helpers ----------

    private fun waitForText(text: String, substring: Boolean = false, timeoutMs: Long = 10_000) {
        try {
            compose.waitUntil(timeoutMs) {
                compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty() ||
                    compose.onAllNodesWithText(text, substring = substring, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
            // Leave the visible screen in logcat to explain the failure.
            compose.onRoot(useUnmergedTree = true).printToLog("BlackBoxTest")
            throw AssertionError("\"$text\" not shown within $timeoutMs ms", e)
        }
    }

    private fun field(label: String): SemanticsNodeInteraction =
        compose.onNode(hasSetTextAction() and hasText(label))

    private fun type(label: String, value: String) {
        field(label).performScrollTo().performTextInput(value)
    }

    /** Clicks the control showing [text]; scrolls to it first when it sits in a scrolling column. */
    private fun button(text: String) {
        val merged = compose.onAllNodes(hasText(text) and hasClickAction())
        val node = if (merged.fetchSemanticsNodes().isNotEmpty()) merged[0]
        else compose.onAllNodes(hasText(text), useUnmergedTree = true)[0]
        runCatching { node.performScrollTo() }
        node.performClick()
    }

    private fun tab(label: String) = compose.onNode(hasText(label) and hasClickAction()).performClick()

    private fun login(email: String, password: String = "Password123!") {
        waitForText("Welcome back")
        type("Email", email)
        type("Password", password)
        button("Sign in")
    }

    private fun openRegistration() {
        waitForText("Welcome back")
        button("New here? Create an account")
        waitForText("How will you use ParentsApp?")
        compose.onNodeWithText("Parent or guardian").performClick()
        waitForText("Signing up as", substring = true)
    }

    private fun uniqueEmail() = "bb${System.currentTimeMillis()}@example.com"

    // ---------- tests ----------

    @Test
    fun bb01_registerWithValidDetails_accountCreatedAndDashboardShown() {
        val email = uniqueEmail()
        openRegistration()
        type("Full name", "Ruth Nankya")
        type("Email", email)
        type("Phone number", "+256772333444")
        type("Password", "Password123")
        type("Confirm password", "Password123")
        button("Create account")
        waitForText("Hello, Ruth")
        waitForText("Due today")
        runBlocking { assertTrue(container.userRepository.getUserByEmail(email) != null) }
    }

    @Test
    fun bb02_registerWithEmptyFields_inlineErrorsNoAccount() {
        openRegistration()
        val before = runBlocking { container.userRepository.count() }
        button("Create account")
        waitForText("Full name is required")
        compose.onNodeWithText("Email is required").assertExists()
        compose.onNodeWithText("Phone number is required").assertExists()
        compose.onNodeWithText("Password is required").assertExists()
        runBlocking { assertEquals(before, container.userRepository.count()) }
    }

    @Test
    fun bb03_registerWithMismatchedPasswords_inlineErrorNoAccount() {
        val email = uniqueEmail()
        openRegistration()
        type("Full name", "Ruth Nankya")
        type("Email", email)
        type("Phone number", "+256772333444")
        type("Password", "Password123")
        type("Confirm password", "Password124")
        button("Create account")
        waitForText("Passwords do not match")
        runBlocking { assertNull(container.userRepository.getUserByEmail(email)) }
    }

    @Test
    fun bb04_registerWithEmailInUse_duplicateErrorNoAccount() {
        openRegistration()
        val before = runBlocking { container.userRepository.count() }
        type("Full name", "Another Sarah")
        type("Email", "parent@example.com")
        type("Phone number", "+256772333444")
        type("Password", "Password123")
        type("Confirm password", "Password123")
        button("Create account")
        waitForText("An account with this email already exists")
        runBlocking { assertEquals(before, container.userRepository.count()) }
    }

    @Test
    fun bb05_loginWithCorrectCredentials_roleDashboardShown() {
        login("parent@example.com")
        waitForText("Hello, Sarah")
        compose.onNodeWithText("My children").assertExists()
    }

    @Test
    fun bb06_loginWithWrongPassword_genericError() {
        login("parent@example.com", "WrongPassword1")
        waitForText("Incorrect email or password")
        compose.onNodeWithText("Welcome back").assertExists()
        runBlocking { assertNull(container.userSession.userId.first()) }
    }

    @Test
    fun bb07_addChildWithValidData_profileShownInList() {
        val name = "Joan Mukasa ${System.currentTimeMillis() % 10_000}"
        login("parent@example.com")
        waitForText("Hello, Sarah")
        tab("Children")
        waitForText("Add child")
        button("Add child")
        waitForText("Full name")
        type("Full name", name)
        compose.onNode(hasText("Gender")).performScrollTo().performClick()
        compose.waitForIdle()
        compose.onAllNodes(hasText("Female") and hasClickAction()).let { it[it.fetchSemanticsNodes().size - 1] }.performClick()
        compose.onNode(hasText("Date of birth")).performScrollTo().performClick()
        compose.onNode(hasContentDescription("text input", substring = true, ignoreCase = true)).performClick()
        compose.onNode(hasSetTextAction() and hasText("Date", substring = true)).performTextInput("03012020")
        button("OK")
        type("Guardian contact", "+256772123456")
        button("Add child")
        waitForText(name)
        runBlocking {
            val saved = container.childRepository.getAllChildren().first().first { it.fullName == name }
            container.childRepository.deleteChild(saved)
        }
    }

    @Test
    fun bb09_assignTaskToCaregiver_taskInCaregiversPendingList() {
        val title = "Collect school report ${System.currentTimeMillis() % 10_000}"
        login("parent@example.com")
        waitForText("Hello, Sarah")
        tab("Tasks")
        waitForText("Assign")
        button("Assign")
        waitForText("Assign to caregiver")
        compose.onNode(hasText("Assign to caregiver")).performScrollTo().performClick()
        compose.waitForIdle()
        // The field already shows the default caregiver; the menu item is the last match (popup window).
        compose.onAllNodes(hasText("Mary Nabirye") and hasClickAction()).let { it[it.fetchSemanticsNodes().size - 1] }.performClick()
        type("Task", title)
        button("Assign task")
        waitForText(title)
        // Sign out and check the caregiver's own list.
        compose.onNodeWithContentDescription("Sign out").performClick()
        login("caregiver@example.com")
        waitForText("Hello, Mary")
        tab("Tasks")
        waitForText(title)
        runBlocking {
            val task = container.caregiverTaskRepository.getTasksVisibleToUser(
                com.example.parentapp.domain.model.UserRole.ADMIN, null, null,
            ).first().first { it.task.title == title }
            assertEquals(TaskStatus.PENDING, task.task.status)
            container.caregiverTaskRepository.deleteTask(task.task)
        }
    }

    @Test
    fun bb10_caregiverCompletesTask_statusUpdatedAndParentNotified() {
        clearNotifications()
        val title = "Give evening medicine ${System.currentTimeMillis() % 10_000}"
        val taskId = runBlocking {
            val parent = container.userRepository.getUserByEmail("parent@example.com")!!
            val mary = container.userRepository.getUserByEmail("caregiver@example.com")!!
            val child = container.childRepository.getChildrenVisibleToUser(
                com.example.parentapp.domain.model.UserRole.PARENT_GUARDIAN, parent.id, null,
            ).first().first()
            container.caregiverTaskRepository.insertTask(
                CaregiverTask(
                    childId = child.id, assignedToCaregiverId = mary.id, assignedByUserId = parent.id,
                    title = title, description = "", dueDateTime = System.currentTimeMillis() + DAY,
                    status = TaskStatus.PENDING,
                ),
            )
        }
        login("caregiver@example.com")
        waitForText("Hello, Mary")
        tab("Tasks")
        waitForText(title)
        compose.onNodeWithText(title).performClick()
        waitForText("Complete")
        button("Complete")
        compose.waitUntil(10_000) {
            runBlocking { container.caregiverTaskRepository.getTaskById(taskId)?.status == TaskStatus.COMPLETED }
        }
        compose.waitUntil(10_000) { activeNotifications().any { it.title().contains("Task completed") } }
        runBlocking { container.caregiverTaskRepository.getTaskById(taskId)?.let { container.caregiverTaskRepository.deleteTask(it) } }
        clearNotifications()
    }

    @Test
    fun bb12_logout_sessionClearedAndLoginShown() {
        login("parent@example.com")
        waitForText("Hello, Sarah")
        compose.onNodeWithContentDescription("Sign out").performClick()
        waitForText("Welcome back")
        runBlocking { assertNull(container.userSession.userId.first()) }
    }
}
