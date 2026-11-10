package com.example.parentapp.data.audit

import com.example.parentapp.data.local.entity.AuditLog
import com.example.parentapp.data.repository.AuditLogRepository
import com.example.parentapp.domain.model.AuditAction

/**
 * Thin helper that records an [AuditLog] row for create / update / delete / auth events.
 * Call from repositories or ViewModels after a successful mutation.
 */
class AuditLogger(private val repository: AuditLogRepository) {

    suspend fun record(
        actorId: Long,
        actorName: String,
        action: AuditAction,
        entity: String,
        recordId: Long,
        description: String,
    ) {
        repository.insertLog(
            AuditLog(
                userId = actorId,
                actorName = actorName,
                actionType = action,
                affectedEntity = entity,
                affectedRecordId = recordId,
                description = description,
            ),
        )
    }

    suspend fun created(actorId: Long, actorName: String, entity: String, recordId: Long, description: String) =
        record(actorId, actorName, AuditAction.CREATE, entity, recordId, description)

    suspend fun updated(actorId: Long, actorName: String, entity: String, recordId: Long, description: String) =
        record(actorId, actorName, AuditAction.UPDATE, entity, recordId, description)

    suspend fun deleted(actorId: Long, actorName: String, entity: String, recordId: Long, description: String) =
        record(actorId, actorName, AuditAction.DELETE, entity, recordId, description)

    suspend fun auth(actorId: Long, actorName: String, action: AuditAction, description: String) =
        record(actorId, actorName, action, "UserAccount", actorId, description)
}
