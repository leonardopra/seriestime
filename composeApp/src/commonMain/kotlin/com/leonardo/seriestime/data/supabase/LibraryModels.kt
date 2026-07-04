package com.leonardo.seriestime.data.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MovieRow(
    @SerialName("tmdb_id") val tmdbId: Int,
    val title: String,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val runtime: Int? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
)

@Serializable
data class ShowRow(
    @SerialName("tmdb_id") val tmdbId: Int,
    val name: String,
    @SerialName("original_name") val originalName: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    val status: String? = null,
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int? = null,
    @SerialName("tvdb_id") val tvdbId: Int? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
)

@Serializable
enum class ShowStatus {
    @SerialName("watchlist") Watchlist,
    @SerialName("watching") Watching,
    @SerialName("watched") Watched,
}

@Serializable
data class UserMovieRow(
    @SerialName("user_id") val userId: String,
    @SerialName("tmdb_id") val tmdbId: Int,
    @SerialName("is_watched") val isWatched: Boolean = false,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("rewatch_count") val rewatchCount: Int = 0,
    @SerialName("watched_at") val watchedAt: String? = null,
)

@Serializable
data class UserMovieWithMovie(
    @SerialName("user_id") val userId: String,
    @SerialName("tmdb_id") val tmdbId: Int,
    @SerialName("is_watched") val isWatched: Boolean = false,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("rewatch_count") val rewatchCount: Int = 0,
    @SerialName("watched_at") val watchedAt: String? = null,
    val movies: MovieRow,
)

@Serializable
data class UserShowRow(
    @SerialName("user_id") val userId: String,
    @SerialName("tmdb_id") val tmdbId: Int,
    val status: ShowStatus = ShowStatus.Watchlist,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
)

@Serializable
data class UserShowWithShow(
    @SerialName("user_id") val userId: String,
    @SerialName("tmdb_id") val tmdbId: Int,
    val status: ShowStatus = ShowStatus.Watchlist,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    val shows: ShowRow,
)

@Serializable
data class UserEpisodeRow(
    @SerialName("user_id") val userId: String,
    @SerialName("show_tmdb_id") val showTmdbId: Int,
    @SerialName("season_number") val seasonNumber: Int,
    @SerialName("episode_number") val episodeNumber: Int,
    @SerialName("watched_at") val watchedAt: String? = null,
    @SerialName("watch_count") val watchCount: Int = 1,
)
