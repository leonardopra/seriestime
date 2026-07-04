package com.leonardo.seriestime.ui.showdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leonardo.seriestime.data.supabase.LibraryRepository
import com.leonardo.seriestime.data.supabase.ShowStatus
import com.leonardo.seriestime.data.tmdb.TmdbApi
import com.leonardo.seriestime.data.tmdb.TmdbSeasonDetails
import com.leonardo.seriestime.data.tmdb.TmdbTvDetails
import com.leonardo.seriestime.data.tmdb.tmdbLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShowDetailUiState(
    val details: TmdbTvDetails? = null,
    val status: ShowStatus? = null, // null = not in library
    val isFavorite: Boolean = false,
    val watched: Set<Pair<Int, Int>> = emptySet(), // (season, episode)
    val loadedSeasons: Map<Int, TmdbSeasonDetails> = emptyMap(),
    val expandedSeason: Int? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    fun watchedCountInSeason(season: Int): Int = watched.count { it.first == season }
}

class ShowDetailViewModel(
    private val tmdbId: Int,
    private val tmdbApi: TmdbApi,
    private val library: LibraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShowDetailUiState())
    val uiState: StateFlow<ShowDetailUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val details = tmdbApi.tvDetails(tmdbId, tmdbLanguage())
                val userShow = library.getUserShow(tmdbId)
                val watched = if (userShow != null) {
                    library.getWatchedEpisodes(tmdbId)
                        .map { it.seasonNumber to it.episodeNumber }
                        .toSet()
                } else emptySet()
                _uiState.update {
                    it.copy(
                        details = details,
                        status = userShow?.status,
                        isFavorite = userShow?.isFavorite ?: false,
                        watched = watched,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun toggleSeason(seasonNumber: Int) {
        val state = _uiState.value
        if (state.expandedSeason == seasonNumber) {
            _uiState.update { it.copy(expandedSeason = null) }
            return
        }
        _uiState.update { it.copy(expandedSeason = seasonNumber) }
        if (!state.loadedSeasons.containsKey(seasonNumber)) {
            viewModelScope.launch {
                try {
                    val season = tmdbApi.seasonDetails(tmdbId, seasonNumber, tmdbLanguage())
                    _uiState.update {
                        it.copy(loadedSeasons = it.loadedSeasons + (seasonNumber to season))
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.message) }
                }
            }
        }
    }

    fun setStatus(status: ShowStatus) = mutate { state ->
        library.upsertUserShow(tmdbId, status, state.isFavorite)
        state.copy(status = status)
    }

    fun toggleFavorite() = mutate { state ->
        val status = state.status ?: ShowStatus.Watchlist
        library.upsertUserShow(tmdbId, status, !state.isFavorite)
        state.copy(status = status, isFavorite = !state.isFavorite)
    }

    fun toggleEpisode(season: Int, episode: Int) = mutate { state ->
        val key = season to episode
        if (key in state.watched) {
            library.unmarkEpisodeWatched(tmdbId, season, episode)
            state.copy(watched = state.watched - key)
        } else {
            ensureWatching(state)
            library.markEpisodeWatched(tmdbId, season, episode)
            state.copy(watched = state.watched + key, status = state.status ?: ShowStatus.Watching)
        }
    }

    fun markSeasonWatched(seasonNumber: Int) = mutate { state ->
        val season = state.loadedSeasons[seasonNumber]
            ?: tmdbApi.seasonDetails(tmdbId, seasonNumber, tmdbLanguage())
        ensureWatching(state)
        val episodes = season.episodes.map { seasonNumber to it.episodeNumber }
        library.markEpisodesWatched(tmdbId, episodes)
        state.copy(
            watched = state.watched + episodes,
            loadedSeasons = state.loadedSeasons + (seasonNumber to season),
            status = state.status ?: ShowStatus.Watching,
        )
    }

    fun markSeriesWatched() = mutate { state ->
        val details = state.details ?: return@mutate state
        ensureWatching(state)
        var watched = state.watched
        for (summary in details.seasons.filter { it.seasonNumber > 0 }) {
            val season = state.loadedSeasons[summary.seasonNumber]
                ?: tmdbApi.seasonDetails(tmdbId, summary.seasonNumber, tmdbLanguage())
            val episodes = season.episodes.map { summary.seasonNumber to it.episodeNumber }
            library.markEpisodesWatched(tmdbId, episodes)
            watched = watched + episodes
        }
        library.upsertUserShow(tmdbId, ShowStatus.Watched, state.isFavorite)
        state.copy(watched = watched, status = ShowStatus.Watched)
    }

    private suspend fun ensureWatching(state: ShowDetailUiState) {
        if (state.status == null) {
            library.upsertUserShow(tmdbId, ShowStatus.Watching, state.isFavorite)
        }
    }

    private fun mutate(block: suspend (ShowDetailUiState) -> ShowDetailUiState) {
        viewModelScope.launch {
            val state = _uiState.value
            val details = state.details ?: return@launch
            try {
                // catalog row must exist before any user_* upsert (FK)
                library.upsertShowCatalog(details)
                _uiState.value = block(state)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
