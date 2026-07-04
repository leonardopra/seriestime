package com.leonardo.seriestime.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import seriestime.composeapp.generated.resources.Res
import seriestime.composeapp.generated.resources.app_name
import seriestime.composeapp.generated.resources.auth_display_name
import seriestime.composeapp.generated.resources.auth_email
import seriestime.composeapp.generated.resources.auth_go_sign_in
import seriestime.composeapp.generated.resources.auth_go_sign_up
import seriestime.composeapp.generated.resources.auth_password
import seriestime.composeapp.generated.resources.auth_sign_in
import seriestime.composeapp.generated.resources.auth_sign_up

@Composable
fun AuthFlow() {
    var showSignUp by remember { mutableStateOf(false) }
    if (showSignUp) {
        SignUpScreen(onGoToSignIn = { showSignUp = false })
    } else {
        SignInScreen(onGoToSignUp = { showSignUp = true })
    }
}

@Composable
private fun SignInScreen(
    onGoToSignUp: () -> Unit,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AuthColumn {
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(Modifier.height(24.dp))
        EmailField(email, { email = it })
        PasswordField(password, { password = it })
        AuthError(state.error)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.signIn(email, password) },
            enabled = !state.isLoading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text(stringResource(Res.string.auth_sign_in))
            }
        }
        TextButton(onClick = {
            viewModel.clearError()
            onGoToSignUp()
        }) {
            Text(stringResource(Res.string.auth_go_sign_up))
        }
    }
}

@Composable
private fun SignUpScreen(
    onGoToSignIn: () -> Unit,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AuthColumn {
        Text(
            text = stringResource(Res.string.auth_sign_up),
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text(stringResource(Res.string.auth_display_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        EmailField(email, { email = it })
        PasswordField(password, { password = it })
        AuthError(state.error)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.signUp(displayName, email, password) },
            enabled = !state.isLoading && email.isNotBlank() && password.length >= 6,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text(stringResource(Res.string.auth_sign_up))
            }
        }
        TextButton(onClick = {
            viewModel.clearError()
            onGoToSignIn()
        }) {
            Text(stringResource(Res.string.auth_go_sign_in))
        }
    }
}

@Composable
private fun AuthColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
private fun EmailField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(Res.string.auth_email)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(Res.string.auth_password)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AuthError(error: String?) {
    if (error != null) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
