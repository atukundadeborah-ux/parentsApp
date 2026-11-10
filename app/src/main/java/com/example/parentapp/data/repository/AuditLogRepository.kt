package com.example.parentapp.data.repository

import com.example.parentapp.data.local.dao.AuditLogDao
import com.example.parentapp.data.local.entity.AuditLog
import com.example.parentapp.domain.model.AuditAction
import kotlinx.coroutines.flow.Flow

class AuditLogRepository(private val auditLogDao: AuditLogDao) {
    suspend fun insertLog(log: AuditLog) = auditLogDao.insertLog(log)
    fun getRecentLogs(limit: Int = 200): Flow<List<AuditLog>> = auditLogDao.getRecentLogs(limit)
    fun getAllLogs(): Flow<List<AuditLog>> = auditLogDao.getAllLogs()
    fun getLogsByUser(userId: Long): Flow<List<AuditLog>> = auditLogDao.getLogsByUser(userId)
    fun getLogsByAction(action: AuditAction): Flow<List<AuditLog>> = auditLogDao.getLogsByAction(action)
}
