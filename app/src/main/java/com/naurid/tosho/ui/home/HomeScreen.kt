package com.naurid.tosho.ui.home

import android.util.Log
import android.widget.Toast
import com.naurid.tosho.source.core.atsumaru.LatestUpdateUiModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naurid.tosho.R
import com.naurid.tosho.ui.theme.DarkBackground
import com.naurid.tosho.ui.theme.DarkGlassBackground
import com.naurid.tosho.ui.theme.DarkGlassBorder
import com.naurid.tosho.ui.theme.DarkTextPrimary
import com.naurid.tosho.ui.theme.DarkTextSecondary
import com.naurid.tosho.ui.theme.ExpressiveSecondary
import com.naurid.tosho.ui.theme.LightBackground
import com.naurid.tosho.ui.theme.LightGlassBackground
import com.naurid.tosho.ui.theme.LightGlassBorder
import com.naurid.tosho.ui.theme.LightTextPrimary
import com.naurid.tosho.ui.theme.LightTextSecondary

enum class HomeCategory(val title: String) {
    FEATURED("FEATURED"),
    CONTINUE_READING("CONTINUE READING"),
    TRENDING_NOW("TRENDING NOW"),
    LATEST_UPDATES("LATEST UPDATES"),
    POPULAR("POPULAR")
}

data class SampleManga(
    val id: String,
    val title: String,
    val author: String,
    val rating: String,
    val genres: List<String>,
    val synopsis: String,
    val currentChapter: String? = null,
    val totalChapters: String? = null,
    val progress: Float = 0f,
    val latestChapter: String? = null,
    val timeAgo: String? = null,
    val scanlator: String? = null,
    val format: String = "MANGA",
    val rank: Int = 0,
    val coverUrl: String? = null
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    onMangaClick: (SampleManga) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(HomeCategory.FEATURED) }
    var searchQuery by remember { mutableStateOf("") }
    var scrapedLatestItems by remember { mutableStateOf<List<LatestUpdateUiModel>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val plugin = remember { com.naurid.tosho.source.core.atsumaru.AtsumaruSourcePlugin() }

    // Fetch latest updates when HomeScreen first enters composition.
    LaunchedEffect(Unit) {
        Log.d("ToshoScraper", "Starting latest updates fetch...")

        val result = withContext(Dispatchers.IO) {
            plugin.getLatestUpdatesUiModels(1)
        }

        result.onSuccess { models ->
            Log.d("ToshoScraper", "Fetched ${models.size} items")
            models.take(5).forEach { model ->
                Log.d("ToshoScraper", "Fetched title: ${model.title}")
            }

            // LaunchedEffect resumes on Main, so Compose state is updated safely.
            scrapedLatestItems = models
            Log.d("ToshoScraper", "scrapedLatestItems assigned: ${scrapedLatestItems.size}")
        }.onFailure { error ->
            Log.e("ToshoScraper", "Latest updates fetch failed", error)
        }
    }

    // Pull-to-refresh. The coroutine starts on Main; only the network call runs on IO.
    val refreshLatest: () -> Unit = {
        scope.launch {
            isRefreshing = true
            Log.d("ToshoScraper", "Refreshing latest updates...")

            val result = withContext(Dispatchers.IO) {
                plugin.refreshLatestUpdatesUiModels()
            }

            result.onSuccess { models ->
                Log.d("ToshoScraper", "Refresh returned ${models.size} items")
                scrapedLatestItems = models
                Toast.makeText(context, "Refreshed latest updates", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Log.e("ToshoScraper", "Latest updates refresh failed", error)
                val message = if (error.message == "COOLDOWN") {
                    "Please wait a few minutes before refreshing again to respect server limits."
                } else {
                    "Refresh failed. Please check your connection."
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }

            isRefreshing = false
        }
    }

    // Search the scraped list. This is always non-null, even before the first fetch finishes.
    val latestUiItems = remember(scrapedLatestItems, searchQuery) {
        Log.d("ToshoScraper", "Building latestUiItems...")
        Log.d("ToshoScraper", "scrapedLatestItems size: ${scrapedLatestItems.size}")
        Log.d("ToshoScraper", "Search query: '$searchQuery'")

        val filtered = if (searchQuery.isBlank()) {
            scrapedLatestItems
        } else {
            scrapedLatestItems.filter { item ->
                item.title.contains(searchQuery, ignoreCase = true)
            }
        }

        Log.d("ToshoScraper", "latestUiItems size: ${filtered.size}")
        filtered
    }

    val textPrimary = if (isDarkTheme) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDarkTheme) DarkTextSecondary else LightTextSecondary
    val glassBg = if (isDarkTheme) DarkGlassBackground else LightGlassBackground
    val glassBorder = if (isDarkTheme) DarkGlassBorder else LightGlassBorder

    val allSampleData = remember { getMockMangaList() }

    // Filter items in the active category based on user search query
    val filteredItems = remember(selectedCategory, searchQuery) {
        val categoryList = when (selectedCategory) {
            HomeCategory.FEATURED -> allSampleData
            HomeCategory.CONTINUE_READING -> allSampleData.filter { it.progress > 0f }
            HomeCategory.TRENDING_NOW -> allSampleData.sortedBy { it.rank }
            HomeCategory.LATEST_UPDATES -> emptyList() // handled via latestUiItems
            HomeCategory.POPULAR -> allSampleData.sortedByDescending { it.rating }
        }

        if (searchQuery.isBlank()) {
            categoryList
        } else {
            categoryList.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.author.contains(searchQuery, ignoreCase = true) ||
                        it.genres.any { g -> g.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // 1. BRAND HEADER & SEARCH BAR
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TOSHO",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    letterSpacing = (-1.5).sp,
                    color = textPrimary
                )
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Frosted Glass Search Bar
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = RoundedCornerShape(21.dp),
                color = glassBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = textPrimary
                        ),
                        cursorBrush = SolidColor(textPrimary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search in ${selectedCategory.title.lowercase()}...",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 13.sp,
                                        color = textSecondary
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = textSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. CATEGORY TABS AT THE TOP
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HomeCategory.values().forEach { category ->
                val isSelected = selectedCategory == category
                Surface(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { selectedCategory = category },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) textPrimary else Color.Transparent,
                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, glassBorder)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.title,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = (-0.5).sp,
                                color = if (isSelected) (if (isDarkTheme) DarkBackground else LightBackground) else textSecondary
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Log.d(
            "ToshoScraper",
            "Selected category: $selectedCategory | " +
                    "filteredItems: ${filteredItems.size} | " +
                    "latestUiItems: ${latestUiItems.size}"
        )

        // Latest Updates has its own scraped list, not the mock-data filteredItems list.
        if (selectedCategory == HomeCategory.LATEST_UPDATES) {
            if (latestUiItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (scrapedLatestItems.isEmpty()) {
                            "LOADING LATEST UPDATES..."
                        } else {
                            "NO TITLES FOUND"
                        },
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textSecondary
                        )
                    )
                }
            } else {
                LatestUpdatesView(
                    items = latestUiItems,
                    isDarkTheme = isDarkTheme,
                    onItemClick = { uiModel ->
                        onMangaClick(
                            SampleManga(
                                id = uiModel.id,
                                title = uiModel.title,
                                author = "Atsumaru",
                                rating = uiModel.rating,
                                genres = listOf(uiModel.type),
                                synopsis = "",
                                coverUrl = uiModel.coverUrl
                            )
                        )
                    },
                    isRefreshing = isRefreshing,
                    onRefresh = refreshLatest
                )
            }
        } else if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NO TITLES FOUND IN ${selectedCategory.title}",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textSecondary
                    )
                )
            }
        } else {
            when (selectedCategory) {
                HomeCategory.FEATURED -> FeaturedView(
                    items = filteredItems,
                    isDarkTheme = isDarkTheme,
                    onMangaClick = onMangaClick
                )
                HomeCategory.CONTINUE_READING -> ContinueReadingView(
                    items = filteredItems,
                    isDarkTheme = isDarkTheme,
                    onMangaClick = onMangaClick
                )
                HomeCategory.TRENDING_NOW -> TrendingView(
                    items = filteredItems,
                    isDarkTheme = isDarkTheme,
                    onMangaClick = onMangaClick
                )
                HomeCategory.LATEST_UPDATES -> Unit // Rendered by the dedicated branch above.
                HomeCategory.POPULAR -> PopularView(
                    items = filteredItems,
                    isDarkTheme = isDarkTheme,
                    onMangaClick = onMangaClick
                )
            }
        }
    }
}

// =========================================================================
// SECTION 1: FEATURED (Cinematic Hero Spotlight Banner + Grid)
// =========================================================================
@Composable
private fun FeaturedView(
    items: List<SampleManga>,
    isDarkTheme: Boolean,
    onMangaClick: (SampleManga) -> Unit
) {
    val hero = items.firstOrNull() ?: return
    val textPrimary = if (isDarkTheme) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDarkTheme) DarkTextSecondary else LightTextSecondary
    val remainingItems = items.drop(1)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onMangaClick(hero) },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.cover_example),
                        contentDescription = hero.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradient Scrim for legibility
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.5f),
                                        Color.Black.copy(alpha = 0.95f)
                                    ),
                                    startY = 60f
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ExpressiveSecondary
                            ) {
                                Text(
                                    text = hero.format,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rating",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = hero.rating,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = hero.title.uppercase(),
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                letterSpacing = (-1).sp,
                                color = Color.White
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = hero.synopsis,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { onMangaClick(hero) },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpressiveSecondary),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "START READING",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }

        // More Featured Titles Header
        item {
            Text(
                text = "CURATED SPOTLIGHT",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = (-1).sp,
                    color = textPrimary
                )
            )
        }

        // Grid of remaining featured titles
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                remainingItems.forEach { manga ->
                    MangaListRowCard(
                        manga = manga,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onClick = { onMangaClick(manga) }
                    )
                }
            }
        }
    }
}

// =========================================================================
// SECTION 2: CONTINUE READING
// =========================================================================
@Composable
private fun ContinueReadingView(
    items: List<SampleManga>,
    isDarkTheme: Boolean,
    onMangaClick: (SampleManga) -> Unit
) {
    val textPrimary = if (isDarkTheme) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDarkTheme) DarkTextSecondary else LightTextSecondary
    val glassBg = if (isDarkTheme) DarkGlassBackground else LightGlassBackground
    val glassBorder = if (isDarkTheme) DarkGlassBorder else LightGlassBorder

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        items(items) { manga ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onMangaClick(manga) },
                shape = RoundedCornerShape(12.dp),
                color = glassBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.cover_example),
                        contentDescription = manga.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 65.dp, height = 90.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = manga.title.uppercase(),
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = (-0.5).sp,
                                color = textPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Currently on: ${manga.currentChapter ?: "Chapter 1"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = textSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { manga.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = ExpressiveSecondary,
                            trackColor = textSecondary.copy(alpha = 0.2f),
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${(manga.progress * 100).toInt()}% completed",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = { onMangaClick(manga) },
                        colors = ButtonDefaults.buttonColors(containerColor = textPrimary),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume",
                            tint = if (isDarkTheme) DarkBackground else LightBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// SECTION 3: TRENDING NOW (Manga Portrait Poster Cards Grid)
// =========================================================================
@Composable
private fun TrendingView(
    items: List<SampleManga>,
    isDarkTheme: Boolean,
    onMangaClick: (SampleManga) -> Unit
) {
    val textPrimary = if (isDarkTheme) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDarkTheme) DarkTextSecondary else LightTextSecondary

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        items(items) { manga ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMangaClick(manga) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.cover_example),
                        contentDescription = manga.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Rank pill in top-left
                    Surface(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopStart),
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = "#${manga.rank}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Rating badge in bottom-right
                    Surface(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.BottomEnd),
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.75f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = manga.rating,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = manga.title.uppercase(),
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = (-0.5).sp,
                        color = textPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = manga.genres.joinToString(" • "),
                    fontSize = 11.sp,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =========================================================================
// SECTION 4: LATEST UPDATES (Real-time chapter releases feed)
// =========================================================================
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun LatestUpdatesView(
    items: List<LatestUpdateUiModel>,
    isDarkTheme: Boolean,
    onItemClick: (LatestUpdateUiModel) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val textPrimary = if (isDarkTheme) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDarkTheme) DarkTextSecondary else LightTextSecondary
    val glassBg = if (isDarkTheme) DarkGlassBackground else LightGlassBackground
    val glassBorder = if (isDarkTheme) DarkGlassBorder else LightGlassBorder

    Log.d(
        "ToshoScraper",
        "LatestUpdatesView called with ${items.size} items"
    )

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(items) { item ->
                Log.d("ToshoScraper", "Rendering manga: ${item.title}")
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onItemClick(item) },
                    shape = RoundedCornerShape(10.dp),
                    color = glassBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Cover Image
                        if (!item.coverUrl.isNullOrEmpty()) {
                            val imageRequest = remember(item.coverUrl) {
                                ImageRequest.Builder(context)
                                    .data(item.coverUrl)
                                    .crossfade(true)
                                    .build()
                            }
                            AsyncImage(
                                model = imageRequest,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 55.dp, height = 75.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.cover_example),
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 55.dp, height = 75.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            // 2. Name (Title)
                            Text(
                                text = item.title.uppercase(),
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    letterSpacing = (-0.5).sp,
                                    color = textPrimary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            // 3. Rating & Type
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = item.rating,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = textPrimary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = item.type,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            // 4. Viewer number & 5. How long the update was (Time Ago)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.viewerCount,
                                    fontSize = 11.sp,
                                    color = textSecondary
                                )

                                Text(
                                    text = item.timeAgo,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpressiveSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// SECTION 5: POPULAR / TOP RATED (Numbered leaderboard)
// =========================================================================
@Composable
private fun PopularView(
    items: List<SampleManga>,
    isDarkTheme: Boolean,
    onMangaClick: (SampleManga) -> Unit
) {
    val textPrimary = if (isDarkTheme) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDarkTheme) DarkTextSecondary else LightTextSecondary
    val glassBg = if (isDarkTheme) DarkGlassBackground else LightGlassBackground
    val glassBorder = if (isDarkTheme) DarkGlassBorder else LightGlassBorder

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        items(items) { manga ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onMangaClick(manga) },
                shape = RoundedCornerShape(10.dp),
                color = glassBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Large Bold Rank Number
                    Text(
                        text = String.format("%02d", manga.rank),
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            letterSpacing = (-1).sp,
                            color = textSecondary.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.width(42.dp)
                    )

                    Image(
                        painter = painterResource(id = R.drawable.cover_example),
                        contentDescription = manga.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 50.dp, height = 70.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = manga.title.uppercase(),
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = (-0.5).sp,
                                color = textPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${manga.rating} • ${manga.genres.firstOrNull() ?: "Manga"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "by ${manga.author}",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaListRowCard(
    manga: SampleManga,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.cover_example),
            contentDescription = manga.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 48.dp, height = 64.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = manga.title.uppercase(),
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = (-0.5).sp,
                    color = textPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = manga.genres.joinToString(" • "),
                fontSize = 11.sp,
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = manga.rating,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
        }
    }
}

private fun getMockMangaList(): List<SampleManga> {
    return listOf(
        SampleManga(
            id = "1",
            title = "Frieren: Beyond Journey's End",
            author = "Kanehito Yamada",
            rating = "4.94",
            genres = listOf("Fantasy", "Adventure", "Drama"),
            synopsis = "The adventure is over, but life goes on for an elf mage just beginning to learn what living is all about.",
            currentChapter = "Chapter 42",
            totalChapters = "135",
            progress = 0.31f,
            latestChapter = "Chapter 135",
            timeAgo = "1 hour ago",
            scanlator = "Kirei Cake",
            format = "MANGA",
            rank = 1
        ),
        SampleManga(
            id = "2",
            title = "Chainsaw Man",
            author = "Tatsuki Fujimoto",
            rating = "4.89",
            genres = listOf("Action", "Supernatural", "Gore"),
            synopsis = "Denji was a small-time devil hunter just trying to survive in a harsh world after being betrayed.",
            currentChapter = "Chapter 168",
            totalChapters = "172",
            progress = 0.97f,
            latestChapter = "Chapter 172",
            timeAgo = "3 hours ago",
            scanlator = "Shueisha",
            format = "MANGA",
            rank = 2
        ),
        SampleManga(
            id = "3",
            title = "Berserk",
            author = "Kentaro Miura",
            rating = "4.98",
            genres = listOf("Dark Fantasy", "Action", "Tragedy"),
            synopsis = "Guts, a former mercenary now known as the 'Black Swordsman', is out for vengeance against his former comrade.",
            currentChapter = "Chapter 120",
            totalChapters = "376",
            progress = 0.32f,
            latestChapter = "Chapter 376",
            timeAgo = "2 days ago",
            scanlator = "Studio Gaga",
            format = "MANGA",
            rank = 3
        ),
        SampleManga(
            id = "4",
            title = "Solo Leveling",
            author = "Chugong",
            rating = "4.86",
            genres = listOf("Action", "Fantasy", "Webtoon"),
            synopsis = "In a world where hunters must battle deadly monsters, Sung Jinwoo, the weakest hunter, discovers a mysterious quest.",
            currentChapter = "Chapter 179",
            totalChapters = "200",
            progress = 0.89f,
            latestChapter = "Side Story 21",
            timeAgo = "5 hours ago",
            scanlator = "Asura Scans",
            format = "MANHWA",
            rank = 4
        ),
        SampleManga(
            id = "5",
            title = "Jujutsu Kaisen",
            author = "Gege Akutami",
            rating = "4.82",
            genres = listOf("Action", "Occult", "Shounen"),
            synopsis = "Yuji Itadori swallows a cursed talisman and becomes possessed by the King of Curses, Ryomen Sukuna.",
            currentChapter = "Chapter 240",
            totalChapters = "271",
            progress = 0.88f,
            latestChapter = "Chapter 271",
            timeAgo = "4 days ago",
            scanlator = "Viz Media",
            format = "MANGA",
            rank = 5
        ),
        SampleManga(
            id = "6",
            title = "Dungeon Meshi",
            author = "Ryoko Kui",
            rating = "4.91",
            genres = listOf("Comedy", "Cooking", "Fantasy"),
            synopsis = "When young adventurer Laios and his party are attacked and soundly thrashed by a dragon deep in a dungeon, they must eat monsters to survive.",
            currentChapter = "Chapter 80",
            totalChapters = "97",
            progress = 0.82f,
            latestChapter = "Chapter 97",
            timeAgo = "1 week ago",
            scanlator = "Delicious Scans",
            format = "MANGA",
            rank = 6
        ),
        SampleManga(
            id = "7",
            title = "Vinland Saga",
            author = "Makoto Yukimura",
            rating = "4.95",
            genres = listOf("Historical", "Action", "Drama"),
            synopsis = "Thorfinn pursues a journey with his father's killer in order to take revenge and end his life in a duel.",
            currentChapter = "Chapter 50",
            totalChapters = "214",
            progress = 0.23f,
            latestChapter = "Chapter 214",
            timeAgo = "3 days ago",
            scanlator = "Project Vinland",
            format = "MANGA",
            rank = 7
        ),
        SampleManga(
            id = "8",
            title = "Oshi no Ko",
            author = "Aka Akasaka",
            rating = "4.84",
            genres = listOf("Drama", "Mystery", "Reincarnation"),
            synopsis = "A doctor and his recently deceased patient find themselves reborn as the twin children of their favorite idol.",
            currentChapter = "Chapter 90",
            totalChapters = "166",
            progress = 0.54f,
            latestChapter = "Chapter 166",
            timeAgo = "6 hours ago",
            scanlator = "MangaPlus",
            format = "MANGA",
            rank = 8
        )
    )
}
