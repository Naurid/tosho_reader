package com.naurid.tosho.domain.model

/**
 * Core domain representation of a book/manga/comic in Tosho.
 * Decoupled from any database entity or scraper DTO.
 */
data class Book(
    val id: String,
    val sourceId: String,
    val url: String,
    val title: String,
    val author: String? = null,
    val artist: String? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val status: BookStatus = BookStatus.UNKNOWN,
    val coverUrl: String? = null,
    val customCoverUrl: String? = null,
    val inLibrary: Boolean = false,
    val format: BookFormat = BookFormat.MANGA
)

enum class BookStatus {
    UNKNOWN,
    ONGOING,
    COMPLETED,
    LICENSED,
    PUBLISHING_FINISHED,
    CANCELLED,
    ON_HIATUS
}

enum class BookFormat {
    MANGA,
    COMIC,
    WEBTOON,
    EPUB
}
