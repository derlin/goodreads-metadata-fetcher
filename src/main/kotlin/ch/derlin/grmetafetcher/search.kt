package ch.derlin.grmetafetcher

import ch.derlin.grmetafetcher.internal.*
import java.io.Serializable

data class GoodReadsSearchResult(
    /** The title, as shown in the search results */
    val title: String,
    /** The list of main authors (excluding illustrators, etc), as shown in the search results */
    val authors: List<String>,
    /** The URL to the book details */
    val url: String,
) : Serializable

class GoodReadsLookup(
    /** The title to look for */
    val title: String,
    /** The author(s) to look for, in the form "FirstName Lastname" (separated by "," if more than one */
    val author: String? = null,
    /** Whether to use the author in the search query (search all) or only search in title */
    val includeAuthorInSearch: Boolean = author != null
) {
    /**
     * Search URL on GoodReads.
     * (note that the title and authors are sanitized for better results)
     */
    val searchUrl: String by lazy {
        GoodReadsUrl.queryFor(title, if (includeAuthorInSearch) author else null)
    }

    /** Get the list of matches for the given title (first page only, thus <= 20 results) */
    fun getMatches(): List<GoodReadsSearchResult> = search(this).toList()

    /**
     * Find the best match for the given title (and authors) on GoodReads.
     * Note that for a result to be a match, the following conditions must apply:
     * - the title must be an "exact match" (subtitles ignored, that is content after ":")
     * - the author(s) must all be present in the list of authors, in the correct order
     *
     * Note that all comparisons are diacritics, symbols, space and casing insensitive
     */
    @Throws(GrNotFoundException::class)
    fun findBestMatch(): GoodReadsSearchResult {
        val results = search(this)
        // first, try to find an exact match
        return results.find { doTitlesMatch(title, it.title) && doAuthorsMatch(author, it.authors) }
        // no result ? try again, this time removing any subtitle in the result (things after ":")
            ?: results.find { doTitlesMatch(title, it.title.substringBefore(":")) && doAuthorsMatch(author, it.authors) }
            ?: throw GrNotFoundException("Could not find a book matching $this")
    }

    override fun toString(): String = "Lookup(title=$title, author=$author, searchUrl=$searchUrl)"
}

internal fun search(lookup: GoodReadsLookup): Sequence<GoodReadsSearchResult> {
    val document = GetHtml(lookup.searchUrl)

    return (document.getElementsByClass("tableList").first() ?: throw GrMissingElementException("No match found for $lookup"))
        .getElementsByTag("tr")
        .asSequence()
        .mapNotNull { tr ->
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

internal fun doTitlesMatch(givenTitle: String, goodReadsTitle: String) =
    fuzzyCompare(givenTitle, goodReadsTitle, strict = true)

internal fun doAuthorsMatch(givenAuthors: String?, goodReadsAuthors: List<String>) =
    givenAuthors == null ||
            fuzzyCompare(givenAuthors.removeInitials(), goodReadsAuthors.joinToString(" ").removeInitials(), strict = false)

