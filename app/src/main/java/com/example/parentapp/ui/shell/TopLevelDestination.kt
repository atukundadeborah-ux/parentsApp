package com.example.parentapp.ui.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.ui.navigation.Destinations

enum class TopLevelDestination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: Destinations,
) {
    HOME("Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, Destinations.Dashboard),
    CHILDREN("Children", Icons.Filled.ChildCare, Icons.Outlined.ChildCare, Destinations.ChildList),
    REMINDERS("Reminders", Icons.Filled.Notifications, Icons.Outlined.Notifications, Destinations.ReminderList()),
    TASKS("Tasks", Icons.AutoMirrored.Filled.Assignment, Icons.AutoMirrored.Outlined.Assignment, Destinations.TaskList),
    ACTIVITIES("Activities", Icons.Filled.Event, Icons.Outlined.Event, Destinations.ActivityList()),
    TIPS("Tips", Icons.Filled.Lightbulb, Icons.Outlined.Lightbulb, Destinations.TipList),
    REPORTS("Reports", Icons.Filled.BarChart, Icons.Outlined.BarChart, Destinations.Reports),
    USERS("Users", Icons.Filled.Group, Icons.Outlined.Group, Destinations.UserManagement),
    AUDIT("Audit", Icons.Filled.Receipt, Icons.Outlined.Receipt, Destinations.AuditLogs),
    MORE("More", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz, Destinations.More),
    ;

    companion object {
        fun forRole(role: UserRole?): List<TopLevelDestination> = when (role) {
            UserRole.PARENT_GUARDIAN -> listOf(HOME, CHILDREN, REMINDERS, TASKS, MORE)
            UserRole.CAREGIVER -> listOf(HOME, TASKS, CHILDREN, TIPS, MORE)
            UserRole.INSTITUTIONAL_USER -> listOf(HOME, CHILDREN, ACTIVITIES, REPORTS, MORE)
            UserRole.ADMIN -> listOf(HOME, USERS, REPORTS, AUDIT, MORE)
            null -> listOf(HOME, MORE)
        }
    }
}
