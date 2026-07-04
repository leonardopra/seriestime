package com.leonardo.seriestime

import androidx.compose.runtime.Composable
import com.leonardo.seriestime.ui.navigation.AppNavHost
import com.leonardo.seriestime.ui.theme.SeriesTimeTheme
import org.koin.compose.KoinContext

@Composable
fun App() {
    KoinContext {
        SeriesTimeTheme {
            AppNavHost()
        }
    }
}
