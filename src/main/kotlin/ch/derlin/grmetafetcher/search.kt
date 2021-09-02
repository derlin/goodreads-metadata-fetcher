package ch.derlin.grmetafetcher

import ch.derlin.grmetafetcher.internal.*
import java.io.Serializable

interface CompilableToString {
    /** Should return the String representation of this class as a compilable snippet (can be copy-pasted into Kotlin code). */
    fun toCompilableString(): String
}

/**
 * Results of a search in GoodReads (see [GoodReadsLookup]).
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

    /** Get the book metadata for this search result. */
    @Throws(GrNotFoundException::class, GrParseException::class)
    fun getMetadata(): GoodReadsMetadata = metaFromUrl(url)

    override fun toCompilableString(): String = ppDataClass(this)
}

/**
 * Find books on GoodReads.
 *
 * @sample samples.findBookAutomatically
 * @sample samples.findBookInteractively
 */
data class GoodReadsLookup(
    /**
     * The (partial) title to look for.
     * If you plan to use [findBestMatch], the title should be "exact".
     */
    val title: String,
    /**
     * The (partial) author(s) to look for. If multiple authors, they can be separated by any of ` `, `,` or `&`.
     * If you plan to use [findBestMatch], the author should at least match the first author in the form "FirstName Lastname".
     */
    val author: String? = null,

    /**
     * Whether to use the author in the search query (search all fields) or not (search title only).
     * This is useful because except when the title is very generic, a search in title only gives better results.
     */
    val includeAuthorInSearch: Boolean = author != null
): CompilableToString {
    /**
     * Search URL on GoodReads for. Note that the title (and maybe authors) are sanitized for better results.
     */
    val searchUrl: String by lazy {
        GoodReadsUrl.queryFor(title, if (includeAuthorInSearch) author else null)
    }

    /**
     * Get the list of matches for the given search query (see [searchUrl]).
     * Note that only the results appearing on the first page are returned, hence a maximum of 20 results.
     */
    fun getMatches(): List<GoodReadsSearchResult> = search(this).toList()

    /**
     * Search and try to find the best match for the given title (and maybe authors) on GoodReads.
     *
     * For a result to be a match, the following conditions must apply:
     * - the title must be an "exact match" (subtitles ignored, that is content after ":")
     * - the author(s) must all be present in the list of authors, in the correct order, in the form "FirstName LastName"
     *
     * Note that all comparisons are diacritics, symbols, space and casing insensitive.
     *
     * @throws GrNotFoundException if no book matching [title] (and [author]) is found (use [kotlin.runCatching] to get `null` instead)
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

    override fun toCompilableString(): String = ppDataClass(this)
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

