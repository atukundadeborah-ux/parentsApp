package com.example.parentapp.ui.activity

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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parentapp.core.designsystem.component.DatePickerField
import com.example.parentapp.core.designsystem.component.LoadingState
import com.example.parentapp.core.designsystem.component.ParentsAppDropdownField
import com.example.parentapp.core.designsystem.component.ParentsAppTextField
import com.example.parentapp.core.designsystem.component.ParentsAppTopBar
import com.example.parentapp.core.designsystem.component.PrimaryButton
import com.example.parentapp.core.designsystem.component.TimePickerField
import com.example.parentapp.domain.model.ActivityCategory
import com.example.parentapp.domain.model.ActivityStatus

@Composable
fun ActivityEditorScreen(
    onDone: () -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: ActivityEditorViewModel = viewModel(factory = ActivityEditorViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.done) { if (state.done) onDone() }

    Scaffold(
        topBar = {
            ParentsAppTopBar(
                title = if (state.editing) "Edit activity" else "Record activity",
                onNavigateUp = onNavigateUp,
            )
        },
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ParentsAppDropdownField(
                label = "Child",
                selected = state.children.firstOrNull { it.id == state.childId },
                options = state.children,
                optionLabel = { it.fullName },
                onSelected = { viewModel.onChild(it.id) },
                isError = state.errors.containsKey("child"),
                errorText = state.errors["child"],
            )
            ParentsAppDropdownField(
                label = "Category",
                selected = state.category,
                options = ActivityCategory.entries,
                optionLabel = { it.label },
                onSelected = viewModel::onCategory,
            )
            ParentsAppTextField(
                value = state.title,
                onValueChange = viewModel::onTitle,
                label = "Title",
                isError = state.errors.containsKey("title"),
                errorText = state.errors["title"],
            )
            ParentsAppTextField(
                value = state.description,
                onValueChange = viewModel::onDescription,
                label = "Details (optional)",
                singleLine = false,
                minLines = 3,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DatePickerField(
                    label = "Date",
                    value = state.dateTimeMillis,
                    onValueChange = viewModel::onDate,
                    modifier = Modifier.weight(1f),
                )
                TimePickerField(
                    label = "Time",
                    value = state.dateTimeMillis,
                    onValueChange = viewModel::onTime,
                    modifier = Modifier.weight(1f),
                )
            }
            ParentsAppDropdownField(
                label = "Status",
                selected = state.status,
                options = ActivityStatus.entries,
                optionLabel = { it.label },
                onSelected = viewModel::onStatus,
            )
            Spacer(Modifier.size(8.dp))
            PrimaryButton(
                text = if (state.editing) "Save changes" else "Save activity",
                onClick = viewModel::save,
                loading = state.saving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
