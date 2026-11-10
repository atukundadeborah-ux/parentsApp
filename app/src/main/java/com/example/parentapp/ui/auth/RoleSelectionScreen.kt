package com.example.parentapp.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Diversity3
import androidx.compose.material.icons.rounded.Escalator
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.parentapp.core.designsystem.component.ParentsAppTopBar

private data class RoleOption(
    val category: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
)

private val roleOptions = listOf(
    RoleOption("Parent / Guardian", "Parent or guardian", "Manage your own children's profiles, activities and reminders.", Icons.Rounded.Diversity3),
    RoleOption("Caregiver", "Caregiver", "Carry out and update tasks assigned for the children in your care.", Icons.Rounded.Escalator),
    RoleOption("Institution staff", "Institution staff", "Coordinate care for children in a school or children's home.", Icons.Rounded.School),
    RoleOption("Administrator", "Administrator", "Manage user accounts and review system activity.", Icons.Rounded.AdminPanelSettings),
)

@Composable
fun RoleSelectionScreen(
    onBack: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onHasAccount: () -> Unit,
) {
    Scaffold(
        topBar = { ParentsAppTopBar(title = "Create your account", onNavigateUp = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "How will you use ParentsApp?",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "This sets what you can see and do. You can be updated to another role by an administrator later.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(4.dp))

            roleOptions.forEach { option ->
                ElevatedCard(onClick = { onCategorySelected(option.category) }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(option.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                    }
                }
            }

            Spacer(Modifier.size(8.dp))
            androidx.compose.material3.TextButton(
                onClick = onHasAccount,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text("Already have an account? Sign in") }
        }
    }
}
