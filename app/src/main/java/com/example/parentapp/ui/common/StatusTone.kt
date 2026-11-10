package com.example.parentapp.ui.common

import com.example.parentapp.core.designsystem.component.BadgeTone
import com.example.parentapp.domain.model.ActivityStatus
import com.example.parentapp.domain.model.Priority
import com.example.parentapp.domain.model.ReminderStatus
import com.example.parentapp.domain.model.TaskStatus

fun ActivityStatus.tone(): BadgeTone = when (this) {
    ActivityStatus.PENDING -> BadgeTone.INFO
    ActivityStatus.COMPLETED -> BadgeTone.SUCCESS
    ActivityStatus.CANCELLED -> BadgeTone.NEUTRAL
}

fun ReminderStatus.tone(): BadgeTone = when (this) {
    ReminderStatus.PENDING -> BadgeTone.INFO
    ReminderStatus.COMPLETED -> BadgeTone.SUCCESS
    ReminderStatus.MISSED -> BadgeTone.DANGER
    ReminderStatus.DISMISSED -> BadgeTone.NEUTRAL
    ReminderStatus.SNOOZED -> BadgeTone.WARNING
}

fun TaskStatus.tone(): BadgeTone = when (this) {
    TaskStatus.PENDING -> BadgeTone.INFO
    TaskStatus.COMPLETED -> BadgeTone.SUCCESS
    TaskStatus.MISSED -> BadgeTone.DANGER
    TaskStatus.REQUIRES_FOLLOW_UP -> BadgeTone.WARNING
}

fun Priority.tone(): BadgeTone = when (this) {
    Priority.LOW -> BadgeTone.NEUTRAL
    Priority.MEDIUM -> BadgeTone.INFO
    Priority.HIGH -> BadgeTone.WARNING
    Priority.URGENT -> BadgeTone.DANGER
}
