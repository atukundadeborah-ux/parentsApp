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
import com.example.parentapp.core.designsystem.component.PrimaryButton

@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit,
    onRegister: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.success) {
        if (state.success) onAuthenticated()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Welcome back", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Sign in to continue coordinating care.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(24.dp))

            ParentsAppTextField(
                value = state.email,
                onValueChange = { viewModel.onField(AuthField.EMAIL, it) },
                label = "Email",
                keyboardType = KeyboardType.Email,
                isError = state.fieldErrors.containsKey(AuthField.EMAIL),
                errorText = state.fieldErrors[AuthField.EMAIL],
            )
            Spacer(Modifier.size(12.dp))
            ParentsAppPasswordField(
                value = state.password,
                onValueChange = { viewModel.onField(AuthField.PASSWORD, it) },
                label = "Password",
                isError = state.fieldErrors.containsKey(AuthField.PASSWORD),
                errorText = state.fieldErrors[AuthField.PASSWORD],
            )

            if (state.formError != null) {
                Spacer(Modifier.size(12.dp))
                Text(
                    text = state.formError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.size(24.dp))
            PrimaryButton(
                text = "Sign in",
                onClick = viewModel::login,
                loading = state.submitting,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.size(8.dp))
            TextButton(
                onClick = onRegister,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text("New here? Create an account") }

            Spacer(Modifier.size(16.dp))
            Text(
                text = "Demo: parent@example.com · Password123!",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}
