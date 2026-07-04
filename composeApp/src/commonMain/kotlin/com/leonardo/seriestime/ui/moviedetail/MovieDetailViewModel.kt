package com.leonardo.seriestime.ui.moviedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leonardo.seriestime.data.supabase.LibraryRepository
import com.leonardo.seriestime.data.supabase.UserMovieRow
import com.leonardo.seriestime.data.tmdb.TmdbApi
import com.leonardo.seriestime.data.tmdb.TmdbMovieDetails
import com.leonardo.seriestime.data.tmdb.tmdbLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MovieDetailUiState(
    val details: TmdbMovieDetails? = null,
    val isWatched: Boolean = false,
    val isFavorite: Boolean = false,
    val inWatchlist: Boolean = false,
    val rewatchCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
)

class MovieDetailViewModel(
    private val tmdbId: Int,
    private val tmdbApi: TmdbApi,
    private val library: LibraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovieDetailUiState())
    val uiState: StateFlow<MovieDetailUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val details = tmdbApi.movieDetails(tmdbId, tmdbLanguage())
                val userRow = library.getUserMovie(tmdbId)
                _uiState.update {
                    it.copy(
                        details = details,
                        isWatched = userRow?.isWatched ?: false,
                        isFavorite = userRow?.isFavorite ?: false,
                        inWatchlist = userRow != null && !(userRow.isWatched),
                        rewatchCount = userRow?.rewatchCount ?: 0,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun toggleWatched() = mutate { state ->
        val watched = !state.isWatched
        library.upsertUserMovie(
            tmdbId = tmdbId,
            isWatched = watched,
            isFavorite = state.isFavorite,
            rewatchCount = if (watched) state.rewatchCount else 0,
        )
        state.copy(isWatched = watched, inWatchlist = !watched && (state.inWatchlist || state.isFavorite))
    }

    fun toggleWatchlist() = mutate { state ->
        if (state.inWatchlist) {
            if (!state.isWatched && !state.isFavorite) {
                library.deleteUserMovie(tmdbId)
            }
            state.copy(inWatchlist = false)
        } else {
            library.upsertUserMovie(
                tmdbId = tmdbId,
                isWatched = false,
                isFavorite = state.isFavorite,
            )
            state.copy(inWatchlist = true, isWatched = false)
        }
    }

    fun toggleFavorite() = mutate { state ->
        library.upsertUserMovie(
            tmdbId = tmdbId,
            isWatched = state.isWatched,
            isFavorite = !state.isFavorite,
            rewatchCount = state.rewatchCount,
        )
        state.copy(isFavorite = !state.isFavorite)
    }

    fun incrementRewatch() = mutate { state ->
        val count = state.rewatchCount + 1
        library.upsertUserMovie(
            tmdbId = tmdbId,
            isWatched = true,
            isFavorite = state.isFavorite,
            rewatchCount = count,
        )
        state.copy(rewatchCount = count, isWatched = true)
    }

    private fun mutate(block: suspend (MovieDetailUiState) -> MovieDetailUiState) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.details == null) return@launch
            try {
                // the catalog row must exist before any user_movies upsert (FK)
                library.upsertMovieCatalog(state.details)
                _uiState.value = block(state)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
