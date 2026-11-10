package com.example.parentapp.core.security

import com.example.parentapp.domain.model.UserRole

/**
 * Write permissions per role. Checked by the ViewModels before any repository write, so a
 * screen reached by an unexpected route still cannot change data the role may only read.
 */
object AccessPolicy {

    /** Parents/guardians, institutional staff and administrators manage child profiles. */
    fun canEditChildProfiles(role: UserRole?): Boolean =
        role == UserRole.PARENT_GUARDIAN || role == UserRole.INSTITUTIONAL_USER || role == UserRole.ADMIN

    /** Caregivers receive tasks; they never assign them. */
    fun canAssignTasks(role: UserRole?): Boolean =
        role == UserRole.PARENT_GUARDIAN || role == UserRole.INSTITUTIONAL_USER || role == UserRole.ADMIN

    const val DENIED_MESSAGE = "Your role does not allow this change."
}
