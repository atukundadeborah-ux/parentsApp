package com.example.parentapp.data.local

import androidx.room.TypeConverter
import com.example.parentapp.domain.model.AccountStatus
import com.example.parentapp.domain.model.ActivityCategory
import com.example.parentapp.domain.model.ActivityStatus
import com.example.parentapp.domain.model.AuditAction
import com.example.parentapp.domain.model.Priority
import com.example.parentapp.domain.model.ProfileStatus
import com.example.parentapp.domain.model.ReferralCategory
import com.example.parentapp.domain.model.ReminderCategory
import com.example.parentapp.domain.model.ReminderStatus
import com.example.parentapp.domain.model.TaskStatus
import com.example.parentapp.domain.model.TipCategory
import com.example.parentapp.domain.model.UserRole

/** Enum <-> String Room converters. Each stores the enum `name`. */
class Converters {

    @TypeConverter fun fromUserRole(v: UserRole): String = v.name
    @TypeConverter fun toUserRole(v: String): UserRole = UserRole.valueOf(v)

    @TypeConverter fun fromAccountStatus(v: AccountStatus): String = v.name
    @TypeConverter fun toAccountStatus(v: String): AccountStatus = AccountStatus.valueOf(v)

    @TypeConverter fun fromProfileStatus(v: ProfileStatus): String = v.name
    @TypeConverter fun toProfileStatus(v: String): ProfileStatus = ProfileStatus.valueOf(v)

    @TypeConverter fun fromActivityCategory(v: ActivityCategory): String = v.name
    @TypeConverter fun toActivityCategory(v: String): ActivityCategory = ActivityCategory.valueOf(v)

    @TypeConverter fun fromActivityStatus(v: ActivityStatus): String = v.name
    @TypeConverter fun toActivityStatus(v: String): ActivityStatus = ActivityStatus.valueOf(v)

    @TypeConverter fun fromReminderCategory(v: ReminderCategory): String = v.name
    @TypeConverter fun toReminderCategory(v: String): ReminderCategory = ReminderCategory.valueOf(v)

    @TypeConverter fun fromReminderStatus(v: ReminderStatus): String = v.name
    @TypeConverter fun toReminderStatus(v: String): ReminderStatus = ReminderStatus.valueOf(v)

    @TypeConverter fun fromPriority(v: Priority): String = v.name
    @TypeConverter fun toPriority(v: String): Priority = Priority.valueOf(v)

    @TypeConverter fun fromTaskStatus(v: TaskStatus): String = v.name
    @TypeConverter fun toTaskStatus(v: String): TaskStatus = TaskStatus.valueOf(v)

    @TypeConverter fun fromTipCategory(v: TipCategory): String = v.name
    @TypeConverter fun toTipCategory(v: String): TipCategory = TipCategory.valueOf(v)

    @TypeConverter fun fromReferralCategory(v: ReferralCategory): String = v.name
    @TypeConverter fun toReferralCategory(v: String): ReferralCategory = ReferralCategory.valueOf(v)

    @TypeConverter fun fromAuditAction(v: AuditAction): String = v.name
    @TypeConverter fun toAuditAction(v: String): AuditAction = AuditAction.valueOf(v)
}
