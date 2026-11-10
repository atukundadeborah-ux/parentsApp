package com.example.parentapp.ui.auth

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parentapp.core.designsystem.component.ParentsAppPasswordField
import com.example.parentapp.core.designsystem.component.ParentsAppTextField
import com.example.parentapp.core.designsystem.component.ParentsAppTopBar
import com.example.parentapp.core.designsystem.component.PrimaryButton
import com.example.parentapp.domain.model.UserRole

@Composable
fun RegisterScreen(
    category: String,
    onBack: () -> Unit,
    onAuthenticated: () -> Unit,
    onHasAccount: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(category) { viewModel.setCategory(category) }
    LaunchedEffect(state.success) { if (state.success) onAuthenticated() }

    val role = UserRole.fromCategory(category)

    Scaffold(
        topBar = { ParentsAppTopBar(title = "Create account", onNavigateUp = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Signing up as ${role.label}", style = MaterialTheme.typography.titleMedium)
            Text(
                "Your details are stored only on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(4.dp))

            ParentsAppTextField(
                value = state.fullName,
                onValueChange = { viewModel.onField(AuthField.NAME, it) },
                label = "Full name",
                isError = state.fieldErrors.containsKey(AuthField.NAME),
                errorText = state.fieldErrors[AuthField.NAME],
            )
            ParentsAppTextField(
                value = state.email,
                onValueChange = { viewModel.onField(AuthField.EMAIL, it) },
                label = "Email",
                keyboardType = KeyboardType.Email,
                isError = state.fieldErrors.containsKey(AuthField.EMAIL),
                errorText = state.fieldErrors[AuthField.EMAIL],
            )
            ParentsAppTextField(
                value = state.phone,
                onValueChange = { viewModel.onField(AuthField.PHONE, it) },
                label = "Phone number",
                keyboardType = KeyboardType.Phone,
                isError = state.fieldErrors.containsKey(AuthField.PHONE),
                errorText = state.fieldErrors[AuthField.PHONE],
            )
            ParentsAppPasswordField(
                value = state.password,
                onValueChange = { viewModel.onField(AuthField.PASSWORD, it) },
                label = "Password",
                isError = state.fieldErrors.containsKey(AuthField.PASSWORD),
                errorText = state.fieldErrors[AuthField.PASSWORD],
            )
            ParentsAppPasswordField(
                value = state.confirmPassword,
                onValueChange = { viewModel.onField(AuthField.CONFIRM, it) },
                label = "Confirm password",
                isError = state.fieldErrors.containsKey(AuthField.CONFIRM),
                errorText = state.fieldErrors[AuthField.CONFIRM],
            )

            if (state.formError != null) {
                Text(
                    text = state.formError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.size(8.dp))
            PrimaryButton(
                text = "Create account",
                onClick = viewModel::register,
                loading = state.submitting,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                onClick = onHasAccount,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text("Already have an account? Sign in") }
        }
    }
}
