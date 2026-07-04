package com.leonardo.seriestime.data.importer

import com.leonardo.seriestime.data.supabase.LibraryRepository
import com.leonardo.seriestime.data.supabase.MovieRow
import com.leonardo.seriestime.data.supabase.ShowRow
import com.leonardo.seriestime.data.supabase.UserEpisodeRow
import com.leonardo.seriestime.data.supabase.UserMovieRow
import com.leonardo.seriestime.data.supabase.UserShowRow
import com.leonardo.seriestime.data.tmdb.TmdbLookup
import com.leonardo.seriestime.data.tmdb.TmdbSearchResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

enum class ImportPhase { MatchingMovies, MatchingSeries, Uploading, Done }

data class ImportProgress(
    val phase: ImportPhase,
    val done: Int,
    val total: Int,
)

data class UnmatchedItem(val title: String, val year: Int? = null, val reason: String? = null)

data class SeriesImportSummary(
    val title: String,
    val matchedEpisodes: Int,
    val unmatchedEpisodes: Int,
)

data class ImportReport(
    val matchedMovies: Int = 0,
    val unmatchedMovies: List<UnmatchedItem> = emptyList(),
    val matchedSeries: Int = 0,
    val unmatchedSeries: List<UnmatchedItem> = emptyList(),
    val seriesSummaries: List<SeriesImportSummary> = emptyList(),
    val importedEpisodes: Int = 0,
    val dryRun: Boolean = false,
)

class TvTimeImporter(
    private val tmdb: TmdbLookup,
    private val library: LibraryRepository,
) {
    suspend fun import(
        export: TvTimeExport,
        dryRun: Boolean = false,
        language: String = "en-US",
        onProgress: (ImportProgress) -> Unit = {},
    ): ImportReport {
        // -------- movies --------
        val movieRows = mutableListOf<MovieRow>()
        val userMovieRows = mutableListOf<UserMovieRow>()
        val unmatchedMovies = mutableListOf<UnmatchedItem>()
        var moviesDone = 0

        coroutineScope {
            val semaphore = Semaphore(8)
            val mutex = Mutex()
            export.movies.map { movie ->
                async {
                    semaphore.withPermit {
                        val match = matchMovie(movie)
                        mutex.withLock {
                            moviesDone++
                            onProgress(
                                ImportProgress(ImportPhase.MatchingMovies, moviesDone, export.movies.size)
                            )
                            if (match != null) {
                                movieRows += MovieRow(
                                    tmdbId = match.id,
                                    title = match.displayTitle.ifBlank { movie.title },
                                    posterPath = match.posterPath,
                                    releaseDate = match.releaseDate?.ifBlank { null },
                                    imdbId = movie.id.imdb,
                                )
                                userMovieRows += UserMovieRow(
                                    userId = library.userId,
                                    tmdbId = match.id,
                                    isWatched = movie.isWatched,
                                    isFavorite = movie.isFavorite,
                                    rewatchCount = movie.rewatchCount,
                                    watchedAt = movie.watchedAt?.normalizeTimestamp(),
                                )
                            } else {
                                unmatchedMovies += UnmatchedItem(movie.title, movie.year, "no TMDB match")
                            }
                        }
                    }
                }
            }.awaitAll()
        }

        // -------- series --------
        val showRows = mutableListOf<ShowRow>()
        val userShowRows = mutableListOf<UserShowRow>()
        val userEpisodeRows = mutableListOf<UserEpisodeRow>()
        val unmatchedSeries = mutableListOf<UnmatchedItem>()
        val seriesSummaries = mutableListOf<SeriesImportSummary>()

        export.series.forEachIndexed { index, series ->
            onProgress(ImportProgress(ImportPhase.MatchingSeries, index, export.series.size))
            try {
                val tvId = matchSeries(series)
                if (tvId == null) {
                    unmatchedSeries += UnmatchedItem(series.title, reason = "no TMDB match")
                    return@forEachIndexed
                }
                val details = tmdb.tvDetails(tvId, language)

                // fetch only the seasons that appear in the export
                val validEpisodes = buildSet {
                    for (seasonNumber in series.seasons.map { it.number }.distinct()) {
                        try {
                            val season = tmdb.seasonDetails(tvId, seasonNumber, language)
                            season.episodes.forEach { add(seasonNumber to it.episodeNumber) }
                        } catch (_: Exception) {
                            // season missing on TMDB: its episodes end up unmatched
                        }
                    }
                }

                val result = matchEpisodes(series.seasons, validEpisodes)
                val totalWatchable = series.seasons.sumOf { s -> s.episodes.count { it.isWatched } }
                val allWatched = details.numberOfEpisodes > 0 &&
                    result.matched.size >= details.numberOfEpisodes

                showRows += ShowRow(
                    tmdbId = tvId,
                    name = details.name.ifBlank { series.title },
                    originalName = details.originalName,
                    posterPath = details.posterPath,
                    firstAirDate = details.firstAirDate?.ifBlank { null },
                    status = details.status,
                    numberOfSeasons = details.numberOfSeasons,
                    numberOfEpisodes = details.numberOfEpisodes,
                    tvdbId = series.id.tvdb,
                    imdbId = series.id.imdb,
                )
                userShowRows += UserShowRow(
                    userId = library.userId,
                    tmdbId = tvId,
                    status = deriveShowStatus(series.status, details.status, allWatched),
                    isFavorite = series.isFavorite,
                )
                userEpisodeRows += result.matched.map {
                    UserEpisodeRow(
                        userId = library.userId,
                        showTmdbId = tvId,
                        seasonNumber = it.season,
                        episodeNumber = it.episode,
                        watchedAt = it.watchedAt,
                        watchCount = it.watchCount,
                    )
                }
                seriesSummaries += SeriesImportSummary(
                    title = series.title,
                    matchedEpisodes = result.matched.size,
                    unmatchedEpisodes = totalWatchable - result.matched.size,
                )
            } catch (e: Exception) {
                unmatchedSeries += UnmatchedItem(series.title, reason = e.message)
            }
        }

        // -------- upload --------
        if (!dryRun) {
            onProgress(ImportProgress(ImportPhase.Uploading, 0, 4))
            library.upsertMovieCatalogRows(movieRows)
            onProgress(ImportProgress(ImportPhase.Uploading, 1, 4))
            library.upsertShowCatalogRows(showRows)
            library.upsertUserMovieRows(userMovieRows)
            onProgress(ImportProgress(ImportPhase.Uploading, 2, 4))
            library.upsertUserShowRows(userShowRows)
            onProgress(ImportProgress(ImportPhase.Uploading, 3, 4))
            library.upsertUserEpisodeRows(userEpisodeRows)
            onProgress(ImportProgress(ImportPhase.Uploading, 4, 4))
        }
        onProgress(ImportProgress(ImportPhase.Done, 1, 1))

        return ImportReport(
            matchedMovies = userMovieRows.size,
            unmatchedMovies = unmatchedMovies,
            matchedSeries = userShowRows.size,
            unmatchedSeries = unmatchedSeries,
            seriesSummaries = seriesSummaries.sortedByDescending { it.unmatchedEpisodes },
            importedEpisodes = userEpisodeRows.size,
            dryRun = dryRun,
        )
    }

    private suspend fun matchMovie(movie: TvTimeMovie): TmdbSearchResult? {
        movie.id.imdb?.let { imdb ->
            runCatching { tmdb.findByImdbId(imdb) }.getOrNull()
                ?.movieResults?.firstOrNull()?.let { return it }
        }
        movie.id.tvdb?.let { tvdb ->
            runCatching { tmdb.findByTvdbId(tvdb) }.getOrNull()
                ?.movieResults?.firstOrNull()?.let { return it }
        }
        // last resort: title + year search
        val results = runCatching { tmdb.searchMulti(movie.title, "en-US") }.getOrNull() ?: return null
        return results
            .filter { it.mediaType == "movie" }
            .firstOrNull { result ->
                movie.year == null || result.year?.toIntOrNull()?.let { y ->
                    kotlin.math.abs(y - movie.year) <= 1
                } ?: false
            }
    }

    private suspend fun matchSeries(series: TvTimeSeries): Int? {
        series.id.tvdb?.let { tvdb ->
            runCatching { tmdb.findByTvdbId(tvdb) }.getOrNull()
                ?.tvResults?.firstOrNull()?.let { return it.id }
        }
        series.id.imdb?.let { imdb ->
            runCatching { tmdb.findByImdbId(imdb) }.getOrNull()
                ?.tvResults?.firstOrNull()?.let { return it.id }
        }
        val results = runCatching { tmdb.searchMulti(series.title, "en-US") }.getOrNull() ?: return null
        return results.firstOrNull { it.mediaType == "tv" }?.id
    }
}
