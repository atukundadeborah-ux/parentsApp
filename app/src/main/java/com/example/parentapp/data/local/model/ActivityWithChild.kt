package com.example.parentapp.data.local.model

import androidx.room.Embedded
import com.example.parentapp.data.local.entity.Activity

data class ActivityWithChild(
    @Embedded val activity: Activity,
    val childName: String
)
