package com.example.parentapp.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** App bar with an optional up affordance and trailing actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentsAppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateUp: (() -> Unit)? = null,
    centered: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    val navIcon: @Composable () -> Unit = {
        if (onNavigateUp != null) {
            IconButton(onClick = onNavigateUp) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    }
    if (centered) {
        CenterAlignedTopAppBar(
            title = { Text(title) },
            navigationIcon = navIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
            modifier = modifier,
        )
    } else {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = navIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
            modifier = modifier,
        )
    }
}

/** Trailing "Sign out" icon button for a top app bar. No-op slot when [onSignOut] is null. */
@Composable
fun SignOutAction(onSignOut: (() -> Unit)?) {
    if (onSignOut != null) {
        IconButton(onClick = onSignOut) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
        }
    }
}
