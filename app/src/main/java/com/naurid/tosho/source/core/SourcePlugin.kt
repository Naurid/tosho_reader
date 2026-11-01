package com.naurid.tosho.source.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.naurid.tosho.domain.model.Book
import com.naurid.tosho.domain.model.Chapter
import com.naurid.tosho.domain.model.Page
import com.naurid.tosho.domain.model.SourceFilterList

/**
 * Core interface defining a modular data and UI provider for Tosho.
 *
 * Every source plugin (whether a local SAF/EPUB provider or a remote web scraper)
 * implements this contract. It satisfies two architectural requirements:
 * 1. Data fetching: Suspending functions for querying books, chapters, and pages.
 * 2. Strict UI decoupling: An injected @Composable [ContentScreen] allowing the source
 *    to own and render its bespoke browsing/filtering experience while delegating selection
 *    back to the host app.
 */
interface SourcePlugin {

    // region Metadata & Capabilities

    /** Unique identifier for the source (e.g., "local_storage", "mangadex_en"). */
    val id: String

    /** Human-readable display name (e.g., "Local Storage", "MangaDex"). */
    val name: String

    /** Semantic version of the plugin (e.g., "1.0.0"). */
    val version: String

    /** Primary language code of content provided (e.g., "en", "all", "local"). */
    val lang: String

    /** Optional URL or resource path for the source's brand icon. */
    val iconUrl: String? get() = null

    /** Indicates whether the source supports querying latest releases/updates. */
    val supportsLatest: Boolean get() = true

    // endregion

    // region Data Fetching Contracts

    /**
     * Fetches a paginated list of popular/trending books.
     *
     * @param page 1-based page index.
     * @return [Result] containing the list of books, or failure exception.
     */
    suspend fun getPopularBooks(page: Int): Result<List<Book>>

    /**
     * Fetches a paginated list of recently updated books.
     *
     * @param page 1-based page index.
     * @return [Result] containing the list of books, or failure exception.
     */
    suspend fun getLatestUpdates(page: Int): Result<List<Book>>

    /**
     * Searches for books matching [query] and [filters].
     *
     * @param query Search keywords entered by the user.
     * @param page 1-based page index.
     * @param filters Selected filter values provided by [getFilterList].
     * @return [Result] containing the list of matching books, or failure exception.
     */
    suspend fun searchBooks(
        query: String,
        page: Int,
        filters: SourceFilterList = emptyList(),
    ): Result<List<Book>>

    /**
     * Fetches complete details (description, genres, status, cover) for a given [book].
     *
     * @param book Skeleton book previously returned from a catalog list or search.
     * @return [Result] containing the fully enriched [Book].
     */
    suspend fun getBookDetails(book: Book): Result<Book>

    /**
     * Retrieves the ordered list of chapters available for the given [book].
     *
     * @param book Target book to fetch chapters for.
     * @return [Result] containing the list of chapters.
     */
    suspend fun getChapterList(book: Book): Result<List<Chapter>>

    /**
     * Resolves the list of pages (image URLs or text chunks) for a [chapter].
     *
     * @param chapter Target chapter to read.
     * @return [Result] containing the ordered list of [Page] items for the reader.
     */
    suspend fun getPageList(chapter: Chapter): Result<List<Page>>

    /**
     * Returns the dynamic filter definitions supported by this source.
     */
    fun getFilterList(): SourceFilterList = emptyList()

    /**
     * Optional HTTP headers required for loading images or bypassing hotlink protections
     * (e.g. Referer, User-Agent, custom authorization tokens). Consumed by Coil and OkHttp.
     */
    fun getImageHeaders(): Map<String, String> = emptyMap()

    // endregion

    // region Decoupled Composable UI Injection

    /**
     * Injected Jetpack Compose UI for browsing and searching this specific source.
     *
     * The host app renders this Composable when the user navigates into the source's
     * section. The plugin has complete control over its UI layout (such as custom category
     * carousels, tag filters, or a SAF folder selector for local files).
     *
     * When the user selects a book, the plugin invokes [onBookClick], delegating navigation
     * and library management back to the core host app.
     *
     * @param onBookClick Callback invoked with the selected [Book].
     * @param modifier Modifier applied to the root container.
     */
    @Composable
    fun ContentScreen(
        onBookClick: (Book) -> Unit,
        modifier: Modifier
    )

    // endregion
}
