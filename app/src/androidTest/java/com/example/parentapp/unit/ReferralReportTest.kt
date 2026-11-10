package com.example.parentapp.unit

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.parentapp.data.local.DatabaseSeeder
import com.example.parentapp.domain.model.ActivityCategory
import com.example.parentapp.domain.model.ReferralCategory
import com.example.parentapp.domain.model.ReminderStatus
import com.example.parentapp.domain.model.TaskStatus
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.testing.DAY
import com.example.parentapp.testing.TestEnv
import com.example.parentapp.testing.awaitState
import com.example.parentapp.ui.reports.ReportRange
import com.example.parentapp.ui.reports.ReportsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** UT-36 – UT-42: Referral Directory and Reports module. */
@RunWith(AndroidJUnit4::class)
class ReferralReportTest {

    private lateinit var env: TestEnv

    @Before
    fun setUp() { env = TestEnv() }

    @After
    fun tearDown() = runBlocking { env.close() }

    @Test
    fun ut36_prePopulatedContactsAreDisplayed() = runBlocking {
        DatabaseSeeder.seedIfEmpty(env.db)
        val all = env.referrals.getAllContacts().first()
        assertTrue("expected the seeded directory, got ${all.size}", all.size >= 10)
        assertTrue(all.all { it.name.isNotBlank() })
        // Emergency lines must be diallable; local offices may be left blank for the user to fill in.
        assertTrue(all.filter { it.isEmergency }.all { it.phone.isNotBlank() })
    }

    @Test
    fun ut37_contactsAreFilteredByType() = runBlocking {
        DatabaseSeeder.seedIfEmpty(env.db)
        val legal = env.referrals.getContactsByCategory(ReferralCategory.LEGAL).first()
        assertTrue(legal.isNotEmpty())
        assertTrue(legal.all { it.category == ReferralCategory.LEGAL })
    }

    @Test
    fun ut38_sauti116IsPinnedAtTheTop() = runBlocking {
        DatabaseSeeder.seedIfEmpty(env.db)
        val first = env.referrals.getAllContacts().first().first()
        assertEquals("116", first.phone)
        assertTrue(first.name.contains("Sauti 116"))
    }

    @Test
    fun ut39_activityCountByTypeIsGenerated() = runBlocking {
        val p = env.user("Sarah", "p@example.com", UserRole.PARENT_GUARDIAN)
        val c = env.child(p)
        val now = System.currentTimeMillis()
        repeat(3) { env.activity(c, p, at = now - DAY, category = ActivityCategory.SCHOOL) }
        repeat(2) { env.activity(c, p, at = now - DAY, category = ActivityCategory.HEALTH) }
        val counts = env.activities.countByCategoryInRange(now - 7 * DAY, now).first().associate { it.label to it.count }
        assertEquals(mapOf("SCHOOL" to 3, "HEALTH" to 2), counts)
    }

    @Test
    fun ut40_missedReminderPercentageIsGenerated() = runBlocking {
        val p = env.user("Sarah", "p@example.com", UserRole.PARENT_GUARDIAN)
        val c = env.child(p)
        val now = System.currentTimeMillis()
        env.reminder(c, p, now - DAY, status = ReminderStatus.MISSED)
        env.reminder(c, p, now - DAY, status = ReminderStatus.COMPLETED)
        env.reminder(c, p, now - 2 * DAY, status = ReminderStatus.COMPLETED)
        env.reminder(c, p, now - 3 * DAY, status = ReminderStatus.DISMISSED)
        val start = now - 7 * DAY
        val missed = env.reminders.missedCountInRange(start, now).first()
        val total = env.reminders.countByStatusInRange(start, now).first().sumOf { it.count }
        assertEquals(1, missed)
        assertEquals(4, total)
        assertEquals(25.0, missed * 100.0 / total, 0.001)
    }

    @Test
    fun ut41_taskCompletionRateIsGenerated() = runBlocking {
        val p = env.user("Sarah", "p@example.com", UserRole.PARENT_GUARDIAN)
        val cg = env.user("Mary Nabirye", "cg@example.com", UserRole.CAREGIVER)
        val c = env.child(p)
        repeat(3) { env.task(c, cg, p, status = TaskStatus.COMPLETED) }
        env.task(c, cg, p, status = TaskStatus.MISSED)
        val row = env.tasks.caregiverCompletionReport().first().single()
        assertEquals(4, row.total)
        assertEquals(3, row.completed)
        assertEquals(1, row.missed)
        assertEquals(0.75f, row.completionRate, 0.0001f)
    }

    @Test
    fun ut42_reportsAreFilteredByDateRange() = runBlocking {
        val p = env.user("Sarah", "p@example.com", UserRole.PARENT_GUARDIAN)
        val c = env.child(p)
        val now = System.currentTimeMillis()
        env.activity(c, p, at = now - 2 * DAY)
        env.activity(c, p, at = now - 20 * DAY)
        env.activity(c, p, at = now - 60 * DAY)
        val vm = ReportsViewModel(env.activities, env.reminders, env.tasks, env.session)
        // awaitState subscribes to the WhileSubscribed StateFlow, which keeps it running.
        assertEquals(2, vm.uiState.awaitState { !it.loading && it.range == ReportRange.MONTH }.activityTotal)
        vm.onRange(ReportRange.WEEK)
        assertEquals(1, vm.uiState.awaitState { !it.loading && it.range == ReportRange.WEEK }.activityTotal)
        vm.onRange(ReportRange.QUARTER)
        assertEquals(3, vm.uiState.awaitState { !it.loading && it.range == ReportRange.QUARTER }.activityTotal)
    }
}
