package com.example.parentapp.integration

import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.parentapp.ParentsApp
import com.example.parentapp.data.local.DatabaseSeeder
import com.example.parentapp.data.local.entity.ChildProfile
import com.example.parentapp.data.local.entity.Reminder
import com.example.parentapp.domain.model.ActivityCategory
import com.example.parentapp.domain.model.Priority
import com.example.parentapp.domain.model.ReminderCategory
import com.example.parentapp.domain.model.ReminderStatus
import com.example.parentapp.domain.model.TaskStatus
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.notifications.ReminderAlarmReceiver
import com.example.parentapp.notifications.ReminderScheduler
import com.example.parentapp.testing.DAY
import com.example.parentapp.testing.HOUR
import com.example.parentapp.testing.TestEnv
import com.example.parentapp.testing.appContext
import com.example.parentapp.testing.awaitNotification
import com.example.parentapp.testing.awaitState
import com.example.parentapp.testing.grantNotificationPermission
import com.example.parentapp.testing.title
import com.example.parentapp.ui.activity.ActivityEditorViewModel
import com.example.parentapp.ui.auth.AuthField
import com.example.parentapp.ui.auth.AuthViewModel
import com.example.parentapp.ui.child.ChildDetailViewModel
import com.example.parentapp.ui.child.ChildEditorViewModel
import com.example.parentapp.ui.referral.ReferralViewModel
import com.example.parentapp.ui.reports.ReportsViewModel
import com.example.parentapp.ui.shell.TopLevelDestination
import com.example.parentapp.ui.shell.TopLevelDestination.ACTIVITIES
import com.example.parentapp.ui.shell.TopLevelDestination.AUDIT
import com.example.parentapp.ui.shell.TopLevelDestination.CHILDREN
import com.example.parentapp.ui.shell.TopLevelDestination.HOME
import com.example.parentapp.ui.shell.TopLevelDestination.MORE
import com.example.parentapp.ui.shell.TopLevelDestination.REMINDERS
import com.example.parentapp.ui.shell.TopLevelDestination.REPORTS
import com.example.parentapp.ui.shell.TopLevelDestination.TASKS
import com.example.parentapp.ui.shell.TopLevelDestination.TIPS
import com.example.parentapp.ui.shell.TopLevelDestination.USERS
import com.example.parentapp.ui.task.TaskEditorViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** IT-01 – IT-10: modules working together through ViewModels, repositories and Room. */
@RunWith(AndroidJUnit4::class)
class IntegrationTest {

    private lateinit var env: TestEnv
    private var parentId = 0L
    private var caregiverId = 0L

    @Before
    fun setUp() = runBlocking {
        env = TestEnv()
        parentId = env.user("Sarah Mukasa", "parent@example.com", UserRole.PARENT_GUARDIAN)
        caregiverId = env.user("Mary Nabirye", "caregiver@example.com", UserRole.CAREGIVER)
        env.signIn(parentId, "Sarah Mukasa", UserRole.PARENT_GUARDIAN)
    }

    @After
    fun tearDown() = runBlocking { env.close() }

    private fun alarmExists(code: Int) = PendingIntent.getBroadcast(
        appContext, code, Intent(appContext, ReminderAlarmReceiver::class.java),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    ) != null

    @Test
    fun it01_childProfileToActivity() = runBlocking {
        val child = env.child(parentId, "Amani Mukasa")
        val vm = ActivityEditorViewModel(env.activities, env.children, env.session, env.audit, SavedStateHandle(mapOf("childId" to child)))
        vm.uiState.awaitState { !it.loading }
        vm.onCategory(ActivityCategory.SCHOOL)
        vm.onTitle("Parents' day at Nakasero Primary")
        vm.save()
        vm.uiState.awaitState { it.done }
        val history = env.activities.getActivityRecordsByChild(child).first()
        assertEquals(1, history.size)
        assertEquals("Amani Mukasa", history.single().childName)
        assertEquals("Parents' day at Nakasero Primary", history.single().activity.title)
    }

    @Test
    fun it02_childProfileToReminder() = runBlocking {
        val child = env.child(parentId, "Amani Mukasa")
        val act = env.activity(child, parentId, title = "Dental check-up")
        val id = env.reminder(child, parentId, System.currentTimeMillis() + DAY, activityId = act, title = "Dentist tomorrow")
        val details = env.reminders.getReminderWithDetailsById(id)!!
        assertEquals("Amani Mukasa", details.childName)
        assertEquals("Dental check-up", details.activityTitle)
        assertEquals(listOf(id), env.reminders.getReminderRecordsByChild(child).first().map { it.reminder.id })
    }

    @Test
    fun it03_caregiverTaskToUserAndChild() = runBlocking {
        val child = env.child(parentId, "Amani Mukasa")
        val vm = TaskEditorViewModel(env.tasks, env.children, env.users, env.session, env.audit, SavedStateHandle())
        vm.uiState.awaitState { !it.loading }
        vm.onChild(child)
        vm.onCaregiver(caregiverId)
        vm.onTitle("Bath and bedtime")
        vm.save()
        vm.uiState.awaitState { it.done }
        val forCaregiver = env.tasks.getTasksAssignedToCaregiver(caregiverId).first().single()
        assertEquals("Amani Mukasa", forCaregiver.childName)
        assertEquals("Mary Nabirye", forCaregiver.caregiverName)
        assertEquals("Sarah Mukasa", forCaregiver.assignedByName)
    }

    @Test
    fun it04_reminderRecordToAlarmNotification() = runBlocking {
        grantNotificationPermission()
        // The alarm receiver reads the application's own database, so this record is written there.
        val c = (appContext as ParentsApp).container
        val childId = c.childRepository.insertChild(
            ChildProfile(
                fullName = "IT04 Test Child", gender = "Male", dateOfBirth = "2020-01-01",
                guardianName = "Guardian", guardianContact = "+256772000000", createdByUserId = 1,
            ),
        )
        val reminderId = c.reminderRepository.insertReminder(
            Reminder(
                childId = childId, title = "IT04 Give medicine", category = ReminderCategory.MEDICATION,
                reminderDateTime = System.currentTimeMillis(), priority = Priority.HIGH,
                status = ReminderStatus.PENDING, notificationEnabled = true, createdByUserId = 1,
            ),
        )
        try {
            appContext.sendBroadcast(
                Intent(appContext, ReminderAlarmReceiver::class.java)
                    .putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId),
            )
            val posted = awaitNotification { it.title().contains("IT04 Give medicine") }
            assertNotNull("alarm notification was not posted", posted)
            val text = posted!!.notification.extras.getCharSequence(android.app.Notification.EXTRA_TEXT).toString()
            assertTrue("notification should name the child: $text", text.contains("IT04 Test Child"))
        } finally {
            c.reminderRepository.getReminderById(reminderId)?.let { c.reminderRepository.deleteReminder(it) }
            c.childRepository.getChildById(childId)?.let { c.childRepository.deleteChild(it) }
            com.example.parentapp.notifications.NotificationHelper.cancel(appContext, reminderId)
        }
    }

    @Test
    fun it05_roleBasedNavigationForFourRoles() = runBlocking {
        assertEquals(listOf(HOME, CHILDREN, REMINDERS, TASKS, MORE), TopLevelDestination.forRole(UserRole.PARENT_GUARDIAN))
        assertEquals(listOf(HOME, TASKS, CHILDREN, TIPS, MORE), TopLevelDestination.forRole(UserRole.CAREGIVER))
        assertEquals(listOf(HOME, CHILDREN, ACTIVITIES, REPORTS, MORE), TopLevelDestination.forRole(UserRole.INSTITUTIONAL_USER))
        assertEquals(listOf(HOME, USERS, REPORTS, AUDIT, MORE), TopLevelDestination.forRole(UserRole.ADMIN))
        // Administrative screens are never offered to non-admin roles.
        listOf(UserRole.PARENT_GUARDIAN, UserRole.CAREGIVER, UserRole.INSTITUTIONAL_USER).forEach { role ->
            assertFalse(TopLevelDestination.forRole(role).any { it == USERS || it == AUDIT })
        }
        // The child screen offers editing to a parent but not to a caregiver.
        val child = env.child(parentId, "Amani Mukasa", caregiverId = caregiverId)
        suspend fun canEdit(): Boolean {
            val vm = ChildDetailViewModel(env.children, env.activities, env.reminders, env.tasks, env.session, SavedStateHandle(mapOf("childId" to child)))
            return vm.uiState.awaitState { !it.loading }.canEdit
        }
        assertTrue(canEdit())
        env.signIn(caregiverId, "Mary Nabirye", UserRole.CAREGIVER)
        assertFalse(canEdit())
    }

    @Test
    fun it06_cascadingDeleteWithAudit() = runBlocking {
        val child = env.child(parentId, "Amani Mukasa", caregiverId = caregiverId)
        val act = env.activity(child, parentId)
        env.reminder(child, parentId, System.currentTimeMillis() + DAY, activityId = act)
        env.task(child, caregiverId, parentId)
        val vm = ChildEditorViewModel(env.children, env.users, env.session, env.audit, SavedStateHandle(mapOf("childId" to child)))
        vm.uiState.awaitState { !it.loading }
        vm.delete()
        vm.uiState.awaitState { it.deleted }
        assertTrue(env.activities.getActivitiesForChild(child).first().isEmpty())
        assertTrue(env.reminders.getRemindersForChild(child).first().isEmpty())
        assertTrue(env.tasks.getTasksForChild(child).first().isEmpty())
        val log = env.auditRepo.getAllLogs().first()
        assertTrue(log.any { it.affectedEntity == "ChildProfile" && it.affectedRecordId == child })
    }

    @Test
    fun it07_creatorAttributionAfterLogin() = runBlocking {
        env.session.clearSession()
        val auth = AuthViewModel(env.users, env.session, env.audit)
        auth.setCategory("Parent / Guardian")
        auth.onField(AuthField.NAME, "Peter Wasswa")
        auth.onField(AuthField.EMAIL, "peter@example.com")
        auth.onField(AuthField.PHONE, "+256772555555")
        auth.onField(AuthField.PASSWORD, "Password123")
        auth.onField(AuthField.CONFIRM, "Password123")
        auth.register()
        auth.uiState.awaitState { it.success }
        val peter = env.users.getUserByEmail("peter@example.com")!!.id
        val child = env.child(peter, "Joel Wasswa")
        val vm = ActivityEditorViewModel(env.activities, env.children, env.session, env.audit, SavedStateHandle(mapOf("childId" to child)))
        vm.uiState.awaitState { !it.loading }
        vm.onTitle("Football practice")
        vm.save()
        vm.uiState.awaitState { it.done }
        assertEquals(peter, env.activities.getActivitiesForChild(child).first().single().createdByUserId)
    }

    @Test
    fun it08_referralDirectoryWithOtherModules() = runBlocking {
        // The seeder only runs on an empty database, so use one without the users created in setUp.
        val seeded = TestEnv()
        try {
            DatabaseSeeder.seedIfEmpty(seeded.db)
            val seededChildren = seeded.children.count()
            val vm = ReferralViewModel(seeded.referrals)
            val state = vm.uiState.awaitState { !it.loading }
            assertTrue(state.emergency.isNotEmpty())
            assertEquals(seeded.referrals.count(), state.emergency.size + state.others.size)
            // Loading the directory did not change other modules' data.
            assertEquals(seededChildren, seeded.children.count())
            assertTrue(seeded.reminders.getAllRemindersWithDetails().first().isNotEmpty())
        } finally {
            seeded.db.close()
        }
    }

    @Test
    fun it09_reportAggregatesAcrossChildren() = runBlocking {
        val a = env.child(parentId, "Amani Mukasa")
        val b = env.child(parentId, "Brian Mukasa")
        val now = System.currentTimeMillis()
        env.activity(a, parentId, at = now - DAY, category = ActivityCategory.SCHOOL)
        env.activity(b, parentId, at = now - DAY, category = ActivityCategory.SCHOOL)
        env.activity(b, parentId, at = now - 2 * DAY, category = ActivityCategory.HEALTH)
        env.reminder(a, parentId, now - DAY, status = ReminderStatus.MISSED)
        env.reminder(b, parentId, now - DAY, status = ReminderStatus.MISSED)
        env.reminder(b, parentId, now - DAY, status = ReminderStatus.COMPLETED)
        env.task(a, caregiverId, parentId, status = TaskStatus.COMPLETED)
        env.task(b, caregiverId, parentId, status = TaskStatus.PENDING)
        val vm = ReportsViewModel(env.activities, env.reminders, env.tasks, env.session)
        val s = vm.uiState.awaitState { !it.loading && it.activityTotal > 0 }
        assertEquals(3, s.activityTotal)
        assertEquals(2, s.activityByCategory.first { it.label == "SCHOOL" }.count)
        assertEquals(2, s.missedReminders)
        val mary = s.caregiverCompletion.single()
        assertEquals(2, mary.total)
        assertEquals(1, mary.completed)
    }

    @Test
    fun it10_remindersAreReRegisteredAfterReboot() = runBlocking {
        val child = env.child(parentId, "Amani Mukasa")
        val now = System.currentTimeMillis()
        val future = env.reminder(child, parentId, now + 2 * HOUR)
        val past = env.reminder(child, parentId, now - 2 * HOUR)
        val muted = env.reminder(child, parentId, now + 3 * HOUR, notify = false)
        val codes = mapOf(future to 900_101, past to 900_102, muted to 900_103)
        codes.forEach { (id, code) ->
            env.reminders.updateReminder(env.reminders.getReminderById(id)!!.copy(notificationRequestCode = code))
            ReminderScheduler.cancel(appContext, code)
        }
        // Same steps BootReceiver runs on BOOT_COMPLETED (alarms are cleared by a reboot).
        val schedulable = env.reminders.getSchedulableReminders(now).map { it.reminder }
        assertEquals(listOf(future), schedulable.map { it.id })
        ReminderScheduler.rescheduleAll(appContext, schedulable)
        assertTrue(alarmExists(900_101))
        assertFalse(alarmExists(900_102))
        assertFalse(alarmExists(900_103))
        codes.values.forEach { ReminderScheduler.cancel(appContext, it) }
    }
}
