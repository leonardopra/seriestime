package com.leonardo.seriestime.ui.showdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.leonardo.seriestime.data.supabase.ShowStatus
import com.leonardo.seriestime.data.tmdb.TmdbImages
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import seriestime.composeapp.generated.resources.Res
import seriestime.composeapp.generated.resources.detail_favorite
import seriestime.composeapp.generated.resources.show_mark_season
import seriestime.composeapp.generated.resources.show_mark_series
import seriestime.composeapp.generated.resources.show_season
import seriestime.composeapp.generated.resources.show_specials
import seriestime.composeapp.generated.resources.status_watched
import seriestime.composeapp.generated.resources.status_watching
import seriestime.composeapp.generated.resources.status_watchlist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDetailScreen(
    tmdbId: Int,
    onBack: () -> Unit,
    viewModel: ShowDetailViewModel = koinViewModel { parametersOf(tmdbId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.details?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::markSeriesWatched) {
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = stringResource(Res.string.show_mark_series),
                        )
                    }
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            imageVector = if (state.isFavorite) Icons.Default.Favorite
                            else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(Res.string.detail_favorite),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.error != null && state.details == null -> Text(
                text = state.error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(padding).padding(16.dp),
            )

            else -> state.details?.let { details ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                ) {
                    item {
                        Row {
                            AsyncImage(
                                model = TmdbImages.poster(details.posterPath),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(width = 120.dp, height = 180.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(details.name, style = MaterialTheme.typography.headlineSmall)
                                details.firstAirDate?.take(4)?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = "${details.numberOfSeasons}S · ${details.numberOfEpisodes}E",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusChip(
                                selected = state.status == ShowStatus.Watchlist,
                                label = stringResource(Res.string.status_watchlist),
                                onClick = { viewModel.setStatus(ShowStatus.Watchlist) },
                            )
                            StatusChip(
                                selected = state.status == ShowStatus.Watching,
                                label = stringResource(Res.string.status_watching),
                                onClick = { viewModel.setStatus(ShowStatus.Watching) },
                            )
                            StatusChip(
                                selected = state.status == ShowStatus.Watched,
                                label = stringResource(Res.string.status_watched),
                                onClick = { viewModel.setStatus(ShowStatus.Watched) },
                            )
                        }

                        details.overview?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(12.dp))
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    items(details.seasons, key = { it.seasonNumber }) { season ->
                        val expanded = state.expandedSeason == season.seasonNumber
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleSeason(season.seasonNumber) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = if (season.seasonNumber == 0)
                                            stringResource(Res.string.show_specials)
                                        else stringResource(Res.string.show_season, season.seasonNumber),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        text = "${state.watchedCountInSeason(season.seasonNumber)}/${season.episodeCount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (expanded) {
                                    TextButton(onClick = { viewModel.markSeasonWatched(season.seasonNumber) }) {
                                        Text(stringResource(Res.string.show_mark_season))
                                    }
                                }
                                Icon(
                                    imageVector = if (expanded) Icons.Default.ExpandLess
                                    else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                )
                            }

                            if (expanded) {
                                val loaded = state.loadedSeasons[season.seasonNumber]
                                if (loaded == null) {
                                    Box(
                                        Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(Modifier.size(24.dp))
                                    }
                                } else {
                                    loaded.episodes.forEach { episode ->
                                        val watched =
                                            (season.seasonNumber to episode.episodeNumber) in state.watched
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.toggleEpisode(
                                                        season.seasonNumber,
                                                        episode.episodeNumber,
                                                    )
                                                }
                                                .padding(vertical = 6.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = "${episode.episodeNumber}.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.width(32.dp),
                                            )
                                            Text(
                                                text = episode.name ?: "",
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f),
                                            )
                                            Icon(
                                                imageVector = if (watched) Icons.Default.CheckCircle
                                                else Icons.Outlined.Circle,
                                                contentDescription = null,
                                                tint = if (watched) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}
