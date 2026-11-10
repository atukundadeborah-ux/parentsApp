package com.example.parentapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.parentapp.domain.model.ActivityCategory
import com.example.parentapp.domain.model.ActivityStatus

@Entity(
    tableName = "activities",
    indices = [Index("childId"), Index("activityDateTime")],
)
data class Activity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val category: ActivityCategory,
    val title: String,
    val description: String,
    val activityDateTime: Long,
    val status: ActivityStatus,
    val createdByUserId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
)
