package com.example.parentapp.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.ui.navigation.Destinations
import com.example.parentapp.ui.shell.AppShell
import com.example.parentapp.ui.shell.TopLevelDestination

private data class MoreEntry(val label: String, val icon: ImageVector, val destination: Destinations)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    role: UserRole?,
    onSelectTab: (TopLevelDestination) -> Unit,
    onNavigate: (Destinations) -> Unit,
    onLogout: () -> Unit,
) {
    val entries = buildList {
        add(MoreEntry("Parenting & caregiver tips", Icons.AutoMirrored.Filled.MenuBook, Destinations.TipList))
        add(MoreEntry("Referral directory", Icons.Filled.SupportAgent, Destinations.ReferralDirectory))
        if (role == UserRole.PARENT_GUARDIAN || role == UserRole.CAREGIVER) {
            add(MoreEntry("All activities", Icons.Filled.Event, Destinations.ActivityList()))
        }
        add(MoreEntry("Reports & summaries", Icons.Filled.BarChart, Destinations.Reports))
        if (role == UserRole.ADMIN) {
            add(MoreEntry("Audit logs", Icons.Filled.Receipt, Destinations.AuditLogs))
        }
    }

    AppShell(
        selected = TopLevelDestination.MORE,
        role = role,
        onSelect = onSelectTab,
        title = "More",
        onSignOut = onLogout,
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(entries.size) { index ->
                val entry = entries[index]
                ListItem(
                    headlineContent = { Text(entry.label) },
                    leadingContent = { Icon(entry.icon, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigate(entry.destination) },
                )
            }
            item { HorizontalDivider() }
            item {
                ListItem(
                    headlineContent = { Text("Profile") },
                    leadingContent = { Icon(Icons.Filled.Person, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigate(Destinations.Profile) },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Settings") },
                    leadingContent = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigate(Destinations.Settings) },
                )
            }
        }
    }
}
