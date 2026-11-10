package com.example.parentapp.unit

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.parentapp.domain.model.TaskStatus
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.testing.DAY
import com.example.parentapp.testing.HOUR
import com.example.parentapp.testing.TestEnv
import com.example.parentapp.testing.appContext
import com.example.parentapp.testing.awaitNotification
import com.example.parentapp.testing.awaitState
import com.example.parentapp.testing.clearNotifications
import com.example.parentapp.testing.grantNotificationPermission
import com.example.parentapp.testing.settle
import com.example.parentapp.testing.title
import com.example.parentapp.ui.task.TaskDetailViewModel
import com.example.parentapp.ui.task.TaskEditorViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** UT-29 – UT-35: Caregiver Task Management module. */
@RunWith(AndroidJUnit4::class)
class CaregiverTaskTest {

    private lateinit var env: TestEnv
    private var parentId = 0L
    private var caregiverId = 0L
    private var childId = 0L

    @Before
    fun setUp() = runBlocking {
        env = TestEnv()
        parentId = env.user("Sarah Mukasa", "parent@example.com", UserRole.PARENT_GUARDIAN)
        caregiverId = env.user("Mary Nabirye", "caregiver@example.com", UserRole.CAREGIVER)
        childId = env.child(parentId, "Amani Mukasa", caregiverId = caregiverId)
    }

    @After
    fun tearDown() = runBlocking { env.close() }

    private fun editor() = TaskEditorViewModel(
        env.tasks, env.children, env.users, env.session, env.audit, SavedStateHandle(),
    )

    private fun detail(taskId: Long) = TaskDetailViewModel(
        appContext as android.app.Application, env.tasks, env.session, env.audit, SavedStateHandle(mapOf("taskId" to taskId)),
    )

    @Test
    fun ut29_taskIsAssignedWithValidData() = runBlocking {
        env.signIn(parentId, "Sarah Mukasa", UserRole.PARENT_GUARDIAN)
        val vm = editor()
        vm.uiState.awaitState { !it.loading }
        vm.onChild(childId)
        vm.onCaregiver(caregiverId)
        vm.onTitle("Pick Amani from school")
        vm.save()
        vm.uiState.awaitState { it.done }
        val task = env.tasks.getTasksForCaregiver(caregiverId).first().single()
        assertEquals("Pick Amani from school", task.title)
        assertEquals(parentId, task.assignedByUserId)
        assertEquals(childId, task.childId)
        assertEquals(TaskStatus.PENDING, task.status)
    }

    @Test
    fun ut30_taskAssignmentByCaregiverRoleIsRejected() = runBlocking {
        env.signIn(caregiverId, "Mary Nabirye", UserRole.CAREGIVER)
        val vm = editor()
        vm.uiState.awaitState { !it.loading }
        vm.onChild(childId)
        vm.onCaregiver(caregiverId)
        vm.onTitle("Self-assigned task")
        vm.save()
        settle()
        assertTrue("caregiver was able to assign a task", env.tasks.getTasksForChild(childId).first().isEmpty())
    }

    @Test
    fun ut31_statusChangesFromPendingToRequiresFollowUpWithNotes() = runBlocking {
        val id = env.task(childId, caregiverId, parentId)
        env.signIn(caregiverId, "Mary Nabirye", UserRole.CAREGIVER)
        val vm = detail(id)
        vm.uiState.awaitState { it.item != null }
        vm.setStatus(TaskStatus.REQUIRES_FOLLOW_UP, "Child had a fever, parent to call school")
        vm.uiState.awaitState { it.item?.task?.status == TaskStatus.REQUIRES_FOLLOW_UP }
        val task = env.tasks.getTaskById(id)!!
        assertEquals(TaskStatus.REQUIRES_FOLLOW_UP, task.status)
        assertEquals("Child had a fever, parent to call school", task.followUpNotes)
        assertNull(task.completedAt)
    }

    @Test
    fun ut32_statusChangesFromPendingToCompletedWithCompletionTime() = runBlocking {
        val id = env.task(childId, caregiverId, parentId)
        env.signIn(caregiverId, "Mary Nabirye", UserRole.CAREGIVER)
        val vm = detail(id)
        vm.uiState.awaitState { it.item != null }
        vm.setStatus(TaskStatus.COMPLETED, null)
        vm.uiState.awaitState { it.item?.task?.status == TaskStatus.COMPLETED }
        val task = env.tasks.getTaskById(id)!!
        assertEquals(TaskStatus.COMPLETED, task.status)
        assertNotNull(task.completedAt)
    }

    @Test
    fun ut33_overdueTaskIsMarkedMissed() = runBlocking {
        val now = System.currentTimeMillis()
        val overdue = env.task(childId, caregiverId, parentId, due = now - HOUR)
        val future = env.task(childId, caregiverId, parentId, due = now + DAY)
        val done = env.task(childId, caregiverId, parentId, due = now - DAY, status = TaskStatus.COMPLETED)
        env.tasks.markPastPendingTasksMissed(now)
        assertEquals(TaskStatus.MISSED, env.tasks.getTaskById(overdue)!!.status)
        assertEquals(TaskStatus.PENDING, env.tasks.getTaskById(future)!!.status)
        assertEquals(TaskStatus.COMPLETED, env.tasks.getTaskById(done)!!.status)
    }

    @Test
    fun ut34_parentIsNotifiedOnTaskCompletion() = runBlocking {
        grantNotificationPermission()
        clearNotifications()
        val id = env.task(childId, caregiverId, parentId, title = "Give Amani lunch")
        env.signIn(caregiverId, "Mary Nabirye", UserRole.CAREGIVER)
        val vm = detail(id)
        vm.uiState.awaitState { it.item != null }
        vm.setStatus(TaskStatus.COMPLETED, null)
        vm.uiState.awaitState { it.item?.task?.status == TaskStatus.COMPLETED }
        val posted = awaitNotification { it.title().contains("Task completed") }
        assertNotNull("no task-completed notification for the parent", posted)
        clearNotifications()
    }

    @Test
    fun ut35_taskVisibilityIsScopedToAssigneeAndAssigner() = runBlocking {
        val otherParent = env.user("David Okello", "david@example.com", UserRole.PARENT_GUARDIAN)
        val otherCaregiver = env.user("Grace Atim", "caregiver2@example.com", UserRole.CAREGIVER)
        val otherChild = env.child(otherParent, "Zawadi Okello")
        env.task(childId, caregiverId, parentId, title = "Mine")
        env.task(otherChild, otherCaregiver, otherParent, title = "Theirs")

        val caregiverView = env.tasks.getTasksVisibleToUser(UserRole.CAREGIVER, caregiverId, null).first()
        assertEquals(listOf("Mine"), caregiverView.map { it.task.title })
        val parentView = env.tasks.getTasksVisibleToUser(UserRole.PARENT_GUARDIAN, parentId, null).first()
        assertEquals(listOf("Mine"), parentView.map { it.task.title })
        val otherCaregiverView = env.tasks.getTasksVisibleToUser(UserRole.CAREGIVER, otherCaregiver, null).first()
        assertEquals(listOf("Theirs"), otherCaregiverView.map { it.task.title })
        val adminView = env.tasks.getTasksVisibleToUser(UserRole.ADMIN, null, null).first()
        assertEquals(2, adminView.size)
    }
}
