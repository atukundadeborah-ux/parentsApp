package com.example.parentapp.ui.child

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parentapp.core.designsystem.component.ConfirmDialog
import com.example.parentapp.core.designsystem.component.DatePickerField
import com.example.parentapp.core.designsystem.component.LoadingState
import com.example.parentapp.core.designsystem.component.ParentsAppDropdownField
import com.example.parentapp.core.designsystem.component.ParentsAppTextField
import com.example.parentapp.core.designsystem.component.ParentsAppTopBar
import com.example.parentapp.core.designsystem.component.PrimaryButton
import com.example.parentapp.domain.model.ProfileStatus

private val genderOptions = listOf("Female", "Male", "Other")

@Composable
fun ChildEditorScreen(
    onDone: () -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: ChildEditorViewModel = viewModel(factory = ChildEditorViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    if (state.savedId != null || state.deleted) {
        androidx.compose.runtime.LaunchedEffect(state.savedId, state.deleted) { onDone() }
    }

    Scaffold(
        topBar = {
            ParentsAppTopBar(
                title = if (state.editing) "Edit child" else "Add child",
                onNavigateUp = onNavigateUp,
                actions = {
                    if (state.editing) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
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
            state.formError?.let {
                androidx.compose.material3.Text(it, color = MaterialTheme.colorScheme.error)
            }
            ParentsAppTextField(
                value = state.fullName,
                onValueChange = viewModel::onFullName,
                label = "Full name",
                isError = state.errors.containsKey("fullName"),
                errorText = state.errors["fullName"],
            )
            ParentsAppDropdownField(
                label = "Gender",
                selected = state.gender.takeIf { it.isNotBlank() },
                options = genderOptions,
                optionLabel = { it },
                onSelected = viewModel::onGender,
                isError = state.errors.containsKey("gender"),
                errorText = state.errors["gender"],
            )
            DatePickerField(
                label = "Date of birth",
                value = state.dateOfBirthMillis,
                onValueChange = viewModel::onDob,
                isError = state.errors.containsKey("dob"),
                errorText = state.errors["dob"],
            )
            ParentsAppTextField(
                value = state.guardianName,
                onValueChange = viewModel::onGuardianName,
                label = "Guardian name",
                isError = state.errors.containsKey("guardianName"),
                errorText = state.errors["guardianName"],
            )
            ParentsAppTextField(
                value = state.guardianContact,
                onValueChange = viewModel::onGuardianContact,
                label = "Guardian contact",
                keyboardType = KeyboardType.Phone,
                isError = state.errors.containsKey("guardianContact"),
                errorText = state.errors["guardianContact"],
            )
            ParentsAppTextField(
                value = state.schoolName,
                onValueChange = viewModel::onSchool,
                label = "School / daycare (optional)",
            )
            ParentsAppDropdownField(
                label = "Assigned caregiver (optional)",
                selected = state.caregivers.firstOrNull { it.id == state.assignedCaregiverId },
                options = state.caregivers,
                optionLabel = { it.fullName },
                onSelected = { viewModel.onCaregiver(it.id) },
                placeholder = "None",
            )
            ParentsAppTextField(
                value = state.medicalNotes,
                onValueChange = viewModel::onMedicalNotes,
                label = "Medical notes (optional)",
                singleLine = false,
                minLines = 2,
            )
            ParentsAppTextField(
                value = state.careNotes,
                onValueChange = viewModel::onCareNotes,
                label = "Care notes (optional)",
                singleLine = false,
                minLines = 2,
            )
            if (state.editing) {
                ParentsAppDropdownField(
                    label = "Status",
                    selected = state.status,
                    options = ProfileStatus.entries,
                    optionLabel = { it.label },
                    onSelected = viewModel::onStatus,
                )
            }

            Spacer(Modifier.size(8.dp))
            PrimaryButton(
                text = if (state.editing) "Save changes" else "Add child",
                onClick = viewModel::save,
                loading = state.saving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete child profile?",
            message = "This permanently removes ${state.fullName.ifBlank { "this child" }}'s profile and cannot be undone. Consider archiving instead to keep the history.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
