package com.example.parentapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.parentapp.domain.model.AuditAction

@Entity(
    tableName = "audit_logs",
    indices = [Index("userId"), Index("createdAt")],
)
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val actorName: String = "",
    val actionType: AuditAction,
    val affectedEntity: String,
    val affectedRecordId: Long,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
)
