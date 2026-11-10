package com.example.parentapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.parentapp.domain.model.ReferralCategory

@Entity(tableName = "referral_contacts")
data class ReferralContact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: ReferralCategory,
    val phone: String,
    val location: String,
    val description: String,
    val isEmergency: Boolean,
    /** Optional 24/7 helpline flag for sorting/pinning. */
    val isHotline: Boolean = false,
)
