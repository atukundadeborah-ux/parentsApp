package com.example.parentapp.data

import com.example.parentapp.data.local.entity.Reminder
import com.example.parentapp.domain.model.Priority
import com.example.parentapp.domain.model.ReminderCategory
import com.example.parentapp.domain.model.ReminderLeadTime
import com.example.parentapp.domain.model.ReminderStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderModelTest {

    private fun reminder(eventAt: Long, lead: Int) = Reminder(
        childId = 1, title = "t", category = ReminderCategory.MEDICATION,
        reminderDateTime = eventAt, leadTimeMinutes = lead, priority = Priority.MEDIUM,
        status = ReminderStatus.PENDING, notificationEnabled = true, createdByUserId = 1,
    )

    @Test
    fun `trigger time subtracts the lead minutes`() {
        val eventAt = 1_700_000_000_000L
        assertEquals(eventAt, reminder(eventAt, 0).triggerAtMillis)
        assertEquals(eventAt - 30 * 60_000L, reminder(eventAt, 30).triggerAtMillis)
        assertEquals(eventAt - 24 * 60 * 60_000L, reminder(eventAt, 1_440).triggerAtMillis)
    }

    @Test
    fun `lead time enum maps minutes both ways`() {
        assertEquals(ReminderLeadTime.MIN_30, ReminderLeadTime.fromMinutes(30))
        assertEquals(ReminderLeadTime.DAY_1, ReminderLeadTime.fromMinutes(1_440))
        assertEquals(ReminderLeadTime.AT_TIME, ReminderLeadTime.fromMinutes(999))
    }
}
