package com.example.parentapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.parentapp.domain.model.AccountStatus
import com.example.parentapp.domain.model.UserRole

@Entity(
    tableName = "user_accounts",
    indices = [Index(value = ["email"], unique = true)],
)
data class UserAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val email: String,
    val phone: String,
    /** PBKDF2 encoded hash — see [com.example.parentapp.core.security.PasswordHasher]. */
    val passwordHash: String,
    val role: UserRole,
    val userCategory: String,
    val institutionId: Long? = null,
    val status: AccountStatus = AccountStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
)
