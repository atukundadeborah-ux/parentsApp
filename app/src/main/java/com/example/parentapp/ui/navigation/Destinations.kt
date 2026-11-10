package com.example.parentapp.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes (Navigation-Compose + kotlinx.serialization).
 * Grouped by module; screens are added to [ParentAppNavHost] as each phase lands.
 */
sealed interface Destinations {

    // ---- Auth / onboarding ----
    @Serializable data object Splash : Destinations
    @Serializable data object Onboarding : Destinations
    @Serializable data object RoleSelection : Destinations
    @Serializable data class Register(val category: String) : Destinations
    @Serializable data object Login : Destinations

    // ---- Shell ----
    @Serializable data object Dashboard : Destinations
    @Serializable data object More : Destinations

    // ---- Child profiles ----
    // -1L in the *Editor / *List routes means "not scoped / create new".
    @Serializable data object ChildList : Destinations
    @Serializable data class ChildDetail(val childId: Long) : Destinations
    @Serializable data class ChildEditor(val childId: Long = -1L) : Destinations

    // ---- Activities ----
    @Serializable data class ActivityList(val childId: Long = -1L) : Destinations
    @Serializable data class ActivityDetail(val activityId: Long) : Destinations
    @Serializable data class ActivityEditor(val childId: Long = -1L, val activityId: Long = -1L) : Destinations

    // ---- Reminders ----
    @Serializable data class ReminderList(val childId: Long = -1L) : Destinations
    @Serializable data class ReminderDetail(val reminderId: Long) : Destinations
    @Serializable data class ReminderEditor(val childId: Long = -1L, val reminderId: Long = -1L) : Destinations

    // ---- Caregiver tasks ----
    @Serializable data object TaskList : Destinations
    @Serializable data class TaskDetail(val taskId: Long) : Destinations
    @Serializable data class TaskEditor(val childId: Long = -1L, val taskId: Long = -1L) : Destinations

    // ---- Content / directory ----
    @Serializable data object TipList : Destinations
    @Serializable data class TipDetail(val tipId: Long) : Destinations
    @Serializable data object ReferralDirectory : Destinations

    // ---- Reporting & account ----
    @Serializable data object Reports : Destinations
    @Serializable data object Profile : Destinations
    @Serializable data object Settings : Destinations

    // ---- Admin ----
    @Serializable data object UserManagement : Destinations
    @Serializable data class UserDetail(val userId: Long) : Destinations
    @Serializable data object AuditLogs : Destinations
}
