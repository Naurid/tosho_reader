package com.naurid.tosho.source.core.atsumaru

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.naurid.tosho.domain.model.Book
import com.naurid.tosho.domain.model.BookStatus
import com.naurid.tosho.domain.model.Chapter
import com.naurid.tosho.domain.model.Page
import com.naurid.tosho.domain.model.SourceFilterList
import com.naurid.tosho.source.core.SourcePlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.Locale

/**
 * UI-tailored data model for Latest Updates cards, respecting separation of concerns
 * between back-end scraping/mapping and front-end presentation.
 */
data class LatestUpdateUiModel(
    val id: String,
    val title: String,
    val coverUrl: String?,
    val viewerCount: String,
    val rating: String,
    val type: String,
    val timeAgo: String,
    val url: String
)

data class PopularUIModel(
    val id: String,
    val title: String,
    val coverUrl: String?,
    val viewerCount: String,
    val rating: String,
    val type: String,
    val url: String
)

class AtsumaruSourcePlugin : SourcePlugin {

    override val id: String = "atsumaru"
    override val name: String = "Atsumaru"
    override val version: String = "1.0.0"
    override val lang: String = "en"
    override val iconUrl: String? = null

    private val client = OkHttpClient.Builder().build()
    private val baseUrl = "https://atsu.moe"

    // Caching and Rate Limiting / Throttling state to respect robots.txt and server performance
    private var cachedLatestBooks: List<Book> = emptyList()
    private var cachedPopularBooks: List<Book> = emptyList()
    private var lastFetchTimestamp: Long = 0L
    private var lastManualRefreshTimestamp: Long = 0L
    private val cacheDurationMs = 30 * 60 * 1000L // 30 minutes cache on app open
    private val refreshCooldownMs = 2 * 60 * 1000L // 2 minutes cooldown between manual refreshes

    override fun getImageHeaders(): Map<String, String> {
        return mapOf(
            "User-Agent" to "ToshoApp/1.0 (+https://atsu.moe; mobile manga reader client)",
            "Referer" to "$baseUrl/"
        )
    }

    override suspend fun getPopularBooks(page: Int): Result<List<Book>> = getLatestUpdates(page)

    override suspend fun getLatestUpdates(page: Int): Result<List<Book>> = runCatching {
        val uiModelsResult = getLatestUpdatesUiModels(page)
        val uiModels = uiModelsResult.getOrThrow()
        uiModels.map { model ->
            Book(
                id = model.id,
                sourceId = id,
                url = model.url,
                title = model.title,
                coverUrl = model.coverUrl,
                status = BookStatus.UNKNOWN
            )
        }
    }

    /**
     * Back-end data mapping function: Fetches from Atsumaru JSON API and maps into UI-tailored [LatestUpdateUiModel] items with pagination.
     */
    suspend fun getLatestUpdatesUiModels(page: Int = 1): Result<List<LatestUpdateUiModel>> = runCatching {
        val currentTime = System.currentTimeMillis()

        // Return cache if valid on page 1
        if (page == 1 && cachedLatestBooks.isNotEmpty() && (currentTime - lastFetchTimestamp < cacheDurationMs)) {
            //Log.d("ToshoScraper", "Returning cached latest books (count: ${cachedLatestBooks.size})")

            return@runCatching cachedLatestBooks.map { book ->
                LatestUpdateUiModel(
                    id = book.id,
                    title = book.title,
                    coverUrl = book.coverUrl,
                    viewerCount = "18.4K views",
                    rating = "4.9",
                    type = "Manga",
                    timeAgo = "1 hour ago",
                    url = book.url
                )
            }
        }

        val offset = (page - 1) * 40
        val url = "$baseUrl/api/home2/recentlyUpdated?offset=$offset&limit=40&types=Manga%2CManwha%2CManhua%2COEL%2COther&mediums=Comic%2CNovel&contentRatings=Safe%2CSuggestive%2CErotica"

        Log.d("ToshoScraper", "Fetching JSON API (page $page, offset $offset): $url")
        val request = Request.Builder()
            .url(url)
            .headers(getImageHeaders().toHeaders())
            .build()

        val response = client.newCall(request).execute()
        Log.d("ToshoScraper", "Response code: ${response.code}, message: ${response.message}")
        if (!response.isSuccessful) {
            Log.e("ToshoScraper", "API error: ${response.code}")
            if (cachedLatestBooks.isNotEmpty()) {
                return@runCatching cachedLatestBooks.map { book ->
                    LatestUpdateUiModel(
                        id = book.id,
                        title = book.title,
                        coverUrl = book.coverUrl,
                        viewerCount = "18.4K views",
                        rating = "4.9",
                        type = "Manga",
                        timeAgo = "1 hour ago",
                        url = book.url
                    )
                }
            }
            throw Exception("API error: ${response.code}")
        }

        val jsonString = response.body?.string() ?: throw Exception("Empty JSON response")
        Log.d("ToshoScraper", "JSON fetched successfully. Length: ${jsonString.length}")

        val root = JSONObject(jsonString)
        val itemsArray = root.getJSONArray("items")
        Log.d("ToshoScraper", "Parsed items array count: ${itemsArray.length()}")

        val list = mutableListOf<LatestUpdateUiModel>()
        for (i in 0 until itemsArray.length()) {
            val obj = itemsArray.getJSONObject(i)
            val id = obj.optString("id", "")
            val title = obj.optString("title", "Unknown")
            val imagePath = obj.optString("image", "").trim()

            val coverUrl = when {
                imagePath.isBlank() -> null
                imagePath.startsWith("http://") || imagePath.startsWith("https://") -> imagePath
                else -> "https://cdn.atsu.moe/static/${imagePath.trimStart('/')}"
            }

            Log.d("ToshoScraper", "Cover URL for '$title': $coverUrl")
            val views = obj.optString("views", "0") + " views"
            val rawRating = obj.optDouble("mbRating", 7.0)
            val rating = String.format(Locale.US, "%.1f", rawRating)
            val type = obj.optString("type", "Manga")
            val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            val timeAgo = formatTimeAgo(updatedAt)

            list.add(
                LatestUpdateUiModel(
                    id = id,
                    title = title,
                    coverUrl = coverUrl,
                    viewerCount = views,
                    rating = rating,
                    type = type,
                    timeAgo = timeAgo,
                    url = "/manga/$id"
                )
            )
        }

        Log.d("ToshoScraper", "Successfully mapped ${list.size} UI models for page $page.")
        if (page == 1 && list.isNotEmpty()) {
            return@runCatching list.map { book ->
                LatestUpdateUiModel(
                    id = book.id,
                    title = book.title,
                    coverUrl = book.coverUrl,
                    viewerCount = book.viewerCount,
                    rating = book.rating,
                    type = book.type,
                    timeAgo = book.timeAgo,
                    url = book.url
                )
            }
            lastFetchTimestamp = currentTime
        }

        list
    }

    suspend fun refreshLatestUpdatesUiModels(): Result<List<LatestUpdateUiModel>> = runCatching {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastManualRefreshTimestamp < refreshCooldownMs) {
            throw IllegalStateException("COOLDOWN")
        }

        val models = getLatestUpdatesUiModels(1).getOrThrow()
        if (models.isNotEmpty()) {
            lastManualRefreshTimestamp = currentTime
        }
        models
    }

    suspend fun getPopularUIModels(page: Int = 1): Result<List<PopularUIModel>> = runCatching {
        val offset = (page - 1) * 40
        val url = "$baseUrl/api/home2/popular?offset=$offset&limit=40&types=Manga%2CManwha%2CManhua%2COEL%2COther&mediums=Comic%2CNovel&contentRatings=Safe%2CSuggestive%2CErotica&timeframe=all"

        Log.d("ToshoScraper", "Fetching Popular API (page $page, offset $offset): $url")
        val request = Request.Builder()
            .url(url)
            .headers(getImageHeaders().toHeaders())
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful){
            if (cachedPopularBooks.isNotEmpty() && page == 1){
                return@runCatching cachedPopularBooks.map{ book ->
                    PopularUIModel(
                        id = book.id,
                        title = book.title,
                        coverUrl = book.coverUrl,
                        viewerCount = "18.4K views",
                        rating = "4.9",
                        type = "Manga",
                        url = book.url
                    )
                }
            }
            throw Exception("API error: ${response.code}")
        }

        val jsonString = response.body?.string() ?: throw Exception("Empty JSON response")
        val root = JSONObject(jsonString)
        val itemsArray = root.getJSONArray("items")

        val list = mutableListOf<PopularUIModel>()
        for (i in 0 until itemsArray.length()) {
            val obj = itemsArray.getJSONObject(i)
            val id = obj.optString("id", "")
            val title = obj.optString("title", "Unknown")
            val imagePath = obj.optString("image", "").trim()

            val coverUrl = when {
                imagePath.isBlank() -> null
                imagePath.startsWith("http://") || imagePath.startsWith("https://") -> imagePath
                else -> "https://cdn.atsu.moe/static/${imagePath.trimStart('/')}"
            }

            val views = obj.optString("views", "0") + " views"
            val rawRating = obj.optDouble("mbRating", 7.0)
            val rating = String.format(Locale.US, "%.1f", rawRating)
            val type = obj.optString("type", "Manga")

            list.add(
                PopularUIModel(
                    id = id,
                    title = title,
                    coverUrl = coverUrl,
                    viewerCount = views,
                    rating = rating,
                    type = type,
                    url = "/manga/$id"
                )
            )
        }

        if (list.isNotEmpty() && page == 1) {
            cachedPopularBooks = list.map { model ->
                Book(
                    id = model.id,
                    sourceId = id,
                    url = model.url,
                    title = model.title,
                    coverUrl = model.coverUrl,
                    status = BookStatus.UNKNOWN
                )
            }
        }

        list
    }
    private fun formatTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "$days days ago"
            hours > 0 -> "$hours hours ago"
            minutes > 0 -> "$minutes minutes ago"
            else -> "Just now"
        }
    }

    override suspend fun searchBooks(query: String, page: Int, filters: SourceFilterList): Result<List<Book>> {
        return getLatestUpdates(page).map { list ->
            if (query.isBlank()) list else list.filter { it.title.contains(query, ignoreCase = true) }
        }
    }

    override suspend fun getBookDetails(book: Book): Result<Book> = runCatching {
        val targetUrl = if (book.url.startsWith("http")) book.url else "$baseUrl${book.url}"
        val request = Request.Builder()
            .url(targetUrl)
            .headers(getImageHeaders().toHeaders())
            .build()

        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: ""
        val doc = Jsoup.parse(html)

        book.copy(
            description = doc.select(".synopsis, .description").text(),
            author = doc.select(".author").text(),
            genres = doc.select(".genres a, .tag").map { it.text() }
        )
    }

    override suspend fun getChapterList(book: Book): Result<List<Chapter>> = runCatching {
        val targetUrl = if (book.url.startsWith("http")) book.url else "$baseUrl${book.url}"
        val request = Request.Builder()
            .url(targetUrl)
            .headers(getImageHeaders().toHeaders())
            .build()

        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: ""
        val doc = Jsoup.parse(html)

        doc.select(".chapter-list li, .chapters a").mapIndexed { index, element ->
            val chapterUrl = element.select("a").attr("href").ifEmpty { element.attr("href") }
            val name = element.text()
            Chapter(
                id = chapterUrl,
                bookId = book.id,
                url = chapterUrl,
                name = name,
                chapterNumber = (index + 1).toFloat()
            )
        }
    }

    override suspend fun getPageList(chapter: Chapter): Result<List<Page>> = runCatching {
        val targetUrl = if (chapter.url.startsWith("http")) chapter.url else "$baseUrl${chapter.url}"
        val request = Request.Builder()
            .url(targetUrl)
            .headers(getImageHeaders().toHeaders())
            .build()

        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: ""
        val doc = Jsoup.parse(html)

        doc.select(".reader-image-container img, .pages img").mapIndexed { index, element ->
            val imgUrl = element.attr("src").ifEmpty { element.attr("data-src") }
            Page(
                index = index,
                url = imgUrl,
                imageUrl = imgUrl
            )
        }
    }

    @Composable
    override fun ContentScreen(onBookClick: (Book) -> Unit, modifier: Modifier) {
        var bookList by remember { mutableStateOf<List<Book>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                getLatestUpdates(page = 1).onSuccess { books ->
                    bookList = books
                }
                isLoading = false
            }
        }

        Box(modifier = modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(bookList) { book ->
                        BookCard(
                            book = book,
                            headers = getImageHeaders(),
                            onClick = { onBookClick(book) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookCard(
    book: Book,
    headers: Map<String, String>,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val imageRequest = remember(book.coverUrl) {
        ImageRequest.Builder(context)
            .data(book.coverUrl)
            .apply {
                headers.forEach { (key, value) ->
                    addHeader(key, value)
                }
            }
            .crossfade(true)
            .build()
    }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column {
            AsyncImage(
                model = imageRequest,
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Text(
                text = book.title,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
