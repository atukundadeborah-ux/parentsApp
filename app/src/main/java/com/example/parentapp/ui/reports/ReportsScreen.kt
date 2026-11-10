package com.example.parentapp.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parentapp.core.designsystem.component.FilterChipRow
import com.example.parentapp.core.designsystem.component.LoadingState
import com.example.parentapp.core.designsystem.component.SectionHeader
import com.example.parentapp.core.designsystem.theme.StatNumberTextStyle
import com.example.parentapp.data.local.model.LabelCount
import com.example.parentapp.ui.shell.AppShell
import com.example.parentapp.ui.shell.TopLevelDestination

@Composable
fun ReportsScreen(
    onSelectTab: (TopLevelDestination) -> Unit,
    onNavigateUp: (() -> Unit)?,
    onSignOut: () -> Unit,
    viewModel: ReportsViewModel = viewModel(factory = ReportsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AppShell(
        selected = TopLevelDestination.REPORTS,
        role = state.role,
        onSelect = onSelectTab,
        title = "Reports & summaries",
        onNavigateUp = onNavigateUp,
        onSignOut = onSignOut,
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@AppShell
        }
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportRange.entries.forEach { r ->
                        androidx.compose.material3.FilterChip(
                            selected = state.range == r,
                            onClick = { viewModel.onRange(r) },
                            label = { Text(r.label) },
                        )
                    }
                }
            }

            item {
                ReportCard("Activity summary") {
                    Text("${state.activityTotal}", style = StatNumberTextStyle)
                    Text("activities logged", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("By category", style = MaterialTheme.typography.titleSmall)
                    BarList(state.activityByCategory)
                    Spacer(Modifier.height(12.dp))
                    Text("By status", style = MaterialTheme.typography.titleSmall)
                    BarList(state.activityByStatus)
                }
            }

            item {
                ReportCard("Missed reminders") {
                    Text("${state.missedReminders}", style = StatNumberTextStyle, color = MaterialTheme.colorScheme.error)
                    Text("reminders missed in this period", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    BarList(state.reminderByStatus)
                }
            }

            item {
                ReportCard("Caregiver task completion") {
                    if (state.caregiverCompletion.isEmpty()) {
                        Text("No caregiver tasks yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        state.caregiverCompletion.forEach { c ->
                            Column(Modifier.padding(vertical = 6.dp)) {
                                Row {
                                    Text(c.caregiverName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Text(
                                        "${c.completed}/${c.total} · ${(c.completionRate * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { c.completionRate },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                )
                                if (c.missed > 0) {
                                    Text(
                                        "${c.missed} missed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportCard(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(title)
            content()
        }
    }
}

@Composable
private fun BarList(data: List<LabelCount>) {
    if (data.isEmpty()) {
        Text("No data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val max = data.maxOf { it.count }.coerceAtLeast(1)
    Column(Modifier.padding(top = 4.dp)) {
        data.forEach { row ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 3.dp),
            ) {
                Text(
                    row.label.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(110.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(row.count.toFloat() / max)
                            .height(18.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                Text(
                    "  ${row.count}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
