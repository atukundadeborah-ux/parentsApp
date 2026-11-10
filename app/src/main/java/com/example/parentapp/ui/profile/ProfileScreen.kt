package com.example.parentapp.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parentapp.core.designsystem.component.InfoRow
import com.example.parentapp.core.designsystem.component.LoadingState
import com.example.parentapp.core.designsystem.component.ParentsAppPasswordField
import com.example.parentapp.core.designsystem.component.ParentsAppTextField
import com.example.parentapp.core.designsystem.component.ParentsAppTopBar
import com.example.parentapp.core.designsystem.component.PrimaryButton
import com.example.parentapp.core.designsystem.component.SectionHeader

@Composable
fun ProfileScreen(
    onNavigateUp: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { ParentsAppTopBar(title = "Profile", onNavigateUp = onNavigateUp) },
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
        ) {
            state.account?.let { acc ->
                InfoRow("Email", acc.email)
                InfoRow("Role", acc.role.label)
                InfoRow("Account status", acc.status.label)
            }

            SectionHeader("Personal details")
            ParentsAppTextField(
                value = state.fullName,
                onValueChange = viewModel::onFullName,
                label = "Full name",
                isError = state.profileErrors.containsKey("name"),
                errorText = state.profileErrors["name"],
            )
            Spacer(Modifier.size(12.dp))
            ParentsAppTextField(
                value = state.phone,
                onValueChange = viewModel::onPhone,
                label = "Phone number",
                keyboardType = KeyboardType.Phone,
                isError = state.profileErrors.containsKey("phone"),
                errorText = state.profileErrors["phone"],
            )
            state.profileMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.size(12.dp))
            PrimaryButton("Save profile", onClick = viewModel::saveProfile, loading = state.savingProfile, modifier = Modifier.fillMaxWidth())

            SectionHeader("Change password")
            ParentsAppPasswordField(
                value = state.currentPassword,
                onValueChange = viewModel::onCurrentPassword,
                label = "Current password",
                isError = state.passwordErrors.containsKey("current"),
                errorText = state.passwordErrors["current"],
            )
            Spacer(Modifier.size(12.dp))
            ParentsAppPasswordField(
                value = state.newPassword,
                onValueChange = viewModel::onNewPassword,
                label = "New password",
                isError = state.passwordErrors.containsKey("new"),
                errorText = state.passwordErrors["new"],
            )
            Spacer(Modifier.size(12.dp))
            ParentsAppPasswordField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmPassword,
                label = "Confirm new password",
                isError = state.passwordErrors.containsKey("confirm"),
                errorText = state.passwordErrors["confirm"],
            )
            state.passwordMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.size(12.dp))
            PrimaryButton("Update password", onClick = viewModel::changePassword, loading = state.savingPassword, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.size(24.dp))
        }
    }
}
