package ch.derlin.grmetafetcher

import org.jsoup.nodes.Document
import java.io.Serializable
import java.time.LocalDate
import ch.derlin.grmetafetcher.internal.*
import java.time.format.DateTimeFormatter

/**
 * Get book metadata from a GoodReads details page, a GoodReads ID or title/author.
 *
 * @sample samples.findBookAutomatically
 */
data class GoodReadsMetadata(

    /** The complete title, as found on GoodReads. */
    val title: String,
    /** The list of *main* authors (excluding editor, illustrators, etc), as found on GoodReads. */
    val authors: List<String>,

    /**
     * The URL of the book on GoodReads.
     * Note that it can also be reconstructed from the [id], using [GoodReadsUrl.forBookId].
     */
    val url: String,
    /** The GoodReads ID for this book. */
    val id: String,

    /**
     * The ISBN of the edition shown in the details page of GoodReads, if present.
     * If both ISBN10 and ISBN13 are present on the page, the latter is used.
     *
     * Note that GoodReads usually has a default edition for each book, and offers a link to other editions.
     * This library only looks at this default edition, whatever its format (Kindle, paperback, ...).
     */
    val isbn: String?,
    /**
     * The number of pages, if present.
     *
     * Note that GoodReads usually has a default edition for each book, and offers a link to other editions.
     * This library only looks at this default edition, and pages are usually only present for paperbacks.
     * */
    val pages: Int?,
    /**
     * The earliest publication date (first published), if present.
     *
     * Note that if the month and/or day is missing, they default to January, 1th.
     */
    val pubDate: LocalDate?,
) : CompilableToString, Serializable {

    override fun toCompilableString(): String = ppDataClass(this)

    /** The list of authors, comma-separated. */
    val authorsStr: String
        get() = authors.joinToString(", ")

    companion object {
        /**
         * Try to find a match on GoodReads using the given parameters, then fetch and return the associated metadata.
         * This is equivalent to `GoodReadsLookup(... params ...).findBestMatch().getMetadata()`.
         *
         * @see GoodReadsLookup.findBestMatch
         * @see GoodReadsSearchResult.getMetadata
         *
         * @throws GrNotFoundException if no book matching the parameters is found
         */
        @Throws(GrNotFoundException::class, GrParseException::class, GrMissingElementException::class)
        fun lookup(title: String, author: String? = null, includeAuthorInSearch: Boolean = author != null): GoodReadsMetadata =
            GoodReadsLookup(title, author, includeAuthorInSearch).findBestMatch().getMetadata()

        /**
         * Get the metadata from a GoodReads book detail page.
         *
         * @throws GrParseException if some mandatory details could not be found on the page.
         */
        @Throws(GrParseException::class)
        fun fromUrl(url: String): GoodReadsMetadata =
            metaFromUrl(url)

        /**
         * Get the metadata from a GoodReads book ID.
         *
         * @see GoodReadsUrl.forBookId
         * @see fromUrl
         */
        @Throws(GrParseException::class)
        fun fromGoodReadsId(id: String): GoodReadsMetadata =
            metaFromUrl(GoodReadsUrl.forBookId(id))
    }
}

internal fun metaFromUrl(url: String): GoodReadsMetadata {
    val document = GetHtml(url)
    return GoodReadsMetadata(
        url = url,
        id = document.getGoodReadsId(),
        title = document.getTitle(),
        authors = document.getAuthors(),
        isbn = document.getIsbn(),
        pages = document.getNumberOfPages(),
        pubDate = document.getPublicationDate()
    )
}

// --- EXTRACT METADATA FROM DOCUMENT

private fun Document.getGoodReadsId(): String =
    // The ID can only be found in the canonical link on a detail page
    // <link href="https://www.goodreads.com/book/show/58950899-the-this" rel="canonical">
    this.getElementsByAttributeValue("rel", "canonical").first()
        .required { "Could not find canonical URL for GoodReads ID" }
        .attr("href")
        .split("/").last()
        .takeWhile { it.isDigit() }

private fun Document.getTitle(): String =
    this.getElementsByAttributeValue("data-testid", "bookTitle").first()
        .required { "Could not find book title" }
        .text()

private fun Document.getAuthors(): List<String> =
    this.getElementsByClass("ContributorLinksList").first()
        .required { "Could not find authors div" }
        .text().let { text ->
            getAuthorsFromString(text).ifEmpty {
                throw GrParseException("Could not extract authors from string: $text")
            }
        }

private fun Document.getIsbn(): String? =
    "\"isbn\":\"(\\d+)\"".toRegex().find(this.getJsonData())?.groupValues?.get(1)

private fun Document.getNumberOfPages(): Int? =
    "\"numberOfPages\": ?(\\d+)".toRegex().find(this.getJsonData())?.groupValues?.get(1)?.toInt()

private fun Document.getPublicationDate(): LocalDate? =
    this.getElementsByAttributeValue("data-testid", "publicationInfo").first()
        ?.text()
        ?.removePrefix("Published ")?.removePrefix("First published ")
        ?.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("MMMM d, yyyy")) }



private fun Document.getJsonData(): String =
    this.getElementsByAttributeValue("type", "application/ld+json").first()
        .required { "Could not find JSON metadata information in page" }
        .data()
