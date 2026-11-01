package com.naurid.tosho.domain.model

/**
 * Domain representation of an individual page within a chapter.
 */
data class Page(
    val index: Int,
    val url: String = "",
    val imageUrl: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val textContent: String? = null
)
