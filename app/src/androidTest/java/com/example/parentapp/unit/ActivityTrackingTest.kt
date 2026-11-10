package com.example.parentapp.unit

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.parentapp.domain.model.ActivityCategory
import com.example.parentapp.domain.model.ActivityStatus
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.testing.DAY
import com.example.parentapp.testing.TestEnv
import com.example.parentapp.testing.awaitState
import com.example.parentapp.ui.activity.ActivityEditorViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** UT-16 – UT-21: Activity Tracking module. */
@RunWith(AndroidJUnit4::class)
class ActivityTrackingTest {

    private lateinit var env: TestEnv
    private var parentId = 0L
    private var amani = 0L
    private var brian = 0L

    @Before
    fun setUp() = runBlocking {
        env = TestEnv()
        parentId = env.user("Sarah Mukasa", "parent@example.com", UserRole.PARENT_GUARDIAN)
        amani = env.child(parentId, "Amani Mukasa")
        brian = env.child(parentId, "Brian Mukasa")
        env.signIn(parentId, "Sarah Mukasa", UserRole.PARENT_GUARDIAN)
    }

    @After
    fun tearDown() = runBlocking { env.close() }

    private suspend fun recordViaEditor(childId: Long, title: String): Long {
        val vm = ActivityEditorViewModel(env.activities, env.children, env.session, env.audit, SavedStateHandle())
        vm.uiState.awaitState { !it.loading }
        vm.onChild(childId)
        vm.onCategory(ActivityCategory.HEALTH)
        vm.onTitle(title)
        vm.onStatus(ActivityStatus.COMPLETED)
        vm.save()
        vm.uiState.awaitState { it.done }
        return env.activities.getAllActivitiesWithChildren().first().first { it.activity.title == title }.activity.id
    }

    @Test
    fun ut16_activityIsRecordedWithValidData() = runBlocking {
        val id = recordViaEditor(amani, "Immunisation at clinic")
        val saved = env.activities.getActivityById(id)!!
        assertEquals("Immunisation at clinic", saved.title)
        assertEquals(ActivityCategory.HEALTH, saved.category)
        assertEquals(ActivityStatus.COMPLETED, saved.status)
    }

    @Test
    fun ut17_activityIsLinkedToTheCorrectChild() = runBlocking {
        val id = recordViaEditor(brian, "Swimming lesson")
        val withChild = env.activities.getActivityWithChildById(id)!!
        assertEquals(brian, withChild.activity.childId)
        assertEquals("Brian Mukasa", withChild.childName)
        assertEquals(0, env.activities.getActivitiesForChild(amani).first().size)
    }

    @Test
    fun ut18_activityHistoryIsRetrievedByChildId() = runBlocking {
        val now = System.currentTimeMillis()
        env.activity(amani, parentId, at = now - 3 * DAY, title = "Oldest")
        env.activity(amani, parentId, at = now - 1 * DAY, title = "Newest")
        env.activity(amani, parentId, at = now - 2 * DAY, title = "Middle")
        env.activity(brian, parentId, at = now, title = "Other child")
        val history = env.activities.getActivitiesForChild(amani).first()
        assertEquals(listOf("Newest", "Middle", "Oldest"), history.map { it.title })
    }

    @Test
    fun ut19_activitiesAreFilteredByType() = runBlocking {
        env.activity(amani, parentId, category = ActivityCategory.SCHOOL, title = "School run")
        env.activity(amani, parentId, category = ActivityCategory.HEALTH, title = "Clinic")
        env.activity(brian, parentId, category = ActivityCategory.HEALTH, title = "Dentist")
        val health = env.activities.filterActivitiesByCategory(ActivityCategory.HEALTH).first()
        assertEquals(setOf("Clinic", "Dentist"), health.map { it.activity.title }.toSet())
    }

    @Test
    fun ut20_activitiesAreFilteredByDateRange() = runBlocking {
        val now = System.currentTimeMillis()
        env.activity(amani, parentId, at = now - 2 * DAY, title = "In range")
        env.activity(amani, parentId, at = now - 10 * DAY, title = "Too old")
        env.activity(amani, parentId, at = now + 5 * DAY, title = "Future")
        val week = env.activities.filterActivitiesByDateRange(now - 7 * DAY, now).first()
        assertEquals(listOf("In range"), week.map { it.activity.title })
    }

    @Test
    fun ut21_creatorIsAttributedFromTheSession() = runBlocking {
        val id = recordViaEditor(amani, "Breakfast")
        assertEquals(parentId, env.activities.getActivityById(id)!!.createdByUserId)
    }
}
