package com.example.parentapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.parentapp.data.local.entity.CaregiverTask
import com.example.parentapp.data.local.model.CaregiverCompletion
import com.example.parentapp.data.local.model.LabelCount
import com.example.parentapp.data.local.model.TaskWithNames
import com.example.parentapp.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow

private const val TASK_JOIN =
    "SELECT t.*, c.fullName AS childName, cg.fullName AS caregiverName, ab.fullName AS assignedByName " +
        "FROM caregiver_tasks t " +
        "INNER JOIN child_profiles c ON c.id = t.childId " +
        "LEFT JOIN user_accounts cg ON cg.id = t.assignedToCaregiverId " +
        "LEFT JOIN user_accounts ab ON ab.id = t.assignedByUserId "

@Dao
interface CaregiverTaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: CaregiverTask): Long

    @Update
    suspend fun updateTask(task: CaregiverTask)

    @Delete
    suspend fun deleteTask(task: CaregiverTask)

    @Query("SELECT * FROM caregiver_tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): CaregiverTask?

    @Query("SELECT * FROM caregiver_tasks WHERE assignedToCaregiverId = :caregiverId ORDER BY dueDateTime ASC")
    fun getTasksForCaregiver(caregiverId: Long): Flow<List<CaregiverTask>>

    @Query("SELECT * FROM caregiver_tasks WHERE childId = :childId ORDER BY dueDateTime ASC")
    fun getTasksForChild(childId: Long): Flow<List<CaregiverTask>>

    @Query(TASK_JOIN + "WHERE t.id = :id")
    suspend fun getTaskWithNamesById(id: Long): TaskWithNames?

    @Query(TASK_JOIN + "ORDER BY t.dueDateTime ASC")
    fun getAllTasksWithNames(): Flow<List<TaskWithNames>>

    @Query(TASK_JOIN + "WHERE t.assignedToCaregiverId = :caregiverId ORDER BY t.dueDateTime ASC")
    fun getTasksAssignedToCaregiver(caregiverId: Long): Flow<List<TaskWithNames>>

    @Query(TASK_JOIN + "WHERE t.assignedByUserId = :userId ORDER BY t.dueDateTime ASC")
    fun getTasksAssignedByUser(userId: Long): Flow<List<TaskWithNames>>

    @Query(TASK_JOIN + "WHERE c.institutionId = :institutionId ORDER BY t.dueDateTime ASC")
    fun getTasksForInstitution(institutionId: Long): Flow<List<TaskWithNames>>

    @Query(TASK_JOIN + "WHERE t.childId = :childId ORDER BY t.dueDateTime ASC")
    fun getTasksWithNamesForChild(childId: Long): Flow<List<TaskWithNames>>

    @Query("UPDATE caregiver_tasks SET status = :status, followUpNotes = :notes, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(
        id: Long,
        status: TaskStatus,
        notes: String?,
        completedAt: Long?,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query("UPDATE caregiver_tasks SET status = 'MISSED', updatedAt = :updatedAt WHERE status = 'PENDING' AND dueDateTime < :now")
    suspend fun markPastPendingTasksMissed(now: Long, updatedAt: Long = System.currentTimeMillis())

    // ---- Reports / aggregates ----

    @Query("SELECT status AS label, COUNT(*) AS count FROM caregiver_tasks GROUP BY status ORDER BY count DESC")
    fun countByStatus(): Flow<List<LabelCount>>

    @Query(
        "SELECT cg.id AS caregiverId, cg.fullName AS caregiverName, " +
            "COUNT(t.id) AS total, " +
            "IFNULL(SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS completed, " +
            "IFNULL(SUM(CASE WHEN t.status = 'MISSED' THEN 1 ELSE 0 END), 0) AS missed " +
            "FROM user_accounts cg " +
            "LEFT JOIN caregiver_tasks t ON t.assignedToCaregiverId = cg.id " +
            "WHERE cg.role = 'CAREGIVER' " +
            "GROUP BY cg.id, cg.fullName ORDER BY cg.fullName ASC",
    )
    fun caregiverCompletionReport(): Flow<List<CaregiverCompletion>>
}
