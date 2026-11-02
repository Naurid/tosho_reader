package com.naurid.tosho

import org.jsoup.Jsoup
import org.junit.Test
import org.junit.Assert.*

class ScraperPlaygroundTest {

    @Test
    fun testParseLocalHtml() {
        // 1. Load your local Explore_TEMP.html file from test resources
        val inputStream = javaClass.getResourceAsStream("/Explore_TEMP.html")
        assertNotNull("Explore_TEMP.html not found in src/test/resources/", inputStream)

        val html = inputStream!!.bufferedReader().use { it.readText() }

        // 2. Parse it with Jsoup (mimicking your plugin's parsing logic)
        val doc = Jsoup.parse(html)

        // 3. Test your selectors and print results
        val items = doc.select("a[href*=\"/manga/\"] img[alt]") // Update selector to match your HTML
        println("Found ${items.size} items in local HTML.")

        items.forEach { element ->
            val title = element.attr("alt")
            val cover = element.attr("src")
            println("Title: $title | Cover: $cover")
        }

        assertTrue(doc.allElements.isNotEmpty())
    }
}