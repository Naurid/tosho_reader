package com.naurid.tosho.domain.model

/**
 * Encapsulates dynamic filter parameters used by source plugins during search and discovery.
 */
sealed class SourceFilter(val name: String) {
    data class Text(val title: String, val state: String = "") : SourceFilter(title)
    data class Select(val title: String, val values: List<String>, val selectedIndex: Int = 0) : SourceFilter(title)
    data class Checkbox(val title: String, val checked: Boolean = false) : SourceFilter(title)
    data class TriState(val title: String, val state: State = State.IGNORED) : SourceFilter(title) {
        enum class State { IGNORED, INCLUDED, EXCLUDED }
    }
    data class Group(val title: String, val filters: List<SourceFilter>) : SourceFilter(title)
    data class Sort(val title: String, val values: List<String>, val selectedIndex: Int = 0, val ascending: Boolean = false) : SourceFilter(title)
}

typealias SourceFilterList = List<SourceFilter>
