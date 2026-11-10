package com.example.parentapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.parentapp.domain.model.Priority
import com.example.parentapp.domain.model.ReminderCategory
import com.example.parentapp.domain.model.ReminderStatus

@Entity(
    tableName = "reminders",
    indices = [Index("childId"), Index("activityId"), Index("reminderDateTime"), Index("createdByUserId")],
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val activityId: Long? = null,
    val title: String,
    val category: ReminderCategory,
    /** The moment the underlying event happens. */
    val reminderDateTime: Long,
    /** Minutes before [reminderDateTime] that the alert fires (0 = at time). */
    val leadTimeMinutes: Int = 0,
    val priority: Priority,
    val status: ReminderStatus,
    val notificationEnabled: Boolean,
    val notificationRequestCode: Int? = null,
    val createdByUserId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
) {
    /** Actual epoch-millis the notification should be delivered. */
    val triggerAtMillis: Long get() = reminderDateTime - leadTimeMinutes * 60_000L
}
