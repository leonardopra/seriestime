package com.leonardo.seriestime

import com.leonardo.seriestime.data.importer.TvTimeParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TvTimeParserTest {

    private val moviesJson = """
        [
          {
            "id": {"tvdb": 111647, "imdb": "tt0163025"},
            "uuid": "ffc9cb1e-7054-4aac-9c87-813b15c8fe3d",
            "created_at": "2023-08-22T20:19:11Z",
            "title": "Jurassic Park III",
            "year": 2001,
            "watched_at": "2023-08-22T20:19:11Z",
            "is_watched": true,
            "is_favorite": false,
            "rewatch_count": 2
          },
          {
            "id": {"tvdb": 12, "imdb": null},
            "title": "Unknown Watchlist Movie",
            "year": 2024,
            "is_watched": false,
            "is_favorite": true,
            "rewatch_count": 0
          }
        ]
    """.trimIndent()

    private val seriesJson = """
        [
          {
            "uuid": "ffabbc6b",
            "id": {"tvdb": 422600, "imdb": null},
            "created_at": "2023-08-23T14:12:05.176564Z",
            "title": "Trainwreck",
            "status": "up_to_date",
            "is_favorite": false,
            "_noEpisodeData": false,
            "seasons": [
              {
                "number": 0,
                "is_specials": true,
                "episodes": [
                  {"id": {"tvdb": 1}, "number": 1, "name": "Special", "special": true,
                   "is_watched": true, "watched_at": "2023-08-23 14:12:05",
                   "rewatch_count": 0, "watched_count": 1}
                ]
              },
              {
                "number": 1,
                "is_specials": false,
                "episodes": [
                  {"id": {"tvdb": 9271101}, "number": 1, "name": "Ep 1", "special": false,
                   "is_watched": true, "watched_at": "2023-08-23 14:12:05",
                   "rewatch_count": 1, "watched_count": 2},
                  {"id": {"tvdb": 9288277}, "number": 2, "name": "Ep 2", "special": false,
                   "is_watched": false, "watched_at": null,
                   "rewatch_count": 0, "watched_count": 0}
                ]
              }
            ]
          }
        ]
    """.trimIndent()

    @Test
    fun parsesMoviesFile() {
        val export = TvTimeParser.parse(moviesJson)
        assertEquals(2, export.movies.size)
        assertEquals(0, export.series.size)
        val first = export.movies.first()
        assertEquals("Jurassic Park III", first.title)
        assertEquals("tt0163025", first.id.imdb)
        assertEquals(111647, first.id.tvdb)
        assertTrue(first.isWatched)
        assertEquals(2, first.rewatchCount)
        val second = export.movies[1]
        assertFalse(second.isWatched)
        assertTrue(second.isFavorite)
        assertEquals(null, second.id.imdb)
    }

    @Test
    fun parsesSeriesFileWithSpecialsAndUnknownKeys() {
        val export = TvTimeParser.parse(seriesJson)
        assertEquals(1, export.series.size)
        val series = export.series.first()
        assertEquals("up_to_date", series.status)
        assertEquals(2, series.seasons.size)
        assertTrue(series.seasons.first().isSpecials)
        assertEquals(3, export.episodeCount)
    }

    @Test
    fun mergesBothFilesRegardlessOfOrder() {
        val export = TvTimeParser.parseAll(listOf(seriesJson, moviesJson))
        assertEquals(2, export.movies.size)
        assertEquals(1, export.series.size)
    }

    @Test
    fun emptyArrayYieldsEmptyExport() {
        val export = TvTimeParser.parse("[]")
        assertEquals(0, export.movies.size)
        assertEquals(0, export.series.size)
    }
}
