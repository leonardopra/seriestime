package com.leonardo.seriestime.data.importer

import com.leonardo.seriestime.data.supabase.ShowStatus

/** Pure matching/derivation logic, unit-testable without network access. */

data class EpisodeMatch(
    val season: Int,
    val episode: Int,
    val watchedAt: String?,
    val watchCount: Int,
)

data class EpisodeMatchResult(
    val matched: List<EpisodeMatch>,
    val unmatched: List<Pair<Int, Int>>, // (season, episode) present in export but not on TMDB
)

/**
 * Matches the watched episodes of a TVTime series against the set of
 * (season, episode) pairs that exist on TMDB.
 */
fun matchEpisodes(
    seasons: List<TvTimeSeason>,
    validEpisodes: Set<Pair<Int, Int>>,
): EpisodeMatchResult {
    val matched = mutableListOf<EpisodeMatch>()
    val unmatched = mutableListOf<Pair<Int, Int>>()
    for (season in seasons) {
        for (episode in season.episodes) {
            if (!episode.isWatched) continue
            val key = season.number to episode.number
            if (key in validEpisodes) {
                matched += EpisodeMatch(
                    season = season.number,
                    episode = episode.number,
                    watchedAt = episode.watchedAt?.normalizeTimestamp(),
                    watchCount = episode.watchedCount.coerceAtLeast(1),
                )
            } else {
                unmatched += key
            }
        }
    }
    return EpisodeMatchResult(matched, unmatched)
}

/**
 * TVTime status + TMDB status + completeness -> library status.
 * up_to_date/continuing -> watching; not_started_yet -> watchlist;
 * everything watched on an ended show -> watched.
 */
fun deriveShowStatus(
    tvTimeStatus: String?,
    tmdbStatus: String?,
    allEpisodesWatched: Boolean,
): ShowStatus = when {
    tvTimeStatus == "not_started_yet" -> ShowStatus.Watchlist
    allEpisodesWatched && tmdbStatus in setOf("Ended", "Canceled") -> ShowStatus.Watched
    else -> ShowStatus.Watching
}

/** "2023-08-23 14:12:05" (TVTime episode format) -> ISO-ish accepted by Postgres. */
fun String.normalizeTimestamp(): String =
    if (contains(' ') && !contains('T')) replace(' ', 'T') + "Z" else this
