package com.example.parentapp.data.local.dao

import androidx.room.*
import com.example.parentapp.data.local.entity.Reminder
import com.example.parentapp.data.local.model.LabelCount
import com.example.parentapp.data.local.model.ReminderWithDetails
import com.example.parentapp.domain.model.Priority
import com.example.parentapp.domain.model.ReminderCategory
import com.example.parentapp.domain.model.ReminderStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("SELECT * FROM reminders WHERE childId = :childId ORDER BY reminderDateTime ASC")
    fun getRemindersForChild(childId: Long): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): Reminder?

    @Query("SELECT * FROM reminders WHERE createdByUserId = :userId ORDER BY reminderDateTime ASC")
    fun getRemindersByUser(userId: Long): Flow<List<Reminder>>

    @Query("""
        SELECT reminders.*, child_profiles.fullName AS childName, activities.title AS activityTitle
        FROM reminders
        INNER JOIN child_profiles ON child_profiles.id = reminders.childId
        LEFT JOIN activities ON activities.id = reminders.activityId
        WHERE reminders.id = :id
    """)
    suspend fun getReminderWithDetailsById(id: Long): ReminderWithDetails?

    @Query("""
        SELECT reminders.*, child_profiles.fullName AS childName, activities.title AS activityTitle
        FROM reminders
        INNER JOIN child_profiles ON child_profiles.id = reminders.childId
        LEFT JOIN activities ON activities.id = reminders.activityId
        ORDER BY reminders.reminderDateTime ASC
    """)
    fun getAllRemindersWithDetails(): Flow<List<ReminderWithDetails>>

    @Query("""
        SELECT reminders.*, child_profiles.fullName AS childName, activities.title AS activityTitle
        FROM reminders
        INNER JOIN child_profiles ON child_profiles.id = reminders.childId
        LEFT JOIN activities ON activities.id = reminders.activityId
        WHERE child_profiles.createdByUserId = :userId
        ORDER BY reminders.reminderDateTime ASC
    """)
    fun getRemindersForGuardian(userId: Long): Flow<List<ReminderWithDetails>>

    @Query("""
        SELECT reminders.*, child_profiles.fullName AS childName, activities.title AS activityTitle
        FROM reminders
        INNER JOIN child_profiles ON child_profiles.id = reminders.childId
        LEFT JOIN activities ON activities.id = reminders.activityId
        WHERE child_profiles.institutionId = :institutionId
        ORDER BY reminders.reminderDateTime ASC
    """)
    fun getRemindersForInstitution(institutionId: Long): Flow<List<ReminderWithDetails>>

    @Query("""
        SELECT reminders.*, child_profiles.fullName AS childName, activities.title AS activityTitle
        FROM reminders
        INNER JOIN child_profiles ON child_profiles.id = reminders.childId
        LEFT JOIN activities ON activities.id = reminders.activityId
        WHERE reminders.childId IN (
            SELECT childId FROM caregiver_tasks WHERE assignedToCaregiverId = :caregiverId
        )
        ORDER BY reminders.reminderDateTime ASC
    """)
    fun getRemindersForCaregiver(caregiverId: Long): Flow<List<ReminderWithDetails>>

    @Query("""
        SELECT reminders.*, child_profiles.fullName AS childName, activities.title AS activityTitle
        FROM reminders
        INNER JOIN child_profiles ON child_profiles.id = reminders.childId
        LEFT JOIN activities ON activities.id = reminders.activityId
        WHERE reminders.childId = :childId
        ORDER BY reminders.reminderDateTime ASC
    """)
    fun getReminderRecordsByChild(childId: Long): Flow<List<ReminderWithDetails>>

    @Query("""
        SELECT reminders.*, child_profiles.fullName AS childName, activities.title AS activityTitle
        FROM reminders
        INNER JOIN child_profiles ON child_profiles.id = reminders.childId
        LEFT JOIN activities ON activities.id = reminders.activityId
        WHERE reminders.category = :category
        ORDER BY reminders.reminderDateTime ASC
    """)
    fun getRemindersByCategory(category: ReminderCategory): Flow<List<ReminderWithDetails>>

    @Query("""
        SELECT reminders.*, child_profiles.fullName AS childName, activities.title AS activityTitle
        FROM reminders
        INNER JOIN child_profiles ON child_profiles.id = reminders.childId
        LEFT JOIN activities ON activities.id = reminders.activityId
        WHERE reminders.priority = :priority
        ORDER BY reminders.reminderDateTime ASC
    """)
    fun getRemindersByPriority(priority: Priority): Flow<List<ReminderWithDetails>>

    @Query("""
        SELECT reminders.*, child_profiles.fullName AS childName, activities.title AS activityTitle
        FROM reminders
        INNER JOIN child_profiles ON child_profiles.id = reminders.childId
        LEFT JOIN activities ON activities.id = reminders.activityId
        WHERE reminders.status = :status
        ORDER BY reminders.reminderDateTime ASC
    """)
    fun getRemindersByStatus(status: ReminderStatus): Flow<List<ReminderWithDetails>>

    @Query("""
        SELECT reminders.*, child_profiles.fullName AS childName, activities.title AS activityTitle
        FROM reminders
        INNER JOIN child_profiles ON child_profiles.id = reminders.childId
        LEFT JOIN activities ON activities.id = reminders.activityId
        WHERE reminders.reminderDateTime BETWEEN :startDateTime AND :endDateTime
        ORDER BY reminders.reminderDateTime ASC
    """)
    fun getRemindersForToday(startDateTime: Long, endDateTime: Long): Flow<List<ReminderWithDetails>>

    @Query("""
        SELECT reminders.*, child_profiles.fullName AS childName, activities.title AS activityTitle
        FROM reminders
        INNER JOIN child_profiles ON child_profiles.id = reminders.childId
        LEFT JOIN activities ON activities.id = reminders.activityId
        WHERE reminders.reminderDateTime > :now AND reminders.status = 'PENDING'
        ORDER BY reminders.reminderDateTime ASC
    """)
    fun getUpcomingReminders(now: Long): Flow<List<ReminderWithDetails>>

    @Query("""
        SELECT reminders.*, child_profiles.fullName AS childName, activities.title AS activityTitle
        FROM reminders
        INNER JOIN child_profiles ON child_profiles.id = reminders.childId
        LEFT JOIN activities ON activities.id = reminders.activityId
        WHERE reminders.status = 'MISSED' AND reminders.reminderDateTime <= :now
        ORDER BY reminders.reminderDateTime DESC
    """)
    fun getMissedReminders(now: Long): Flow<List<ReminderWithDetails>>

    @Query("""
        SELECT reminders.*, child_profiles.fullName AS childName, activities.title AS activityTitle
        FROM reminders
        INNER JOIN child_profiles ON child_profiles.id = reminders.childId
        LEFT JOIN activities ON activities.id = reminders.activityId
        WHERE reminders.title LIKE '%' || :query || '%'
        ORDER BY reminders.reminderDateTime ASC
    """)
    fun searchRemindersByTitle(query: String): Flow<List<ReminderWithDetails>>

    @Query("UPDATE reminders SET status = 'MISSED', updatedAt = :updatedAt WHERE reminderDateTime < :now AND status = 'PENDING'")
    suspend fun markPastPendingRemindersMissed(now: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE reminders SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: ReminderStatus, updatedAt: Long = System.currentTimeMillis())

    // ---- Reports / aggregates ----

    @Query("""
        SELECT status AS label, COUNT(*) AS count FROM reminders
        WHERE reminderDateTime BETWEEN :start AND :end
        GROUP BY status ORDER BY count DESC
    """)
    fun countByStatusInRange(start: Long, end: Long): Flow<List<LabelCount>>

    @Query("SELECT COUNT(*) FROM reminders WHERE status = 'MISSED' AND reminderDateTime BETWEEN :start AND :end")
    fun missedCountInRange(start: Long, end: Long): Flow<Int>

    @Query("""
        SELECT reminders.*, child_profiles.fullName AS childName, activities.title AS activityTitle
        FROM reminders
        INNER JOIN child_profiles ON child_profiles.id = reminders.childId
        LEFT JOIN activities ON activities.id = reminders.activityId
        WHERE reminders.notificationEnabled = 1 AND reminders.status = 'PENDING'
              AND reminders.reminderDateTime > :now
        ORDER BY reminders.reminderDateTime ASC
    """)
    suspend fun getSchedulableReminders(now: Long): List<ReminderWithDetails>
}
