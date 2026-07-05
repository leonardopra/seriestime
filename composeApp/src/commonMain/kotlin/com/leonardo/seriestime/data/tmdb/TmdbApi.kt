package com.leonardo.seriestime.data.tmdb

import com.leonardo.seriestime.BuildKonfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

interface TmdbLookup {
    suspend fun searchMulti(query: String, language: String): List<TmdbSearchResult>
    suspend fun tvDetails(id: Int, language: String): TmdbTvDetails
    suspend fun seasonDetails(tvId: Int, seasonNumber: Int, language: String): TmdbSeasonDetails
    suspend fun findByImdbId(imdbId: String): TmdbFindResponse
    suspend fun findByTvdbId(tvdbId: Int): TmdbFindResponse
}

class TmdbApi(
    private val client: HttpClient = defaultClient(),
    private val accessToken: String = BuildKonfig.TMDB_ACCESS_TOKEN,
) : TmdbLookup {
    // 32 hex chars = legacy v3 api key (goes in the query string);
    // anything longer is a v4 read access token (Bearer header)
    private val isV3Key = accessToken.length == 32 && accessToken.all { it.isDigit() || it in 'a'..'f' }

    private val seasonCache = LruCache<String, TmdbSeasonDetails>(maxSize = 64)
    private val tvCache = LruCache<String, TmdbTvDetails>(maxSize = 64)
    private val cacheMutex = Mutex()

    override suspend fun searchMulti(query: String, language: String): List<TmdbSearchResult> {
        val page: TmdbPage<TmdbSearchResult> = get("search/multi", language) {
            parameter("query", query)
            parameter("include_adult", "false")
        }
        return page.results.filter { it.mediaType == "movie" || it.mediaType == "tv" }
    }

    suspend fun movieDetails(id: Int, language: String): TmdbMovieDetails =
        get("movie/$id", language) {
            parameter("append_to_response", "external_ids")
        }

    override suspend fun tvDetails(id: Int, language: String): TmdbTvDetails {
        val key = "$id-$language"
        cacheMutex.withLock { tvCache.get(key) }?.let { return it }
        val details: TmdbTvDetails = get("tv/$id", language) {
            parameter("append_to_response", "external_ids")
        }
        cacheMutex.withLock { tvCache.put(key, details) }
        return details
    }

    override suspend fun seasonDetails(tvId: Int, seasonNumber: Int, language: String): TmdbSeasonDetails {
        val key = "$tvId-$seasonNumber-$language"
        cacheMutex.withLock { seasonCache.get(key) }?.let { return it }
        val details: TmdbSeasonDetails = get("tv/$tvId/season/$seasonNumber", language)
        cacheMutex.withLock { seasonCache.put(key, details) }
        return details
    }

    override suspend fun findByImdbId(imdbId: String): TmdbFindResponse =
        get("find/$imdbId", language = null) {
            parameter("external_source", "imdb_id")
        }

    override suspend fun findByTvdbId(tvdbId: Int): TmdbFindResponse =
        get("find/$tvdbId", language = null) {
            parameter("external_source", "tvdb_id")
        }

    private suspend inline fun <reified T> get(
        path: String,
        language: String?,
        crossinline extra: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {},
    ): T {
        var attempt = 0
        while (true) {
            val response: HttpResponse = client.get("$BASE_URL/$path") {
                if (isV3Key) parameter("api_key", accessToken)
                else header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (language != null) parameter("language", language)
                extra()
            }
            if (response.status == HttpStatusCode.TooManyRequests && attempt < MAX_RETRIES) {
                val retryAfter = response.headers[HttpHeaders.RetryAfter]?.toLongOrNull() ?: 1L
                delay(retryAfter * 1000)
                attempt++
                continue
            }
            if (!response.status.isSuccess()) {
                throw TmdbException("TMDB ${response.status.value} on $path")
            }
            return response.body()
        }
    }

    companion object {
        private const val BASE_URL = "https://api.themoviedb.org/3"
        private const val MAX_RETRIES = 5

        fun defaultClient(): HttpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
        }
    }
}

class TmdbException(message: String) : Exception(message)

private fun HttpStatusCode.isSuccess() = value in 200..299

/** Minimal LRU cache backed by an access-ordered LinkedHashMap-like structure. */
class LruCache<K, V>(private val maxSize: Int) {
    private val map = LinkedHashMap<K, V>()

    fun get(key: K): V? {
        val value = map.remove(key) ?: return null
        map[key] = value // re-insert to mark as most recently used
        return value
    }

    fun put(key: K, value: V) {
        map.remove(key)
        map[key] = value
        while (map.size > maxSize) {
            map.remove(map.keys.first())
        }
    }
}
