package com.leonardo.seriestime.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.leonardo.seriestime.data.supabase.ShowStatus
import com.leonardo.seriestime.data.tmdb.TmdbImages
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import seriestime.composeapp.generated.resources.Res
import seriestime.composeapp.generated.resources.library_empty
import seriestime.composeapp.generated.resources.status_watched
import seriestime.composeapp.generated.resources.status_watching
import seriestime.composeapp.generated.resources.status_watchlist

@Composable
fun MoviesTab(
    onOpenMovie: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoviesTabViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = !state.showWatched,
                onClick = { viewModel.setShowWatched(false) },
                label = { Text(stringResource(Res.string.status_watchlist)) },
            )
            FilterChip(
                selected = state.showWatched,
                onClick = { viewModel.setShowWatched(true) },
                label = { Text(stringResource(Res.string.status_watched)) },
            )
            Box(Modifier.weight(1f))
            IconButton(onClick = viewModel::toggleFavoritesOnly) {
                Icon(
                    imageVector = if (state.favoritesOnly) Icons.Default.Favorite
                    else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        when {
            state.isLoading -> Loading()
            state.error != null -> ErrorText(state.error.orEmpty())
            state.visible.isEmpty() -> EmptyText()
            else -> PosterGrid(
                items = state.visible.map { PosterItem(it.tmdbId, it.movies.title, it.movies.posterPath, null) },
                onClick = onOpenMovie,
            )
        }
    }
}

@Composable
fun ShowsTab(
    onOpenShow: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShowsTabViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = state.selectedStatus == ShowStatus.Watching,
                onClick = { viewModel.setStatus(ShowStatus.Watching) },
                label = { Text(stringResource(Res.string.status_watching)) },
            )
            FilterChip(
                selected = state.selectedStatus == ShowStatus.Watchlist,
                onClick = { viewModel.setStatus(ShowStatus.Watchlist) },
                label = { Text(stringResource(Res.string.status_watchlist)) },
            )
            FilterChip(
                selected = state.selectedStatus == ShowStatus.Watched,
                onClick = { viewModel.setStatus(ShowStatus.Watched) },
                label = { Text(stringResource(Res.string.status_watched)) },
            )
            Box(Modifier.weight(1f))
            IconButton(onClick = viewModel::toggleFavoritesOnly) {
                Icon(
                    imageVector = if (state.favoritesOnly) Icons.Default.Favorite
                    else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        when {
            state.isLoading -> Loading()
            state.error != null -> ErrorText(state.error.orEmpty())
            state.visible.isEmpty() -> EmptyText()
            else -> PosterGrid(
                items = state.visible.map { userShow ->
                    val total = userShow.shows.numberOfEpisodes
                    val watched = state.watchedCounts[userShow.tmdbId] ?: 0
                    PosterItem(
                        tmdbId = userShow.tmdbId,
                        title = userShow.shows.name,
                        posterPath = userShow.shows.posterPath,
                        subtitle = total?.let { "$watched/$it" },
                    )
                },
                onClick = onOpenShow,
            )
        }
    }
}

data class PosterItem(
    val tmdbId: Int,
    val title: String,
    val posterPath: String?,
    val subtitle: String?,
)

@Composable
private fun PosterGrid(items: List<PosterItem>, onClick: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = { it.tmdbId }) { item ->
            Column(
                modifier = Modifier.clickable { onClick(item.tmdbId) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AsyncImage(
                    model = TmdbImages.poster(item.posterPath),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(10.dp)),
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
                item.subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(16.dp),
    )
}

@Composable
private fun EmptyText() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(Res.string.library_empty),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
