package com.leonardo.seriestime

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.leonardo.seriestime.ui.navigation.AppNavHost
import com.leonardo.seriestime.ui.theme.SeriesTimeTheme
import org.koin.compose.KoinContext

@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
    }
    KoinContext {
        SeriesTimeTheme {
            AppNavHost()
        }
    }
}
