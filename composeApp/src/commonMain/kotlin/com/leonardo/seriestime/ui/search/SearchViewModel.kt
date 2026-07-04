package com.leonardo.seriestime.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leonardo.seriestime.data.tmdb.TmdbApi
import com.leonardo.seriestime.data.tmdb.TmdbSearchResult
import com.leonardo.seriestime.data.tmdb.tmdbLanguage
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<TmdbSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class SearchViewModel(private val tmdbApi: TmdbApi) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState

    private val queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            @OptIn(FlowPreview::class)
            queryFlow.debounce(400).collect { query ->
                if (query.length < 2) {
                    _uiState.update { it.copy(results = emptyList(), isLoading = false) }
                } else {
                    search(query)
                }
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        queryFlow.value = query
    }

    private suspend fun search(query: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val results = tmdbApi.searchMulti(query, tmdbLanguage())
            _uiState.update { it.copy(results = results, isLoading = false) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = e.message) }
        }
    }
}
