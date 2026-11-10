package com.example.parentapp.ui.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.parentapp.domain.model.UserRole

/**
 * Standard chrome for a top-level screen: a shared [TopAppBar] (with an optional back
 * affordance on the left and a "Sign out" button on the right) plus the role-specific
 * bottom [NavigationBar]. [content] receives the scaffold inner padding.
 *
 * Pass [onNavigateUp] when the screen was reached from another screen (shows a back arrow);
 * leave it null when the screen is the selected bottom-nav tab. Pass [onSignOut] to show
 * the top-right sign-out button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    selected: TopLevelDestination,
    role: UserRole?,
    onSelect: (TopLevelDestination) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onNavigateUp: (() -> Unit)? = null,
    onSignOut: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val items = TopLevelDestination.forRole(role)
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    if (subtitle == null) {
                        Text(title)
                    } else {
                        Column {
                            Text(title)
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onNavigateUp != null) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    actions()
                    if (onSignOut != null) {
                        IconButton(onClick = onSignOut) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                        }
                    }
                },
            )
        },
        floatingActionButton = floatingActionButton,
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    val isSelected = item == selected
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { if (!isSelected) onSelect(item) },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = null,
                            )
                        },
                        label = { Text(item.label) },
                    )
                }
            }
        },
        content = content,
    )
}
