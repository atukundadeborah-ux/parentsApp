package com.example.parentapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.parentapp.domain.model.TaskStatus

@Entity(
    tableName = "caregiver_tasks",
    indices = [Index("childId"), Index("assignedToCaregiverId"), Index("assignedByUserId"), Index("dueDateTime")],
)
data class CaregiverTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val assignedToCaregiverId: Long,
    val assignedByUserId: Long,
    val title: String,
    val description: String,
    val dueDateTime: Long,
    val status: TaskStatus,
    val followUpNotes: String? = null,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
