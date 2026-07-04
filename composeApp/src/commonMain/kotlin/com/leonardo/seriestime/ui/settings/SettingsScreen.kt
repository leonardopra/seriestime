package com.leonardo.seriestime.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.leonardo.seriestime.data.supabase.AuthRepository
import com.leonardo.seriestime.ui.auth.AuthViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import seriestime.composeapp.generated.resources.Res
import seriestime.composeapp.generated.resources.settings_sign_out
import seriestime.composeapp.generated.resources.settings_signed_in_as
import seriestime.composeapp.generated.resources.tmdb_attribution

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val authRepository = koinInject<AuthRepository>()

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(
                Res.string.settings_signed_in_as,
                authRepository.currentUserEmail ?: "",
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { viewModel.signOut() }) {
            Text(stringResource(Res.string.settings_sign_out))
        }
        Spacer(Modifier.height(48.dp))
        Text(
            text = stringResource(Res.string.tmdb_attribution),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
