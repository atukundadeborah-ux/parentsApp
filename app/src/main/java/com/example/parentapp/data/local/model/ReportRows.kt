package com.example.parentapp.data.local.model

/** `GROUP BY` projections used by the Reports module. Enum columns come back as their `name`. */

data class LabelCount(
    val label: String,
    val count: Int,
)

data class CaregiverCompletion(
    val caregiverId: Long,
    val caregiverName: String,
    val total: Int,
    val completed: Int,
    val missed: Int,
) {
    val completionRate: Float get() = if (total == 0) 0f else completed.toFloat() / total
}
