package com.naurid.tosho.domain.model

/**
 * Domain representation of a reading chapter or volume.
 */
data class Chapter(
    val id: String,
    val bookId: String,
    val url: String,
    val name: String,
    val chapterNumber: Float = -1f,
    val scanlator: String? = null,
    val dateUpload: Long = 0L,
    val read: Boolean = false,
    val bookmark: Boolean = false,
    val lastPageRead: Int = 0
)
