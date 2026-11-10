package com.example.parentapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "institutions")
data class Institution(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val location: String,
    val contactPerson: String,
    val phone: String,
    val createdAt: Long = System.currentTimeMillis()
)
