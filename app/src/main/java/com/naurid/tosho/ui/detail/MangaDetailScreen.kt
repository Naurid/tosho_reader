package com.naurid.tosho.ui.detail

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.naurid.tosho.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale

data class ChapterUiModel(
    val id: String,
    val name: String,
    val chapterNumber: Float,
    val dateUpload: String,
    val scanlationMangaId: String? = null
)

data class ScanlatorUiModel(
    val id: String,
    val name: String
)

data class RelationUiModel(
    val id: String,
    val title: String,
    val coverUrl: String?,
    val type: String
)

data class RecommendationUiModel(
    val id: String,
    val title: String,
    val coverUrl: String?,
    val rating: String,
    val views: String
)

data class ReviewUiModel(
    val id: String,
    val title: String,
    val content: String,
    val rating: String,
    val authorName: String
)

sealed class ReviewContentItem {
    data class Text(val text: String) : ReviewContentItem()
    data class Image(val url: String, val alt: String) : ReviewContentItem()
}

fun parseReviewContent(content: String): List<ReviewContentItem> {
    val items = mutableListOf<ReviewContentItem>()
    val regex = Regex("!\\[(.*?)]\\((.*?)\\)")
    var lastIndex = 0

    for (match in regex.findAll(content)) {
        val range = match.range
        if (range.first > lastIndex) {
            val plainText = content.substring(lastIndex, range.first).trim()
            if (plainText.isNotEmpty()) {
                items.add(ReviewContentItem.Text(plainText))
            }
        }
        val alt = match.groups[1]?.value ?: ""
        val url = match.groups[2]?.value ?: ""
        if (url.isNotEmpty()) {
            items.add(ReviewContentItem.Image(url, alt))
        }
        lastIndex = range.last + 1
    }

    if (lastIndex < content.length) {
        val remaining = content.substring(lastIndex).trim()
        if (remaining.isNotEmpty()) {
            items.add(ReviewContentItem.Text(remaining))
        }
    }

    if (items.isEmpty() && content.isNotEmpty()) {
        items.add(ReviewContentItem.Text(content))
    }

    return items
}

data class MangaDetailUiModel(
    val id: String,
    val title: String,
    val coverUrl: String?,
    val backdropUrl: String?,
    val description: String,
    val author: String,
    val rating: String,
    val views: String,
    val type: String,
    val status: String,
    val genres: List<String>,       // Horizontally scrollable subtitles band
    val themes: List<String>,       // Expandable scaling badges
    val scanlators: List<ScanlatorUiModel>,
    val chapters: List<ChapterUiModel>,
    val relations: List<RelationUiModel>,
    val recommendations: List<RecommendationUiModel>,
    val reviews: List<ReviewUiModel>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaDetailScreen(
    mangaId: String,
    onBack: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    // Android system back navigation
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var details by remember { mutableStateOf<MangaDetailUiModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedScanlatorIndex by remember { mutableStateOf(0) }
    var themesExpanded by remember { mutableStateOf(false) }
    var showRelations by remember { mutableStateOf(false) }
    var showRecommendations by remember { mutableStateOf(false) }
    var showReviews by remember { mutableStateOf(false) }

    val textPrimary = if (isDarkTheme) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDarkTheme) DarkTextSecondary else LightTextSecondary
    val bgPrimary = if (isDarkTheme) DarkBackground else LightBackground
    val glassBg = if (isDarkTheme) DarkGlassBackground else LightGlassBackground
    val glassBorder = if (isDarkTheme) DarkGlassBorder else LightGlassBorder

    LaunchedEffect(mangaId) {
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                // 1. Fetch Manga Page Details
                val pageUrl = "https://atsu.moe/api/manga/page?id=$mangaId"
                Log.d("ToshoScraper", "Fetching manga detail URL: $pageUrl")
                val pageRequest = Request.Builder()
                    .url(pageUrl)
                    .header("User-Agent", "ToshoApp/1.0 (+https://atsu.moe; mobile manga reader client)")
                    .header("Referer", "https://atsu.moe/")
                    .build()

                val pageResponse = client.newCall(pageRequest).execute()
                if (pageResponse.isSuccessful) {
                    val json = pageResponse.body?.string() ?: "{}"
                    val root = JSONObject(json)
                    val mangaObj = root.optJSONObject("mangaPage") ?: root.optJSONObject("manga") ?: root.optJSONObject("page") ?: root

                    val title = mangaObj.optString("title", "Unknown Title")
                    val desc = mangaObj.optString("synopsis", mangaObj.optString("description", "No synopsis available."))

                    val authorsArr = mangaObj.optJSONArray("authors")
                    val author = if (authorsArr != null && authorsArr.length() > 0) {
                        authorsArr.getJSONObject(0).optString("name", "Atsumaru Author")
                    } else {
                        "Atsumaru Author"
                    }

                    val ratingVal = mangaObj.optDouble("avgRating", mangaObj.optDouble("mbRating", 7.5))
                    val rating = String.format(Locale.US, "%.2f", ratingVal)
                    val views = mangaObj.optString("views", "120K") + " views"
                    val type = mangaObj.optString("type", "Manga")
                    val status = mangaObj.optString("status", "Ongoing")

                    val posterObj = mangaObj.optJSONObject("poster")
                    val imagePath = posterObj?.optString("image", posterObj.optString("largeImage", mangaObj.optString("image", ""))) ?: mangaObj.optString("image", "")
                    val coverUrl = when {
                        imagePath.isBlank() -> null
                        imagePath.startsWith("http") -> imagePath
                        else -> "https://cdn.atsu.moe/static/${imagePath.trimStart('/')}"
                    }

                    val bannerObj = mangaObj.optJSONObject("banner")
                    val backdropPath = bannerObj?.optString("url", imagePath) ?: imagePath
                    val backdropUrl = when {
                        backdropPath.isBlank() -> coverUrl
                        backdropPath.startsWith("http") -> backdropPath
                        else -> "https://cdn.atsu.moe/static/${backdropPath.trimStart('/')}"
                    }

                    // Genres (plain subtitles)
                    val genresArr = mangaObj.optJSONArray("genres")
                    val genres = mutableListOf<String>()
                    if (genresArr != null) {
                        for (i in 0 until genresArr.length()) {
                            val gObj = genresArr.optJSONObject(i)
                            val gName = gObj?.optString("name") ?: genresArr.optString(i)
                            if (gName.isNotBlank()) genres.add(gName)
                        }
                    }
                    if (genres.isEmpty()) genres.add(type)

                    // Themes / Tags (horizontal scaling badges)
                    val tagsArr = mangaObj.optJSONArray("tags")
                    val themes = mutableListOf<String>()
                    if (tagsArr != null) {
                        for (i in 0 until tagsArr.length()) {
                            val tObj = tagsArr.optJSONObject(i)
                            val tName = tObj?.optString("name") ?: tagsArr.optString(i)
                            if (tName.isNotBlank()) themes.add(tName)
                        }
                    }

                    // Scanlators
                    val scanlatorsArr = mangaObj.optJSONArray("scanlators")
                    val scanlators = mutableListOf<ScanlatorUiModel>()
                    if (scanlatorsArr != null) {
                        for (i in 0 until scanlatorsArr.length()) {
                            val sObj = scanlatorsArr.getJSONObject(i)
                            scanlators.add(
                                ScanlatorUiModel(
                                    id = sObj.optString("id", "$i"),
                                    name = sObj.optString("name", "Scanlator ${i + 1}")
                                )
                            )
                        }
                    }
                    if (scanlators.isEmpty()) {
                        scanlators.add(ScanlatorUiModel("default", "Official"))
                    }

                    // 2. Fetch All Chapters from allChapters endpoint
                    val chaptersUrl = "https://atsu.moe/api/manga/allChapters?mangaId=$mangaId"
                    Log.d("ToshoScraper", "Fetching all chapters URL: $chaptersUrl")
                    val chaptersRequest = Request.Builder()
                        .url(chaptersUrl)
                        .header("User-Agent", "ToshoApp/1.0 (+https://atsu.moe; mobile manga reader client)")
                        .header("Referer", "https://atsu.moe/")
                        .build()

                    val chaptersResponse = client.newCall(chaptersRequest).execute()
                    val chapters = mutableListOf<ChapterUiModel>()
                    if (chaptersResponse.isSuccessful) {
                        val chJson = chaptersResponse.body?.string() ?: "{}"
                        val chRoot = JSONObject(chJson)
                        val chaptersArr = chRoot.optJSONArray("chapters") ?: mangaObj.optJSONArray("chapters")
                        if (chaptersArr != null) {
                            for (i in 0 until chaptersArr.length()) {
                                val chObj = chaptersArr.getJSONObject(i)
                                val createdAt = chObj.optLong("createdAt", System.currentTimeMillis())
                                chapters.add(
                                    ChapterUiModel(
                                        id = chObj.optString("id", "$i"),
                                        name = chObj.optString("title", "Chapter ${i + 1}"),
                                        chapterNumber = chObj.optDouble("number", (i + 1).toDouble()).toFloat(),
                                        dateUpload = formatChapterTime(createdAt),
                                        scanlationMangaId = chObj.optString("scanlationMangaId", null)
                                    )
                                )
                            }
                        }
                    }
                    if (chapters.isEmpty()) {
                        chapters.add(ChapterUiModel("1", "Chapter 1: The Beginning", 1f, "Just now", scanlators.first().id))
                    }

                    // Relations
                    val relationsArr = mangaObj.optJSONArray("relations")
                    val relations = mutableListOf<RelationUiModel>()
                    if (relationsArr != null) {
                        for (i in 0 until relationsArr.length()) {
                            val rObj = relationsArr.getJSONObject(i)
                            val relType = rObj.optString("type", "Related")
                            val mangaSub = rObj.optJSONObject("manga")
                            if (mangaSub != null) {
                                val rId = mangaSub.optString("id", "")
                                val rTitle = mangaSub.optString("title", "")
                                val rImg = mangaSub.optString("image", "")
                                val rCover = if (rImg.startsWith("http")) rImg else "https://cdn.atsu.moe/static/${rImg.trimStart('/')}"
                                relations.add(RelationUiModel(rId, rTitle, rCover, relType))
                            }
                        }
                    }

                    // Recommendations
                    val recsArr = mangaObj.optJSONArray("recommendations")
                    val recommendations = mutableListOf<RecommendationUiModel>()
                    if (recsArr != null) {
                        for (i in 0 until recsArr.length()) {
                            val recObj = recsArr.getJSONObject(i)
                            val recId = recObj.optString("id", "")
                            val recTitle = recObj.optString("title", "")
                            val recImg = recObj.optString("image", "")
                            val recCover = if (recImg.startsWith("http")) recImg else "https://cdn.atsu.moe/static/${recImg.trimStart('/')}"
                            val recViews = recObj.optString("views", "0") + " views"
                            val recRating = String.format(Locale.US, "%.1f", recObj.optDouble("mbRating", 7.0))
                            recommendations.add(RecommendationUiModel(recId, recTitle, recCover, recRating, recViews))
                        }
                    }

                    // Top Reviews
                    val reviewsArr = mangaObj.optJSONArray("topReviews")
                    val reviews = mutableListOf<ReviewUiModel>()
                    if (reviewsArr != null) {
                        for (i in 0 until reviewsArr.length()) {
                            val revObj = reviewsArr.getJSONObject(i)
                            val revId = revObj.optString("id", "")
                            val revTitle = revObj.optString("title", "")
                            val revContent = revObj.optString("content", "")
                            val revRating = revObj.optDouble("rating", 9.0).toString()
                            val authorObj = revObj.optJSONObject("author")
                            val authorName = authorObj?.optString("usernameDisplay", "Reader") ?: "Reader"
                            reviews.add(ReviewUiModel(revId, revTitle, revContent, revRating, authorName))
                        }
                    }

                    details = MangaDetailUiModel(
                        id = mangaId,
                        title = title,
                        coverUrl = coverUrl,
                        backdropUrl = backdropUrl,
                        description = desc,
                        author = author,
                        rating = rating,
                        views = views,
                        type = type,
                        status = status,
                        genres = genres,
                        themes = themes,
                        scanlators = scanlators,
                        chapters = chapters,
                        relations = relations,
                        recommendations = recommendations,
                        reviews = reviews
                    )
                    Log.d("ToshoScraper", "Successfully parsed manga detail for: $title with ${chapters.size} chapters and ${reviews.size} reviews")
                } else {
                    Log.e("ToshoScraper", "Detail API error code: ${pageResponse.code}")
                }
            } catch (e: Exception) {
                Log.e("ToshoScraper", "Failed to load manga details", e)
            }

            // Fallback mock details if API failed
            if (details == null) {
                details = MangaDetailUiModel(
                    id = mangaId,
                    title = "Manga $mangaId",
                    coverUrl = "https://picsum.photos/300/450",
                    backdropUrl = "https://picsum.photos/600/400",
                    description = "Detailed synopsis for this manga title.",
                    author = "Atsumaru Studio",
                    rating = "4.9",
                    views = "240K views",
                    type = "Manga",
                    status = "Ongoing",
                    genres = listOf("Action", "Fantasy", "Adventure"),
                    themes = listOf("Magic", "School", "Supernatural", "Revenge", "Mystery", "Comedy", "Drama", "Slice of Life", "Tragedy", "Sci-Fi", "Romance"),
                    scanlators = listOf(ScanlatorUiModel("1", "Official"), ScanlatorUiModel("2", "Community")),
                    chapters = listOf(
                        ChapterUiModel("1", "Chapter 3", 3f, "1 hour ago", "1"),
                        ChapterUiModel("2", "Chapter 2", 2f, "3 hours ago", "1"),
                        ChapterUiModel("3", "Chapter 1", 1f, "1 day ago", "2")
                    ),
                    relations = emptyList(),
                    recommendations = emptyList(),
                    reviews = emptyList()
                )
            }
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgPrimary)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = textPrimary
            )
        } else if (details == null) {
            Text(
                text = "Failed to load manga details.",
                color = textSecondary,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            val manga = details!!
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    // 1. Hero Backdrop & Header (Scrolls away naturally)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp)
                        ) {
                            if (!manga.backdropUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(manga.backdropUrl).crossfade(true).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.DarkGray)
                                )
                            }

                            // Gradient Scrim overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, bgPrimary.copy(alpha = 0.8f), bgPrimary),
                                            startY = 100f
                                        )
                                    )
                            )

                            // Back Button (Closer to top)
                            Surface(
                                modifier = Modifier
                                    .padding(top = 8.dp, start = 12.dp)
                                    .size(40.dp)
                                    .align(Alignment.TopStart),
                                shape = CircleShape,
                                color = glassBg,
                                border = BorderStroke(1.dp, glassBorder)
                            ) {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = textPrimary
                                    )
                                }
                            }

                            // Bottom Metadata Header Overlay
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomStart)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .size(width = 110.dp, height = 155.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    shadowElevation = 8.dp,
                                    border = BorderStroke(1.dp, glassBorder)
                                ) {
                                    if (!manga.coverUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data(manga.coverUrl).crossfade(true).build(),
                                            contentDescription = manga.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = manga.title,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 22.sp,
                                            letterSpacing = (-0.5).sp,
                                            color = textPrimary
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = manga.author,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textSecondary
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.Black.copy(alpha = 0.6f)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFFD700),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(text = manga.rating, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = textPrimary.copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                text = manga.type,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Genres as Horizontally Scrollable Subtitle Band
                    item {
                        val genreScrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(genreScrollState)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            manga.genres.forEachIndexed { index, genre ->
                                Text(
                                    text = genre,
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = textSecondary
                                    )
                                )
                                if (index < manga.genres.size - 1) {
                                    Text(
                                        text = " • ",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = textSecondary.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 3. Themes as Expandable Badges (First 2 lines / 10 items + Expand Button)
                    if (manga.themes.isNotEmpty()) {
                        item {
                            val displayedThemes = if (themesExpanded || manga.themes.size <= 10) manga.themes else manga.themes.take(10)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    displayedThemes.forEach { theme ->
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = glassBg,
                                            border = BorderStroke(1.dp, glassBorder)
                                        ) {
                                            Text(
                                                text = theme,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textPrimary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                                if (manga.themes.size > 10) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    TextButton(
                                        onClick = { themesExpanded = !themesExpanded },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = if (themesExpanded) "Show less" else "+${manga.themes.size - 10} more themes",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Synopsis
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "SYNOPSIS",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.sp,
                                    color = textSecondary
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = manga.description,
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = textPrimary
                                )
                            )
                        }
                    }

                    // 5. Relations Section (Expandable by section title)
                    if (manga.relations.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                // Clickable section title
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showRelations = !showRelations
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "RELATIONS",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            letterSpacing = 1.sp,
                                            color = textSecondary
                                        )
                                    )

                                    Icon(
                                        imageVector = if (showRelations)
                                            Icons.Default.KeyboardArrowUp
                                        else
                                            Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = textSecondary
                                    )
                                }

                                // Entire section content
                                if (showRelations) {
                                    Spacer(modifier = Modifier.height(8.dp))

                                    ScrollableTabRow(
                                        selectedTabIndex = 0,
                                        edgePadding = 16.dp,
                                        containerColor = Color.Transparent,
                                        divider = {},
                                        indicator = {}
                                    ) {
                                        manga.relations.forEach { rel ->
                                            Surface(
                                                modifier = Modifier
                                                    .padding(end = 12.dp)
                                                    .width(130.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                color = glassBg,
                                                border = BorderStroke(1.dp, glassBorder)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(10.dp)
                                                ) {
                                                    if (!rel.coverUrl.isNullOrEmpty()) {
                                                        AsyncImage(
                                                            model = ImageRequest.Builder(context)
                                                                .data(rel.coverUrl)
                                                                .crossfade(true)
                                                                .build(),
                                                            contentDescription = rel.title,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(140.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                        )

                                                        Spacer(modifier = Modifier.height(6.dp))
                                                    }

                                                    Text(
                                                        text = rel.title,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = textPrimary,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    Spacer(modifier = Modifier.height(2.dp))

                                                    Text(
                                                        text = rel.type,
                                                        fontSize = 10.sp,
                                                        color = textSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. Recommendations Section (Expandable by section title)
                    if (manga.recommendations.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                // Clickable section title
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showRecommendations = !showRecommendations
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "RECOMMENDATIONS",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            letterSpacing = 1.sp,
                                            color = textSecondary
                                        )
                                    )

                                    Icon(
                                        imageVector = if (showRecommendations)
                                            Icons.Default.KeyboardArrowUp
                                        else
                                            Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = textSecondary
                                    )
                                }

                                // Entire section content
                                if (showRecommendations) {
                                    Spacer(modifier = Modifier.height(8.dp))

                                    ScrollableTabRow(
                                        selectedTabIndex = 0,
                                        edgePadding = 16.dp,
                                        containerColor = Color.Transparent,
                                        divider = {},
                                        indicator = {}
                                    ) {
                                        manga.recommendations.forEach { rec ->
                                            Surface(
                                                modifier = Modifier
                                                    .padding(end = 12.dp)
                                                    .width(130.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                color = glassBg,
                                                border = BorderStroke(1.dp, glassBorder)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(10.dp)
                                                ) {
                                                    if (!rec.coverUrl.isNullOrEmpty()) {
                                                        AsyncImage(
                                                            model = ImageRequest.Builder(context)
                                                                .data(rec.coverUrl)
                                                                .crossfade(true)
                                                                .build(),
                                                            contentDescription = rec.title,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(140.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                        )

                                                        Spacer(modifier = Modifier.height(6.dp))
                                                    }

                                                    Text(
                                                        text = rec.title,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = textPrimary,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    Spacer(modifier = Modifier.height(2.dp))

                                                    Text(
                                                        text = "${rec.views} • ★ ${rec.rating}",
                                                        fontSize = 10.sp,
                                                        color = textSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 7. Top Reviews Section (Expandable by section title)
                    if (manga.reviews.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                // Clickable section title
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showReviews = !showReviews
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "TOP REVIEWS",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            letterSpacing = 1.sp,
                                            color = textSecondary
                                        )
                                    )

                                    Icon(
                                        imageVector = if (showReviews)
                                            Icons.Default.KeyboardArrowUp
                                        else
                                            Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = textSecondary
                                    )
                                }

                                // Entire section content
                                if (showReviews) {
                                    Spacer(modifier = Modifier.height(8.dp))

                                    manga.reviews.take(5).forEach { rev ->
                                        val parsedContent = remember(rev.content) {
                                            parseReviewContent(rev.content)
                                        }

                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            color = glassBg,
                                            border = BorderStroke(1.dp, glassBorder)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = rev.authorName,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = textPrimary
                                                    )

                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color.Black.copy(alpha = 0.6f)
                                                    ) {
                                                        Text(
                                                            text = "★ ${rev.rating}",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(
                                                                horizontal = 6.dp,
                                                                vertical = 2.dp
                                                            )
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    text = rev.title,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textPrimary
                                                )

                                                Spacer(modifier = Modifier.height(4.dp))

                                                // Review content is always visible when section is open
                                                parsedContent.forEach { item ->
                                                    when (item) {
                                                        is ReviewContentItem.Text -> {
                                                            Text(
                                                                text = item.text,
                                                                fontSize = 12.sp,
                                                                color = textSecondary
                                                            )

                                                            Spacer(modifier = Modifier.height(4.dp))
                                                        }

                                                        is ReviewContentItem.Image -> {
                                                            AsyncImage(
                                                                model = ImageRequest.Builder(context)
                                                                    .data(item.url)
                                                                    .crossfade(true)
                                                                    .build(),
                                                                contentDescription = item.alt,
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(180.dp)
                                                                    .clip(RoundedCornerShape(8.dp))
                                                            )

                                                            Spacer(modifier = Modifier.height(4.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 8. Scanlator Tabs Header & Chapters List
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "CHAPTERS BY SCANLATOR",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.sp,
                                    color = textSecondary
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Scanlator Tabs
                            if (manga.scanlators.isNotEmpty()) {
                                ScrollableTabRow(
                                    selectedTabIndex = selectedScanlatorIndex,
                                    edgePadding = 16.dp,
                                    containerColor = Color.Transparent,
                                    divider = {},
                                    indicator = {}
                                ) {
                                    manga.scanlators.forEachIndexed { index, scanlator ->
                                        val isSelected = selectedScanlatorIndex == index
                                        Tab(
                                            selected = isSelected,
                                            onClick = { selectedScanlatorIndex = index },
                                            text = {
                                                Text(
                                                    text = scanlator.name,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) textPrimary else textSecondary
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Filter chapters by selected scanlator
                    val activeScanlatorId = manga.scanlators.getOrNull(selectedScanlatorIndex)?.id
                    val filteredChapters = manga.chapters.filter { ch ->
                        activeScanlatorId == null || ch.scanlationMangaId == null || ch.scanlationMangaId == activeScanlatorId
                    }.ifEmpty { manga.chapters }

                    items(filteredChapters) { chapter ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { },
                            shape = RoundedCornerShape(10.dp),
                            color = glassBg,
                            border = BorderStroke(1.dp, glassBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = chapter.name,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = textPrimary
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = chapter.dateUpload,
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Floating Action Button for "Start Reading" on bottom right
                ExtendedFloatingActionButton(
                    onClick = { /* Start reading */ },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                        .navigationBarsPadding(),
                    containerColor = textPrimary,
                    contentColor = bgPrimary,
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Reading"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Reading",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatChapterTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val days = diff / (1000 * 60 * 60 * 24)
    val hours = diff / (1000 * 60 * 60)
    return when {
        days > 0 -> "$days days ago"
        hours > 0 -> "$hours hours ago"
        else -> "Recently"
    }
}
