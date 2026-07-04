package com.leonardo.seriestime.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leonardo.seriestime.data.supabase.AuthRepository
import com.leonardo.seriestime.ui.auth.AuthFlow
import com.leonardo.seriestime.ui.home.MainScaffold
import io.github.jan.supabase.auth.status.SessionStatus
import org.koin.compose.koinInject

@Composable
fun AppNavHost() {
    val authRepository = koinInject<AuthRepository>()
    val sessionStatus by authRepository.sessionStatus.collectAsStateWithLifecycle()

    when (sessionStatus) {
        is SessionStatus.Initializing -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        is SessionStatus.Authenticated -> MainScaffold()

        else -> AuthFlow()
    }
}
