package com.example.parentapp.data.repository

import com.example.parentapp.data.local.dao.ReminderDao
import com.example.parentapp.data.local.entity.Reminder
import com.example.parentapp.data.local.model.LabelCount
import com.example.parentapp.data.local.model.ReminderWithDetails
import com.example.parentapp.domain.model.Priority
import com.example.parentapp.domain.model.ReminderCategory
import com.example.parentapp.domain.model.ReminderStatus
import com.example.parentapp.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val reminderDao: ReminderDao) {
    suspend fun insertReminder(reminder: Reminder): Long = reminderDao.insertReminder(reminder)
    suspend fun updateReminder(reminder: Reminder) = reminderDao.updateReminder(reminder)
    suspend fun deleteReminder(reminder: Reminder) = reminderDao.deleteReminder(reminder)
    suspend fun getReminderById(id: Long): Reminder? = reminderDao.getReminderById(id)
    suspend fun getReminderWithDetailsById(id: Long): ReminderWithDetails? = reminderDao.getReminderWithDetailsById(id)
    fun getRemindersForChild(childId: Long): Flow<List<Reminder>> = reminderDao.getRemindersForChild(childId)
    fun getRemindersByUser(userId: Long): Flow<List<Reminder>> = reminderDao.getRemindersByUser(userId)
    fun getAllRemindersWithDetails(): Flow<List<ReminderWithDetails>> = reminderDao.getAllRemindersWithDetails()
    fun getReminderRecordsByChild(childId: Long): Flow<List<ReminderWithDetails>> = reminderDao.getReminderRecordsByChild(childId)
    fun getRemindersByCategory(category: ReminderCategory): Flow<List<ReminderWithDetails>> = reminderDao.getRemindersByCategory(category)
    fun getRemindersByPriority(priority: Priority): Flow<List<ReminderWithDetails>> = reminderDao.getRemindersByPriority(priority)
    fun getRemindersByStatus(status: ReminderStatus): Flow<List<ReminderWithDetails>> = reminderDao.getRemindersByStatus(status)
    fun getRemindersForToday(start: Long, end: Long): Flow<List<ReminderWithDetails>> = reminderDao.getRemindersForToday(start, end)
    fun getUpcomingReminders(now: Long = System.currentTimeMillis()): Flow<List<ReminderWithDetails>> = reminderDao.getUpcomingReminders(now)
    fun getMissedReminders(now: Long = System.currentTimeMillis()): Flow<List<ReminderWithDetails>> = reminderDao.getMissedReminders(now)
    fun searchRemindersByTitle(query: String): Flow<List<ReminderWithDetails>> = reminderDao.searchRemindersByTitle(query)

    suspend fun updateStatus(id: Long, status: ReminderStatus) = reminderDao.updateStatus(id, status)
    suspend fun markPastPendingRemindersMissed(now: Long = System.currentTimeMillis()) =
        reminderDao.markPastPendingRemindersMissed(now)
    suspend fun getSchedulableReminders(now: Long = System.currentTimeMillis()): List<ReminderWithDetails> =
        reminderDao.getSchedulableReminders(now)

    fun countByStatusInRange(start: Long, end: Long): Flow<List<LabelCount>> = reminderDao.countByStatusInRange(start, end)
    fun missedCountInRange(start: Long, end: Long): Flow<Int> = reminderDao.missedCountInRange(start, end)

    fun getRemindersVisibleToUser(
        role: UserRole?,
        userId: Long?,
        institutionId: Long?,
    ): Flow<List<ReminderWithDetails>> = when (role) {
        UserRole.PARENT_GUARDIAN -> reminderDao.getRemindersForGuardian(userId ?: -1L)
        UserRole.INSTITUTIONAL_USER -> reminderDao.getRemindersForInstitution(institutionId ?: -1L)
        UserRole.CAREGIVER -> reminderDao.getRemindersForCaregiver(userId ?: -1L)
        UserRole.ADMIN, null -> reminderDao.getAllRemindersWithDetails()
    }
}
