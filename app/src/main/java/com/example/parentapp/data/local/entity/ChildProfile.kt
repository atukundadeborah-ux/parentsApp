package com.example.parentapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.parentapp.domain.model.ProfileStatus

@Entity(
    tableName = "child_profiles",
    indices = [Index("createdByUserId"), Index("assignedCaregiverId"), Index("institutionId")],
)
data class ChildProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val gender: String,
    /** ISO `yyyy-MM-dd`. */
    val dateOfBirth: String,
    val guardianName: String,
    val guardianContact: String,
    val householdInstitution: String = "Family",
    /** Display name of the assigned caregiver. */
    val assignedCaregiver: String = "",
    /** FK into `user_accounts` for the assigned caregiver, when one is linked. */
    val assignedCaregiverId: Long? = null,
    val schoolName: String = "",
    val medicalNotes: String = "",
    val careNotes: String = "",
    val photoUri: String? = null,
    val institutionId: Long? = null,
    /** Guardian link — the account that owns this profile. */
    val createdByUserId: Long,
    val profileStatus: ProfileStatus = ProfileStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
