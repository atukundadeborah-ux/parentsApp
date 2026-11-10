package com.example.parentapp.domain.model

/**
 * Domain enums shared across the data and UI layers. Every enum carries a human [label]
 * for display; persistence stores the `name` (see [com.example.parentapp.data.local.Converters]).
 */

enum class UserRole(val label: String) {
    PARENT_GUARDIAN("Parent / Guardian"),
    INSTITUTIONAL_USER("Institutional user"),
    CAREGIVER("Caregiver"),
    ADMIN("Administrator"),
    ;

    companion object {
        /** Maps the onboarding "category" selection to a role (order matters: most specific first). */
        fun fromCategory(category: String): UserRole {
            val c = category.trim().lowercase()
            return when {
                c.contains("admin") -> ADMIN
                c.contains("institution") || c.contains("school") || c.contains("orphanage") || c.contains("staff") -> INSTITUTIONAL_USER
                c.contains("caregiver") || c.contains("nanny") -> CAREGIVER
                c.contains("parent") || c.contains("guardian") -> PARENT_GUARDIAN
                else -> PARENT_GUARDIAN
            }
        }
    }
}

enum class AccountStatus(val label: String) {
    ACTIVE("Active"),
    SUSPENDED("Suspended"),
}

enum class ProfileStatus(val label: String) {
    ACTIVE("Active"),
    ARCHIVED("Archived"),
}

enum class ActivityCategory(val label: String) {
    SCHOOL("School"),
    HEALTH("Health"),
    FEEDING("Feeding"),
    MEDICATION("Medication"),
    PLAY("Play"),
    OTHER("Other"),
}

enum class ActivityStatus(val label: String) {
    PENDING("Pending"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
}

enum class ReminderCategory(val label: String) {
    SCHOOL("School"),
    HEALTH("Health"),
    FEEDING("Feeding"),
    MEDICATION("Medication"),
    PLAY("Play"),
    APPOINTMENT("Appointment"),
    OTHER("Other"),
}

enum class ReminderStatus(val label: String) {
    PENDING("Pending"),
    COMPLETED("Completed"),
    MISSED("Missed"),
    DISMISSED("Dismissed"),
    SNOOZED("Snoozed"),
}

enum class Priority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    URGENT("Urgent"),
}

enum class TaskStatus(val label: String) {
    PENDING("Pending"),
    COMPLETED("Completed"),
    MISSED("Missed"),
    REQUIRES_FOLLOW_UP("Requires follow-up"),
}

enum class TipCategory(val label: String) {
    DEVELOPMENT("Child development"),
    HYGIENE("Hygiene"),
    NUTRITION("Nutrition"),
    SAFETY("Safety"),
    HEALTH("Health"),
    BEHAVIOUR("Behaviour"),
}

enum class ReferralCategory(val label: String) {
    EMERGENCY("Emergency"),
    CHILD_PROTECTION("Child protection"),
    HEALTH("Health facility"),
    LEGAL("Legal aid"),
    EDUCATION("Education"),
    COUNSELLING("Counselling"),
    SOCIAL_WELFARE("Social welfare"),
}

enum class AuditAction(val label: String) {
    CREATE("Created"),
    UPDATE("Updated"),
    DELETE("Deleted"),
    LOGIN("Signed in"),
    LOGOUT("Signed out"),
    STATUS_CHANGE("Status changed"),
}

/** Selectable "remind me before" offsets for a reminder; persisted as [minutes] on the reminder. */
enum class ReminderLeadTime(val minutes: Int, val label: String) {
    AT_TIME(0, "At time of event"),
    MIN_10(10, "10 minutes before"),
    MIN_30(30, "30 minutes before"),
    HOUR_1(60, "1 hour before"),
    HOUR_3(180, "3 hours before"),
    DAY_1(1_440, "1 day before"),
    ;

    companion object {
        fun fromMinutes(minutes: Int): ReminderLeadTime =
            entries.firstOrNull { it.minutes == minutes } ?: AT_TIME
    }
}
