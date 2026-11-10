package com.example.parentapp.ui.referral

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parentapp.core.designsystem.component.BadgeTone
import com.example.parentapp.core.designsystem.component.EmptyState
import com.example.parentapp.core.designsystem.component.FilterChipRow
import com.example.parentapp.core.designsystem.component.LoadingState
import com.example.parentapp.core.designsystem.component.ParentsAppTopBar
import com.example.parentapp.core.designsystem.component.SectionHeader
import com.example.parentapp.core.designsystem.component.StatusBadge
import com.example.parentapp.data.local.entity.ReferralContact
import com.example.parentapp.domain.model.ReferralCategory

@Composable
fun ReferralScreen(
    onNavigateUp: () -> Unit,
    viewModel: ReferralViewModel = viewModel(factory = ReferralViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    fun dial(phone: String) {
        if (phone.isBlank()) {
            Toast.makeText(context, "No number on file — check locally", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.filter { it.isDigit() || it == '+' }}")))
        }.onFailure { Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show() }
    }

    fun map(location: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(location)}")))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No maps app installed", Toast.LENGTH_SHORT).show()
        }
    }

    ParentsAppScaffold(onNavigateUp) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQuery,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Search services or places") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            FilterChipRow(
                options = ReferralCategory.entries,
                selected = state.category,
                optionLabel = { it.label },
                onSelected = viewModel::onCategory,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            when {
                state.loading -> LoadingState()
                state.emergency.isEmpty() && state.others.isEmpty() -> EmptyState(
                    icon = Icons.Filled.SupportAgent,
                    title = "No contacts found",
                    description = "Try another category or search term.",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.emergency.isNotEmpty()) {
                        item { SectionHeader("Emergency & helplines") }
                        items(state.emergency, key = { it.id }) { ContactCard(it, ::dial, ::map) }
                    }
                    if (state.others.isNotEmpty()) {
                        item { SectionHeader("Support services") }
                        items(state.others, key = { it.id }) { ContactCard(it, ::dial, ::map) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentsAppScaffold(
    onNavigateUp: () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    androidx.compose.material3.Scaffold(
        topBar = { ParentsAppTopBar(title = "Referral directory", onNavigateUp = onNavigateUp) },
        content = content,
    )
}

@Composable
private fun ContactCard(
    contact: ReferralContact,
    onDial: (String) -> Unit,
    onMap: (String) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (contact.isHotline) StatusBadge("24/7", BadgeTone.DANGER)
                else StatusBadge(contact.category.label, BadgeTone.INFO)
            }
            Text(
                contact.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                contact.location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalIconButton(onClick = { onDial(contact.phone) }) {
                    Icon(Icons.Filled.Call, contentDescription = "Call ${contact.name}")
                }
                FilledTonalIconButton(onClick = { onMap(contact.location) }) {
                    Icon(Icons.Filled.Place, contentDescription = "Show ${contact.name} on a map")
                }
                if (contact.phone.isNotBlank()) {
                    Text(
                        contact.phone,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}
