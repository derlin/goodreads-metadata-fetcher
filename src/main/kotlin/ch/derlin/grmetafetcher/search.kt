package ch.derlin.grmetafetcher

import java.net.URLEncoder

internal const val GOODREADS_BASE_URL = "https://www.goodreads.com"

data class GoodReadsSearchResult(
    /** The title, as shown in the search results */
    val title: String,
    /** The list of main authors (excluding illustrators, etc), as shown in the search results */
    val authors: List<String>,
    /** The URL to the book details */
    val url: String,
)

class GoodReadsLookup(
    /** The title to look for */
    val title: String,
    /** The author(s) to look for, in the form "FirstName Lastname" (separated by "," if more than one */
    val author: String? = null,
) {
    /**
     * Search URL (including the query string matching the lookup title) on GoodReads.
     * (note that the title is sanitized for better results)
     */
    val searchUrl: String by lazy { createGoodReadsQueryUrl(title) }

    /** Get the list of matches for the given title (first page only, thus <= 20 results) */
    fun getAllMatches(): List<GoodReadsSearchResult> = search(this).toList()

    /**
     * Find the best match for the given title (and authors) on GoodReads.
     * Note that for a result to be a match, the following conditions must apply:
     * - the title must be an "exact match"
     * - the author(s) must all be present in the list of authors, in the correct order
     *
     * Note that all comparisons are diacritics, symbols, space and casing insensitive
     */
    @Throws(GrNotFoundException::class)
    fun findBestMatch(): GoodReadsSearchResult = search(this).find { result ->
        val titleMatch = fuzzyCompare(title, result.title, strict = true)
        val authorMatch = author?.run { fuzzyCompare(this, result.authors.joinToString(" ")) } != false

        titleMatch && authorMatch
    } ?: throw GrNotFoundException("Could not find a book matching $this")

    override fun toString(): String = "Lookup(title=$title, author=$author, searchUrl=$searchUrl)"
}

internal fun search(lookup: GoodReadsLookup): Sequence<GoodReadsSearchResult> {
    val document = GetHtml(lookup.searchUrl)

    return (document.getElementsByClass("tableList").first() ?: throw GrMissingElementException("No match found for $lookup"))
        .getElementsByTag("tr")
        .asSequence()
        .mapNotNull { tr ->
            // remove trailing (...), which denotes series
            val grTitle = tr.getElementsByClass("bookTitle").first()?.text()?.substringBefore("(") ?: ""

            val grAuthors = getAuthorsFromString(
                // the content is already in the form "by First Last[, First Last]"
                tr.getElementsByClass("authorName__container").joinToString(" ") { it.text() })

            val url = tr.getElementsByTag("a").first()?.attr("href")?.let {
                GOODREADS_BASE_URL + it.substringBefore("?")
            }

            url?.let { GoodReadsSearchResult(grTitle, grAuthors, url) } // ignore results without URLs (should not happen)
        }
}

internal fun createGoodReadsQueryUrl(title: String): String {
    // We could do "https://www.goodreads.com/search?search_type=%22books%22&search[query]=" + title_and_author,
    // but the search is often better if we focalize on the title only.
    // Note that if author is used, initials must be stripped (and other cleanup done), using e.g.:
    //     author.removeDiacritics().replace("(\\b|\\W)[A-Z]\\.".toRegex(), " ").trim()
    return "$GOODREADS_BASE_URL/search?&search_type=books&search%5Bfield%5D=title&q=" +
            URLEncoder.encode(cleanTitleForSearchQuery(title), "UTF-8")
}

internal fun cleanTitleForSearchQuery(title: String) =
    // GoodReads doesn't like commas, semi-colon... or parenthesis in the search URL, so remove all
    title.toLowerCase()
        .replace("(\\(.*\\))".toRegex(), "")
        .removeDiacritics()
        .replace(",", "")
        .replace(":", " ")
        .replace(" & ", " and ")
        .replace("\\s+".toRegex(), " ")
        .trim()


