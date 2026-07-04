package com.leonardo.seriestime

import com.leonardo.seriestime.data.importer.TvTimeEpisode
import com.leonardo.seriestime.data.importer.TvTimeSeason
import com.leonardo.seriestime.data.importer.deriveShowStatus
import com.leonardo.seriestime.data.importer.matchEpisodes
import com.leonardo.seriestime.data.importer.normalizeTimestamp
import com.leonardo.seriestime.data.supabase.ShowStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class TvTimeMatcherTest {

    private fun episode(number: Int, watched: Boolean = true) = TvTimeEpisode(
        number = number,
        isWatched = watched,
        watchedAt = "2023-08-23 14:12:05",
        watchedCount = 1,
    )

    @Test
    fun matchesEpisodesBySeasonAndNumber() {
        val seasons = listOf(
            TvTimeSeason(number = 1, episodes = listOf(episode(1), episode(2), episode(3))),
        )
        val valid = setOf(1 to 1, 1 to 2) // ep 3 missing on TMDB
        val result = matchEpisodes(seasons, valid)
        assertEquals(2, result.matched.size)
        assertEquals(listOf(1 to 3), result.unmatched)
    }

    @Test
    fun skipsUnwatchedEpisodes() {
        val seasons = listOf(
            TvTimeSeason(number = 1, episodes = listOf(episode(1), episode(2, watched = false))),
        )
        val result = matchEpisodes(seasons, setOf(1 to 1, 1 to 2))
        assertEquals(1, result.matched.size)
        assertEquals(0, result.unmatched.size)
    }

    @Test
    fun matchesSpecialsAsSeasonZero() {
        val seasons = listOf(
            TvTimeSeason(number = 0, isSpecials = true, episodes = listOf(episode(1))),
        )
        val result = matchEpisodes(seasons, setOf(0 to 1))
        assertEquals(1, result.matched.size)
    }

    @Test
    fun statusDerivation() {
        assertEquals(ShowStatus.Watchlist, deriveShowStatus("not_started_yet", "Returning Series", false))
        assertEquals(ShowStatus.Watching, deriveShowStatus("continuing", "Returning Series", false))
        assertEquals(ShowStatus.Watching, deriveShowStatus("up_to_date", "Returning Series", true))
        assertEquals(ShowStatus.Watched, deriveShowStatus("up_to_date", "Ended", true))
        assertEquals(ShowStatus.Watched, deriveShowStatus("continuing", "Canceled", true))
        assertEquals(ShowStatus.Watching, deriveShowStatus("up_to_date", "Ended", false))
    }

    @Test
    fun timestampNormalization() {
        assertEquals("2023-08-23T14:12:05Z", "2023-08-23 14:12:05".normalizeTimestamp())
        assertEquals("2023-08-22T20:19:11Z", "2023-08-22T20:19:11Z".normalizeTimestamp())
    }
}
