package com.example.parentapp.unit

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.testing.DAY
import com.example.parentapp.testing.TestEnv
import com.example.parentapp.testing.awaitState
import com.example.parentapp.testing.settle
import com.example.parentapp.ui.child.ChildEditorViewModel
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

/** UT-09 – UT-15: Child Profile Management module. */
@RunWith(AndroidJUnit4::class)
class ChildProfileTest {

    private lateinit var env: TestEnv
    private var parentId = 0L

    @Before
    fun setUp() = runBlocking {
        env = TestEnv()
        parentId = env.user("Sarah Mukasa", "parent@example.com", UserRole.PARENT_GUARDIAN)
        env.signIn(parentId, "Sarah Mukasa", UserRole.PARENT_GUARDIAN)
    }

    @After
    fun tearDown() = runBlocking { env.close() }

    private fun editor(childId: Long? = null) = ChildEditorViewModel(
        env.children, env.users, env.session, env.audit,
        SavedStateHandle(if (childId != null) mapOf("childId" to childId) else emptyMap()),
    )

    private suspend fun ChildEditorViewModel.fillValid(name: String = "Amani Mukasa") {
        uiState.awaitState { !it.loading }
        onFullName(name)
        onGender("Female")
        onDob(System.currentTimeMillis() - 6 * 365 * DAY)
        onGuardianName("Sarah Mukasa")
        onGuardianContact("+256772123456")
    }

    @Test
    fun ut09_childIsCreatedWithValidData() = runBlocking {
        val vm = editor()
        vm.fillValid()
        vm.save()
        val saved = vm.uiState.awaitState { it.savedId != null }
        val child = env.children.getChildById(saved.savedId!!)
        assertNotNull(child)
        assertEquals("Amani Mukasa", child!!.fullName)
        assertEquals(parentId, child.createdByUserId)
    }

    @Test
    fun ut10_emptyNameFieldIsRejected() = runBlocking {
        val vm = editor()
        vm.fillValid(name = "")
        vm.save()
        val state = vm.uiState.awaitState { it.errors.isNotEmpty() }
        assertEquals("Full name is required", state.errors["fullName"])
        settle()
        assertEquals(0, env.children.count())
    }

    @Test
    fun ut11_futureDateOfBirthIsRejected() = runBlocking {
        val vm = editor()
        vm.fillValid()
        vm.onDob(System.currentTimeMillis() + 10 * DAY)
        vm.save()
        val state = vm.uiState.awaitState { it.errors.isNotEmpty() }
        assertEquals("Date of birth cannot be in the future", state.errors["dob"])
        settle()
        assertEquals(0, env.children.count())
    }

    @Test
    fun ut12_childrenAreRetrievedByGuardianId() = runBlocking {
        val otherParent = env.user("David Okello", "david@example.com", UserRole.PARENT_GUARDIAN)
        env.child(parentId, "Amani Mukasa")
        env.child(parentId, "Brian Mukasa")
        env.child(otherParent, "Zawadi Okello")
        val mine = env.children.getChildrenVisibleToUser(UserRole.PARENT_GUARDIAN, parentId, null).first()
        assertEquals(listOf("Amani Mukasa", "Brian Mukasa"), mine.map { it.fullName })
    }

    @Test
    fun ut13_childProfileIsUpdated() = runBlocking {
        val id = env.child(parentId, "Amani Mukasa")
        val vm = editor(id)
        vm.uiState.awaitState { !it.loading && it.fullName == "Amani Mukasa" }
        vm.onFullName("Amani N. Mukasa")
        vm.onSchool("Nakasero Primary School")
        vm.save()
        vm.uiState.awaitState { it.savedId == id }
        val updated = env.children.getChildById(id)!!
        assertEquals("Amani N. Mukasa", updated.fullName)
        assertEquals("Nakasero Primary School", updated.schoolName)
        assertEquals(1, env.children.count())
    }

    @Test
    fun ut14_deletingChildCascadesToActivitiesRemindersAndTasks() = runBlocking {
        val caregiver = env.user("Mary Nabirye", "caregiver@example.com", UserRole.CAREGIVER)
        val id = env.child(parentId, "Amani Mukasa")
        val keep = env.child(parentId, "Brian Mukasa")
        val act = env.activity(id, parentId)
        env.reminder(id, parentId, System.currentTimeMillis() + DAY, activityId = act)
        env.task(id, caregiver, parentId)
        env.activity(keep, parentId)

        val vm = editor(id)
        vm.uiState.awaitState { !it.loading }
        vm.delete()
        vm.uiState.awaitState { it.deleted }

        assertNull(env.children.getChildById(id))
        assertTrue("activities of deleted child remain", env.activities.getActivitiesForChild(id).first().isEmpty())
        assertTrue("reminders of deleted child remain", env.reminders.getRemindersForChild(id).first().isEmpty())
        assertTrue("tasks of deleted child remain", env.tasks.getTasksForChild(id).first().isEmpty())
        // the sibling's records are untouched
        assertEquals(1, env.activities.getActivitiesForChild(keep).first().size)
    }

    @Test
    fun ut15_caregiverSeesOnlyAssignedChildrenAndCannotEditProfiles() = runBlocking {
        val cg1 = env.user("Mary Nabirye", "caregiver@example.com", UserRole.CAREGIVER)
        val cg2 = env.user("Grace Atim", "caregiver2@example.com", UserRole.CAREGIVER)
        val assigned = env.child(parentId, "Amani Mukasa", caregiverId = cg1)
        val viaTask = env.child(parentId, "Brian Mukasa")
        env.task(viaTask, cg1, parentId)
        env.child(parentId, "Zawadi Okello", caregiverId = cg2)

        val visible = env.children.getChildrenVisibleToUser(UserRole.CAREGIVER, cg1, null).first()
        assertEquals(setOf("Amani Mukasa", "Brian Mukasa"), visible.map { it.fullName }.toSet())

        // A caregiver who reaches the editor by another route must not be able to change the profile.
        env.signIn(cg1, "Mary Nabirye", UserRole.CAREGIVER)
        val vm = editor(assigned)
        vm.uiState.awaitState { !it.loading && it.fullName == "Amani Mukasa" }
        vm.onFullName("Changed By Caregiver")
        vm.save()
        settle()
        assertEquals("Amani Mukasa", env.children.getChildById(assigned)!!.fullName)
    }
}
