package ch.derlin.grmetafetcher

import ch.derlin.grmetafetcher.internal.cleanAuthorForSearchQuery
import ch.derlin.grmetafetcher.internal.cleanTitleForSearchQuery
import java.net.URLEncoder


object GoodReadsUrl {

    /**
     * Base URL for the GoodReads front page.
     */
    const val home = "https://www.goodreads.com"

    /**
     * Get the full URL to a GoodReads detail page from an GoodReads ID
     */
    fun forBookId(id: String) = "$home/book/show/$id"

    /**
     * Get the full URL for a search query.
     * If author is null, the search will be in title only, else in all fields.
     */
    fun queryFor(title: String, author: String? = null): String {
        // Usually, search in title only is better
        val searchString = cleanTitleForSearchQuery(title) + (author?.let { " " + cleanAuthorForSearchQuery(it) } ?: "")
        val searchUrl = "$home/search?&search_type=books&q=" + URLEncoder.encode(searchString, "UTF-8")

        return if (author == null) "$searchUrl&search%5Bfield%5D=title" else searchUrl
    }
}