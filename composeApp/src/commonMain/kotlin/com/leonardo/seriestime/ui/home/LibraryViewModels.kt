package com.leonardo.seriestime.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leonardo.seriestime.data.supabase.LibraryRepository
import com.leonardo.seriestime.data.supabase.ShowStatus
import com.leonardo.seriestime.data.supabase.UserMovieWithMovie
import com.leonardo.seriestime.data.supabase.UserShowWithShow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MoviesTabUiState(
    val showWatched: Boolean = false,
    val favoritesOnly: Boolean = false,
    val movies: List<UserMovieWithMovie> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val visible: List<UserMovieWithMovie>
        get() = movies
            .filter { it.isWatched == showWatched }
            .filter { !favoritesOnly || it.isFavorite }
            .sortedBy { it.movies.title }
}

class MoviesTabViewModel(private val library: LibraryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MoviesTabUiState())
    val uiState: StateFlow<MoviesTabUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val movies = library.getUserMovies()
                _uiState.update { it.copy(movies = movies, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun setShowWatched(value: Boolean) = _uiState.update { it.copy(showWatched = value) }
    fun toggleFavoritesOnly() = _uiState.update { it.copy(favoritesOnly = !it.favoritesOnly) }
}

data class ShowsTabUiState(
    val selectedStatus: ShowStatus = ShowStatus.Watching,
    val favoritesOnly: Boolean = false,
    val shows: List<UserShowWithShow> = emptyList(),
    val watchedCounts: Map<Int, Int> = emptyMap(), // show tmdb_id -> watched episodes
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val visible: List<UserShowWithShow>
        get() = shows
            .filter { it.status == selectedStatus }
            .filter { !favoritesOnly || it.isFavorite }
            .sortedBy { it.shows.name }
}

class ShowsTabViewModel(private val library: LibraryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ShowsTabUiState())
    val uiState: StateFlow<ShowsTabUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val shows = library.getUserShows()
                val counts = library.getAllWatchedEpisodes()
                    .groupingBy { it.showTmdbId }
                    .eachCount()
                _uiState.update {
                    it.copy(shows = shows, watchedCounts = counts, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun setStatus(status: ShowStatus) = _uiState.update { it.copy(selectedStatus = status) }
    fun toggleFavoritesOnly() = _uiState.update { it.copy(favoritesOnly = !it.favoritesOnly) }
}
