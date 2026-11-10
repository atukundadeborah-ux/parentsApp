package com.example.parentapp.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.parentapp.core.designsystem.component.StatusBadge
import com.example.parentapp.core.util.DateTimeFormatters
import com.example.parentapp.data.local.entity.Reminder
import com.example.parentapp.data.local.model.ActivityWithChild
import com.example.parentapp.data.local.model.ReminderWithDetails
import com.example.parentapp.data.local.model.TaskWithNames

@Composable
fun ActivityRow(item: ActivityWithChild, showChild: Boolean = true, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(item.activity.title) },
        supportingContent = {
            Text(
                buildString {
                    if (showChild) append("${item.childName} · ")
                    append(item.activity.category.label)
                    append(" · ")
                    append(DateTimeFormatters.formatDateTime(item.activity.activityDateTime))
                },
            )
        },
        trailingContent = { StatusBadge(item.activity.status.label, item.activity.status.tone()) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
fun ReminderRow(item: ReminderWithDetails, showChild: Boolean = true, onClick: () -> Unit) =
    ReminderRow(item.reminder, item.childName, showChild, onClick)

@Composable
fun ReminderRow(reminder: Reminder, childName: String, showChild: Boolean = true, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(reminder.title) },
        supportingContent = {
            Text(
                buildString {
                    if (showChild) append("$childName · ")
                    append(reminder.category.label)
                    append(" · ")
                    append(DateTimeFormatters.formatDateTime(reminder.reminderDateTime))
                },
            )
        },
        trailingContent = { StatusBadge(reminder.status.label, reminder.status.tone()) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
fun TaskRow(item: TaskWithNames, showChild: Boolean = true, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(item.task.title) },
        supportingContent = {
            Text(
                buildString {
                    if (showChild) append("${item.childName} · ")
                    append("due ${DateTimeFormatters.formatDateTime(item.task.dueDateTime)}")
                    item.caregiverName?.let { append(" · $it") }
                },
            )
        },
        trailingContent = { StatusBadge(item.task.status.label, item.task.status.tone()) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    )
}
