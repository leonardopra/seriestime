package com.leonardo.seriestime.data.importer

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject

object TvTimeParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Parses a TVTime export file, detecting whether it contains movies or series:
     * series items have a "seasons" array, movie items don't.
     */
    fun parse(content: String): TvTimeExport {
        val root = json.parseToJsonElement(content)
        require(root is JsonArray) { "Expected a JSON array" }
        if (root.isEmpty()) return TvTimeExport()

        val isSeries = root.first().jsonObject.containsKey("seasons")
        return if (isSeries) {
            TvTimeExport(series = json.decodeFromJsonElement(
                kotlinx.serialization.builtins.ListSerializer(TvTimeSeries.serializer()), root
            ))
        } else {
            TvTimeExport(movies = json.decodeFromJsonElement(
                kotlinx.serialization.builtins.ListSerializer(TvTimeMovie.serializer()), root
            ))
        }
    }

    fun parseAll(contents: List<String>): TvTimeExport =
        contents.map(::parse).fold(TvTimeExport()) { acc, e -> acc + e }
}
