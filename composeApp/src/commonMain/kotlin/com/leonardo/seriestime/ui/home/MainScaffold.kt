package com.leonardo.seriestime.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.leonardo.seriestime.ui.search.SearchScreen
import com.leonardo.seriestime.ui.settings.SettingsScreen
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import seriestime.composeapp.generated.resources.Res
import seriestime.composeapp.generated.resources.tab_movies
import seriestime.composeapp.generated.resources.tab_search
import seriestime.composeapp.generated.resources.tab_settings
import seriestime.composeapp.generated.resources.tab_shows

private enum class MainTab(val label: StringResource) {
    Shows(Res.string.tab_shows),
    Movies(Res.string.tab_movies),
    Search(Res.string.tab_search),
    Settings(Res.string.tab_settings),
}

@Composable
fun MainScaffold(
    onOpenMovie: (Int) -> Unit,
    onOpenShow: (Int) -> Unit,
    onOpenImport: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Shows) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    MainTab.Shows -> Icons.Default.Tv
                                    MainTab.Movies -> Icons.Default.Movie
                                    MainTab.Search -> Icons.Default.Search
                                    MainTab.Settings -> Icons.Default.Settings
                                },
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
        },
    ) { padding ->
        when (selectedTab) {
            MainTab.Shows -> ShowsTab(
                onOpenShow = onOpenShow,
                modifier = Modifier.padding(padding),
            )
            MainTab.Movies -> MoviesTab(
                onOpenMovie = onOpenMovie,
                modifier = Modifier.padding(padding),
            )
            MainTab.Search -> SearchScreen(
                onResultClick = { result ->
                    if (result.mediaType == "movie") onOpenMovie(result.id)
                    else onOpenShow(result.id)
                },
                modifier = Modifier.padding(padding),
            )
            MainTab.Settings -> SettingsScreen(
                onOpenImport = onOpenImport,
                modifier = Modifier.padding(padding),
            )
        }
    }
}
