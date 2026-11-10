package com.example.parentapp.data.local.model

import androidx.room.Embedded
import com.example.parentapp.data.local.entity.CaregiverTask

/** A caregiver task joined with the display names it references. */
data class TaskWithNames(
    @Embedded val task: CaregiverTask,
    val childName: String,
    val caregiverName: String?,
    val assignedByName: String?,
)
