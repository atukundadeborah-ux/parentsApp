package com.example.parentapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.parentapp.domain.model.TipCategory

@Entity(tableName = "tips")
data class Tip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: TipCategory,
    val summary: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
)
