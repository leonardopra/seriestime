package com.leonardo.seriestime.data.importer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TvTimeIds(
    val tvdb: Int? = null,
    val imdb: String? = null,
)

@Serializable
data class TvTimeMovie(
    val id: TvTimeIds = TvTimeIds(),
    val uuid: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val title: String = "",
    val year: Int? = null,
    @SerialName("watched_at") val watchedAt: String? = null,
    @SerialName("is_watched") val isWatched: Boolean = false,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("rewatch_count") val rewatchCount: Int = 0,
)

@Serializable
data class TvTimeSeries(
    val uuid: String? = null,
    val id: TvTimeIds = TvTimeIds(),
    @SerialName("created_at") val createdAt: String? = null,
    val title: String = "",
    val status: String? = null, // up_to_date | continuing | not_started_yet
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    val seasons: List<TvTimeSeason> = emptyList(),
)

@Serializable
data class TvTimeSeason(
    val number: Int,
    @SerialName("is_specials") val isSpecials: Boolean = false,
    val episodes: List<TvTimeEpisode> = emptyList(),
)

@Serializable
data class TvTimeEpisode(
    val id: TvTimeIds = TvTimeIds(),
    val number: Int,
    val name: String? = null,
    val special: Boolean = false,
    @SerialName("is_watched") val isWatched: Boolean = false,
    @SerialName("watched_at") val watchedAt: String? = null,
    @SerialName("rewatch_count") val rewatchCount: Int = 0,
    @SerialName("watched_count") val watchedCount: Int = 1,
)

/** Content of one or both picked export files. */
data class TvTimeExport(
    val movies: List<TvTimeMovie> = emptyList(),
    val series: List<TvTimeSeries> = emptyList(),
) {
    val episodeCount: Int
        get() = series.sumOf { s -> s.seasons.sumOf { it.episodes.size } }

    operator fun plus(other: TvTimeExport) = TvTimeExport(
        movies = movies + other.movies,
        series = series + other.series,
    )
}
