package ch.derlin.grmetafetcher

import ch.derlin.grmetafetcher.internal.fuzzyCompare
import ch.derlin.grmetafetcher.internal.ppDataClass
import ch.derlin.grmetafetcher.internal.removeInitials

/**
 * Classes implementing this interface should provide a method for printing themselves into "compilable strings",
 * that is strings that can be copy-pasted into a kotlin file and compile.
 */
interface CompilableToString {
    /** Should return the String representation of this class as a compilable snippet (can be copy-pasted into Kotlin code). */
    fun toCompilableString(): String
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
    val includeAuthorInSearch: Boolean = author != null,
) : CompilableToString {
    /**
     * Search URL on GoodReads for. Note that the title (and maybe authors) are sanitized for better results.
     */
    val searchUrl: String by lazy {
        GoodReadsUrl.queryFor(title, if (includeAuthorInSearch) author else null)
    }

    /**
     * Get the list of matches for the given search query (see [searchUrl]).
     * Note that only the results appearing on the first page are returned, hence a maximum of 20 results.
     *
     * @throws GrNotFoundException if no result is found.
     */
    @Throws(GrNotFoundException::class)
    fun getMatches(): List<GoodReadsSearchResult> = search().toList()

    /**
     * Get the list of matches for the given search query (see [searchUrl]), paginated.
     *
     * @throws GrNotFoundException if no result is found.
     * @sample samples.searchGoodReadsPaginated
     */
    @Throws(GrNotFoundException::class)
    fun getMatchesPaginated(): GoodReadsPaginatedSearchResults = GoodReadsPaginatedSearchResults(this.searchUrl)

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
        val results = search()
        // first, try to find an exact match
        return results.find { doTitlesMatch(title, it.title) && doAuthorsMatch(author, it.authors) }
        // no result ? try again, this time removing any subtitle in the result (things after ":")
            ?: results.find { doTitlesMatch(title, it.title.substringBefore(":")) && doAuthorsMatch(author, it.authors) }
            ?: throw GrNotFoundException("Could not find a book matching $this")
    }

    override fun toCompilableString(): String = ppDataClass(this)

    private fun search() =
        search(this.searchUrl) ?: throw GrMissingElementException("No match found for $this")

}

internal fun doTitlesMatch(givenTitle: String, goodReadsTitle: String) =
    fuzzyCompare(givenTitle, goodReadsTitle, strict = true)

internal fun doAuthorsMatch(givenAuthors: String?, goodReadsAuthors: List<String>) =
    givenAuthors == null ||
            fuzzyCompare(givenAuthors.removeInitials(), goodReadsAuthors.joinToString(" ").removeInitials(), strict = false)
