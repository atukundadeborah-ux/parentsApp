package com.example.parentapp.unit

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.parentapp.data.local.model.ReminderWithDetails
import com.example.parentapp.domain.model.ReminderLeadTime
import com.example.parentapp.domain.model.ReminderStatus
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.notifications.NotificationHelper
import com.example.parentapp.notifications.ReminderAlarmReceiver
import com.example.parentapp.notifications.ReminderScheduler
import com.example.parentapp.testing.DAY
import com.example.parentapp.testing.HOUR
import com.example.parentapp.testing.MINUTE
import com.example.parentapp.testing.TestEnv
import com.example.parentapp.testing.appContext
import com.example.parentapp.testing.awaitNotification
import com.example.parentapp.testing.awaitState
import com.example.parentapp.testing.grantNotificationPermission
import com.example.parentapp.testing.title
import com.example.parentapp.ui.reminder.ReminderDetailViewModel
import com.example.parentapp.ui.reminder.ReminderEditorViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UT-22, UT-24 – UT-28: Reminders and Alarms module.
 * (UT-23, the trigger-time calculation, is a JVM unit test: ReminderModelTest.)
 */
@RunWith(AndroidJUnit4::class)
class ReminderTest {

    private lateinit var env: TestEnv
    private var parentId = 0L
    private var childId = 0L
    private val app get() = appContext as Application

    @Before
    fun setUp() = runBlocking {
        env = TestEnv()
        parentId = env.user("Sarah Mukasa", "parent@example.com", UserRole.PARENT_GUARDIAN)
        childId = env.child(parentId, "Amani Mukasa")
        env.signIn(parentId, "Sarah Mukasa", UserRole.PARENT_GUARDIAN)
    }

    @After
    fun tearDown() = runBlocking { env.close() }

    private fun alarmExists(requestCode: Int): Boolean = PendingIntent.getBroadcast(
        appContext, requestCode, Intent(appContext, ReminderAlarmReceiver::class.java),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    ) != null

    @Test
    fun ut22_reminderIsCreatedWithValidData() = runBlocking {
        val vm = ReminderEditorViewModel(
            app, env.reminders, env.children, env.settings, env.session, env.audit, SavedStateHandle(),
        )
        vm.uiState.awaitState { !it.loading }
        val due = System.currentTimeMillis() + 2 * DAY
        vm.onChild(childId)
        vm.onTitle("Polio booster")
        vm.onTime(due)
        vm.onLeadTime(ReminderLeadTime.HOUR_1)
        vm.save()
        vm.uiState.awaitState { it.done }

        val saved = env.reminders.getRemindersForChild(childId).first().single()
        assertEquals("Polio booster", saved.title)
        assertEquals(ReminderStatus.PENDING, saved.status)
        assertEquals(60, saved.leadTimeMinutes)
        assertEquals(parentId, saved.createdByUserId)
        ReminderScheduler.cancel(appContext, saved)
    }

    @Test
    fun ut24_pendingIntentIsRegisteredWithAlarmManager() = runBlocking {
        val id = env.reminder(childId, parentId, System.currentTimeMillis() + 3 * HOUR)
        val reminder = env.reminders.getReminderById(id)!!.copy(notificationRequestCode = 900_024)
        ReminderScheduler.cancel(appContext, 900_024)
        assertTrue(!alarmExists(900_024))
        ReminderScheduler.schedule(appContext, reminder)
        assertTrue("no PendingIntent registered", alarmExists(900_024))
        ReminderScheduler.cancel(appContext, reminder)
    }

    @Test
    fun ut25_notificationIsShownWhenTheAlarmFires() = runBlocking {
        grantNotificationPermission()
        val id = env.reminder(childId, parentId, System.currentTimeMillis() + 10 * MINUTE, title = "Give malaria medicine")
        val item = env.reminders.getReminderWithDetailsById(id)!!
        val probe = ReminderWithDetails(item.reminder.copy(id = 900_025), item.childName, item.activityTitle)
        NotificationHelper.showReminderAlarm(appContext, probe)
        val posted = awaitNotification { it.id == NotificationHelper.notificationIdFor(900_025) }
        assertNotNull("alarm notification not posted", posted)
        assertTrue(posted!!.title().contains("Give malaria medicine"))
        NotificationHelper.cancel(appContext, 900_025)
    }

    @Test
    fun ut26_markingReminderDoneChangesStatusFromPendingToCompleted() = runBlocking {
        val id = env.reminder(childId, parentId, System.currentTimeMillis() + HOUR)
        val vm = ReminderDetailViewModel(app, env.reminders, env.session, env.audit, SavedStateHandle(mapOf("reminderId" to id)))
        vm.uiState.awaitState { it.item != null }
        assertEquals(ReminderStatus.PENDING, vm.uiState.value.item!!.reminder.status)
        vm.complete()
        vm.uiState.awaitState { it.item?.reminder?.status == ReminderStatus.COMPLETED }
        assertEquals(ReminderStatus.COMPLETED, env.reminders.getReminderById(id)!!.status)
    }

    @Test
    fun ut27_overdueReminderIsMarkedMissed() = runBlocking {
        val now = System.currentTimeMillis()
        val overdue = env.reminder(childId, parentId, now - 2 * HOUR)
        val upcoming = env.reminder(childId, parentId, now + 2 * HOUR)
        val done = env.reminder(childId, parentId, now - 3 * HOUR, status = ReminderStatus.COMPLETED)
        env.reminders.markPastPendingRemindersMissed(now)
        assertEquals(ReminderStatus.MISSED, env.reminders.getReminderById(overdue)!!.status)
        assertEquals(ReminderStatus.PENDING, env.reminders.getReminderById(upcoming)!!.status)
        assertEquals(ReminderStatus.COMPLETED, env.reminders.getReminderById(done)!!.status)
        assertEquals(listOf(overdue), env.reminders.getMissedReminders(now).first().map { it.reminder.id })
    }

    @Test
    fun ut28_cancellingReminderRemovesItsPendingIntent() = runBlocking {
        val id = env.reminder(childId, parentId, System.currentTimeMillis() + 3 * HOUR)
        val reminder = env.reminders.getReminderById(id)!!.copy(notificationRequestCode = 900_028)
        ReminderScheduler.schedule(appContext, reminder)
        assertTrue(alarmExists(900_028))
        ReminderScheduler.cancel(appContext, reminder)
        assertTrue("PendingIntent still registered", !alarmExists(900_028))
    }
}
