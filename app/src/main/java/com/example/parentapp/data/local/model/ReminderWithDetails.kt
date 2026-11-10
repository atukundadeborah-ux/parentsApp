package com.example.parentapp.data.local.model

import androidx.room.Embedded
import com.example.parentapp.data.local.entity.Reminder

data class ReminderWithDetails(
    @Embedded val reminder: Reminder,
    val childName: String,
    val activityTitle: String?
)
