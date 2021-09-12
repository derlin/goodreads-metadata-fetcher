package ch.derlin.grmetafetcher

import ch.derlin.grmetafetcher.internal.GetHtml
import ch.derlin.grmetafetcher.internal.getAuthorsFromString
import ch.derlin.grmetafetcher.internal.ppDataClass
import ch.derlin.grmetafetcher.internal.removeContentInParentheses
import org.jsoup.nodes.Document
import java.io.Serializable
import java.lang.IllegalStateException

/**
 * Results of a search in GoodReads (see [GoodReadsLookup]).
 *
 * @see GoodReadsLookup.getMatches
 * @see GoodReadsPaginatedSearchResults
 */
data class GoodReadsSearchResult(
    /** The title, as shown in the search results. */
    val title: String,
    /**
     * The list of authors, as shown in the search results.
     * This includes only main authors, thus excluding authors marked as Illustrator, Editor, etc.
     */
    val authors: List<String>,
    /**
     * The URL to the book details, as shown in the search results.
     * This URL can then be passed to [GoodReadsMetadata.fromUrl] to fetch the book's metadata.
     */
    val url: String,
) : Serializable, CompilableToString {

    /** The list of authors, comma-separated. */
    val authorsStr: String
        get() = authors.joinToString(", ")

    /** Get the book metadata for this search result. */
    @Throws(GrNotFoundException::class, GrParseException::class)
    fun getMetadata(): GoodReadsMetadata = metaFromUrl(url)

    override fun toCompilableString(): String = ppDataClass(this)

    companion object {
        /**
         * Get all results from the GoodReads search page [searchUrl].
         *
         * @see GoodReadsUrl
         * @throws GrNotFoundException if no results was found on the page.
         */
        fun fromSearchPage(searchUrl: String): List<GoodReadsSearchResult> =
            (search(searchUrl) ?: throw GrNotFoundException("No results on page $searchUrl")).toList()
    }
}

/**
 * Wrapper of [GoodReadsSearchResult] that can be used to fetch more than the results of the first page.
 *
 * @see GoodReadsLookup.getMatchesPaginated
 * @sample samples.searchGoodReadsPaginated
 */
class GoodReadsPaginatedSearchResults(searchUrl: String) {

    /* Total number of results available, or zero if no result. */
    val totalResults: Int

    /** Total number of result pages available, or zero if no result. */
    val totalPages: Int

    /** Latest fetched page, `0` if [next] has never been called. */
    var currentPage: Int = 0
        private set

    private val results = mutableListOf<GoodReadsSearchResult>()
    private var firstResultsReturned = false
    private var nextPage: String?

    init {
        val document = GetHtml(searchUrl)
        // we need to fetch the page to get the total pages and next link anyways,
        // so cache the results of the first page
        search(document)?.let { results.addAll(it) }
        nextPage = document.getNextResultPageLink()
        totalPages = document.getTotalResultPages()
        totalResults = document.getTotalResults()
    }

    /**
     * Returns true if there are more results available (a next page is present),
     * should always be called prior to [next].
     */
    fun hasNext(): Boolean = (!firstResultsReturned && results.isNotEmpty()) || nextPage != null

    /**
     * Fetch the next list of results from the page, also updating the [results] list.
     * @throws IllegalStateException if this is called while [hasNext] if false
     */
    fun next(): List<GoodReadsSearchResult> =
        if (!firstResultsReturned) {
            firstResultsReturned = true
            results.toList()
        } else {
            val document = GetHtml(requireNotNull(nextPage))
            val newResults = search(document)
                ?: throw IllegalStateException("Call to next() while no more results are available")
            nextPage = document.getNextResultPageLink()
            newResults.toList().also { results.addAll(it) }
        }.also {
            currentPage += 1
        }

    /**
     * Return all the fetched results so far.
     */
    fun allResults() = results.toList()
}

internal fun search(searchUrl: String) = search(GetHtml(searchUrl))
internal fun search(document: Document): Sequence<GoodReadsSearchResult>? {

    return document.getElementsByClass("tableList").first()
        ?.getElementsByTag("tr")
        ?.asSequence()
        ?.mapNotNull { tr ->
            // strip (...), which are used for series
            val grTitle = tr.getElementsByClass("bookTitle").first()?.text()?.removeContentInParentheses() ?: ""
            val grAuthors = getAuthorsFromString(
                // the content is already in the form "by First Last[, First Last]"
                tr.getElementsByClass("authorName__container").joinToString(" ") { it.text() })
            val url = tr.getElementsByTag("a").first()?.attr("href")?.let {
                GoodReadsUrl.home + it.substringBefore("?")
            }

            url?.let { GoodReadsSearchResult(grTitle, grAuthors, url) } // ignore results without URLs (should not happen)
        }
}

internal fun Document.getTotalResults(): Int = this
    .getElementsByClass("searchSubNavContainer")
    .first()
    ?.let { "of about (\\d+) results".toRegex().find(it.text()) }
    ?.groupValues?.get(1)
    ?.toInt() ?: 0

internal fun Document.getNextResultPageLink(): String? = this
    .getElementsByClass("next_page")
    .first()
    ?.takeIf { it.tagName() == "a" } // becomes a span on the last page
    ?.attr("href")
    ?.let { GoodReadsUrl.home + it }

internal fun Document.getTotalResultPages(): Int = this
    .getElementsByClass("next_page")
    .first()
    ?.previousElementSibling()
    ?.text()
    ?.toInt() ?: 0