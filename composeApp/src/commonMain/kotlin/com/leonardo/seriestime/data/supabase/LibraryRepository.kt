package com.leonardo.seriestime.data.supabase

import com.leonardo.seriestime.data.tmdb.TmdbMovieDetails
import com.leonardo.seriestime.data.tmdb.TmdbTvDetails
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.datetime.Clock

class LibraryRepository(private val client: SupabaseClient) {

    val userId: String
        get() = client.auth.currentUserOrNull()?.id
            ?: error("Not authenticated")

    private fun now(): String = Clock.System.now().toString()

    // ---------- catalog cache ----------

    suspend fun upsertMovieCatalog(details: TmdbMovieDetails) {
        client.from("movies").upsert(
            MovieRow(
                tmdbId = details.id,
                title = details.title,
                originalTitle = details.originalTitle,
                posterPath = details.posterPath,
                releaseDate = details.releaseDate?.ifBlank { null },
                runtime = details.runtime,
                imdbId = details.externalIds?.imdbId,
            )
        )
    }

    suspend fun upsertShowCatalog(details: TmdbTvDetails) {
        client.from("shows").upsert(
            ShowRow(
                tmdbId = details.id,
                name = details.name,
                originalName = details.originalName,
                posterPath = details.posterPath,
                firstAirDate = details.firstAirDate?.ifBlank { null },
                status = details.status,
                numberOfSeasons = details.numberOfSeasons,
                numberOfEpisodes = details.numberOfEpisodes,
                tvdbId = details.externalIds?.tvdbId,
                imdbId = details.externalIds?.imdbId,
            )
        )
    }

    suspend fun upsertMovieCatalogRows(rows: List<MovieRow>) {
        if (rows.isNotEmpty()) client.from("movies").upsert(rows)
    }

    suspend fun upsertShowCatalogRows(rows: List<ShowRow>) {
        if (rows.isNotEmpty()) client.from("shows").upsert(rows)
    }

    // ---------- movies ----------

    suspend fun getUserMovies(): List<UserMovieWithMovie> =
        client.from("user_movies")
            .select(Columns.raw("*, movies(*)")) {
                filter { eq("user_id", userId) }
            }
            .decodeList()

    suspend fun getUserMovie(tmdbId: Int): UserMovieRow? =
        client.from("user_movies")
            .select {
                filter {
                    eq("user_id", userId)
                    eq("tmdb_id", tmdbId)
                }
            }
            .decodeSingleOrNull()

    suspend fun upsertUserMovie(
        tmdbId: Int,
        isWatched: Boolean,
        isFavorite: Boolean,
        rewatchCount: Int = 0,
        watchedAt: String? = null,
    ) {
        client.from("user_movies").upsert(
            UserMovieRow(
                userId = userId,
                tmdbId = tmdbId,
                isWatched = isWatched,
                isFavorite = isFavorite,
                rewatchCount = rewatchCount,
                watchedAt = watchedAt ?: if (isWatched) now() else null,
            )
        )
    }

    suspend fun upsertUserMovieRows(rows: List<UserMovieRow>) {
        if (rows.isNotEmpty()) client.from("user_movies").upsert(rows)
    }

    suspend fun deleteUserMovie(tmdbId: Int) {
        client.from("user_movies").delete {
            filter {
                eq("user_id", userId)
                eq("tmdb_id", tmdbId)
            }
        }
    }

    // ---------- shows ----------

    suspend fun getUserShows(): List<UserShowWithShow> =
        client.from("user_shows")
            .select(Columns.raw("*, shows(*)")) {
                filter { eq("user_id", userId) }
            }
            .decodeList()

    suspend fun getUserShow(tmdbId: Int): UserShowRow? =
        client.from("user_shows")
            .select {
                filter {
                    eq("user_id", userId)
                    eq("tmdb_id", tmdbId)
                }
            }
            .decodeSingleOrNull()

    suspend fun upsertUserShow(tmdbId: Int, status: ShowStatus, isFavorite: Boolean) {
        client.from("user_shows").upsert(
            UserShowRow(userId = userId, tmdbId = tmdbId, status = status, isFavorite = isFavorite)
        )
    }

    suspend fun upsertUserShowRows(rows: List<UserShowRow>) {
        if (rows.isNotEmpty()) client.from("user_shows").upsert(rows)
    }

    suspend fun deleteUserShow(tmdbId: Int) {
        client.from("user_shows").delete {
            filter {
                eq("user_id", userId)
                eq("tmdb_id", tmdbId)
            }
        }
        client.from("user_episodes").delete {
            filter {
                eq("user_id", userId)
                eq("show_tmdb_id", tmdbId)
            }
        }
    }

    // ---------- episodes ----------

    /** All watched episodes for the current user (used for progress counting). */
    suspend fun getAllWatchedEpisodes(): List<UserEpisodeRow> =
        client.from("user_episodes")
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeList()

    suspend fun getWatchedEpisodes(showTmdbId: Int): List<UserEpisodeRow> =
        client.from("user_episodes")
            .select {
                filter {
                    eq("user_id", userId)
                    eq("show_tmdb_id", showTmdbId)
                }
            }
            .decodeList()

    suspend fun markEpisodeWatched(showTmdbId: Int, season: Int, episode: Int) {
        client.from("user_episodes").upsert(
            UserEpisodeRow(
                userId = userId,
                showTmdbId = showTmdbId,
                seasonNumber = season,
                episodeNumber = episode,
                watchedAt = now(),
                watchCount = 1,
            )
        )
    }

    suspend fun unmarkEpisodeWatched(showTmdbId: Int, season: Int, episode: Int) {
        client.from("user_episodes").delete {
            filter {
                eq("user_id", userId)
                eq("show_tmdb_id", showTmdbId)
                eq("season_number", season)
                eq("episode_number", episode)
            }
        }
    }

    /** Bulk mark; episode numbers are per season. Used by "mark season/series watched" and import. */
    suspend fun markEpisodesWatched(
        showTmdbId: Int,
        episodes: List<Pair<Int, Int>>, // (season, episode)
        watchedAt: String? = null,
        chunkSize: Int = 500,
    ) {
        val ts = watchedAt ?: now()
        episodes
            .map { (season, episode) ->
                UserEpisodeRow(
                    userId = userId,
                    showTmdbId = showTmdbId,
                    seasonNumber = season,
                    episodeNumber = episode,
                    watchedAt = ts,
                    watchCount = 1,
                )
            }
            .chunked(chunkSize)
            .forEach { chunk -> client.from("user_episodes").upsert(chunk) }
    }

    suspend fun upsertUserEpisodeRows(rows: List<UserEpisodeRow>, chunkSize: Int = 500) {
        rows.chunked(chunkSize).forEach { chunk ->
            client.from("user_episodes").upsert(chunk)
        }
    }
}
