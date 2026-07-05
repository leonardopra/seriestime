package com.leonardo.seriestime.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.leonardo.seriestime.data.supabase.AuthRepository
import com.leonardo.seriestime.ui.auth.AuthFlow
import com.leonardo.seriestime.ui.home.MainScaffold
import com.leonardo.seriestime.ui.importer.ImportScreen
import com.leonardo.seriestime.ui.moviedetail.MovieDetailScreen
import com.leonardo.seriestime.ui.showdetail.ShowDetailScreen
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object TabsRoute

@Serializable
data class MovieDetailRoute(val tmdbId: Int)

@Serializable
data class ShowDetailRoute(val tmdbId: Int)

@Serializable
object ImportRoute

@Composable
fun AppNavHost() {
    val authRepository = koinInject<AuthRepository>()
    val sessionStatus by authRepository.sessionStatus.collectAsStateWithLifecycle()

    // created once at the top level so the back stack survives any
    // Initializing/Authenticated branch flips (e.g. token refresh on resume)
    val navController = rememberNavController()

    when (sessionStatus) {
        is SessionStatus.Initializing -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        is SessionStatus.Authenticated -> AuthenticatedNav(navController)

        else -> AuthFlow()
    }
}

@Composable
private fun AuthenticatedNav(navController: androidx.navigation.NavHostController) {
    NavHost(navController = navController, startDestination = TabsRoute) {
        composable<TabsRoute> {
            MainScaffold(
                onOpenMovie = { navController.navigate(MovieDetailRoute(it)) },
                onOpenShow = { navController.navigate(ShowDetailRoute(it)) },
                onOpenImport = { navController.navigate(ImportRoute) },
            )
        }
        composable<ImportRoute> {
            ImportScreen(onBack = { navController.popBackStack() })
        }
        composable<MovieDetailRoute> { entry ->
            val route = entry.toRoute<MovieDetailRoute>()
            MovieDetailScreen(
                tmdbId = route.tmdbId,
                onBack = { navController.popBackStack() },
            )
        }
        composable<ShowDetailRoute> { entry ->
            val route = entry.toRoute<ShowDetailRoute>()
            ShowDetailScreen(
                tmdbId = route.tmdbId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
