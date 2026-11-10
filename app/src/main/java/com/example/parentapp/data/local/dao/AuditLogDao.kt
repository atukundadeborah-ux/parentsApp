package com.example.parentapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.parentapp.data.local.entity.AuditLog
import com.example.parentapp.domain.model.AuditAction
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Insert
    suspend fun insertLog(log: AuditLog)

    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 200): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC")
    fun getAllLogs(): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs WHERE userId = :userId ORDER BY createdAt DESC")
    fun getLogsByUser(userId: Long): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs WHERE actionType = :action ORDER BY createdAt DESC")
    fun getLogsByAction(action: AuditAction): Flow<List<AuditLog>>
}
