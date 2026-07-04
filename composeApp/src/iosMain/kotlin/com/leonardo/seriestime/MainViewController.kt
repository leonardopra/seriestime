package com.leonardo.seriestime

import androidx.compose.ui.window.ComposeUIViewController
import com.leonardo.seriestime.di.initKoin

fun MainViewController() = ComposeUIViewController(
    configure = { initKoin() }
) {
    App()
}
