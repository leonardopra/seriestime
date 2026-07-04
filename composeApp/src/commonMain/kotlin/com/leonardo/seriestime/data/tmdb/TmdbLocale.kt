package com.leonardo.seriestime.data.tmdb

import androidx.compose.ui.text.intl.Locale

/** TMDB language tag from the current app/device locale (IT falls back to EN). */
fun tmdbLanguage(): String =
    if (Locale.current.language == "it") "it-IT" else "en-US"
