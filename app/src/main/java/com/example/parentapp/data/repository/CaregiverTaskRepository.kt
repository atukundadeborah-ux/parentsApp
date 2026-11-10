package com.example.parentapp.data.repository

import com.example.parentapp.data.local.dao.CaregiverTaskDao
import com.example.parentapp.data.local.entity.CaregiverTask
import com.example.parentapp.data.local.model.CaregiverCompletion
import com.example.parentapp.data.local.model.LabelCount
import com.example.parentapp.data.local.model.TaskWithNames
import com.example.parentapp.domain.model.TaskStatus
import com.example.parentapp.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

class CaregiverTaskRepository(private val dao: CaregiverTaskDao) {
    suspend fun insertTask(task: CaregiverTask): Long = dao.insertTask(task)
    suspend fun updateTask(task: CaregiverTask) = dao.updateTask(task)
    suspend fun deleteTask(task: CaregiverTask) = dao.deleteTask(task)
    suspend fun getTaskById(id: Long): CaregiverTask? = dao.getTaskById(id)
    suspend fun getTaskWithNamesById(id: Long): TaskWithNames? = dao.getTaskWithNamesById(id)

    fun getTasksForCaregiver(caregiverId: Long): Flow<List<CaregiverTask>> = dao.getTasksForCaregiver(caregiverId)
    fun getTasksForChild(childId: Long): Flow<List<CaregiverTask>> = dao.getTasksForChild(childId)
    fun getTasksWithNamesForChild(childId: Long): Flow<List<TaskWithNames>> = dao.getTasksWithNamesForChild(childId)
    fun getTasksAssignedToCaregiver(caregiverId: Long): Flow<List<TaskWithNames>> = dao.getTasksAssignedToCaregiver(caregiverId)
    fun getTasksAssignedByUser(userId: Long): Flow<List<TaskWithNames>> = dao.getTasksAssignedByUser(userId)

    suspend fun updateStatus(id: Long, status: TaskStatus, notes: String?, completedAt: Long?) =
        dao.updateStatus(id, status, notes, completedAt)
    suspend fun markPastPendingTasksMissed(now: Long = System.currentTimeMillis()) =
        dao.markPastPendingTasksMissed(now)

    fun countByStatus(): Flow<List<LabelCount>> = dao.countByStatus()
    fun caregiverCompletionReport(): Flow<List<CaregiverCompletion>> = dao.caregiverCompletionReport()

    fun getTasksVisibleToUser(role: UserRole?, userId: Long?, institutionId: Long?): Flow<List<TaskWithNames>> =
        when (role) {
            UserRole.CAREGIVER -> dao.getTasksAssignedToCaregiver(userId ?: -1L)
            UserRole.PARENT_GUARDIAN -> dao.getTasksAssignedByUser(userId ?: -1L)
            UserRole.INSTITUTIONAL_USER -> dao.getTasksForInstitution(institutionId ?: -1L)
            UserRole.ADMIN, null -> dao.getAllTasksWithNames()
        }
}
