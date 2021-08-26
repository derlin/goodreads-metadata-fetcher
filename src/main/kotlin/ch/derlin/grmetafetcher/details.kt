package ch.derlin.grmetafetcher

import org.jsoup.nodes.Document
import java.io.Serializable
import java.time.LocalDate
import ch.derlin.grmetafetcher.internal.*

data class GoodReadsMetadata(
    /** The URL of the book on GoodReads (could also be reconstructed from the ID) */
    val url: String,
    /** The GoodReads ID for this book */
    val id: String,

    /** The title, as found on GoodReads */
    val title: String,
    /** The list of main authors, as found on GoodReads (excluding illustrators, etc) */
    val authors: List<String>,

    /** The ISBN, if specified. If both ISBN10 and ISBN13 are present on the page, the latter is used */
    val isbn: String?,
    /** The number of pages, if present.
     * (as shown in the details section, usually for the hardcover edition) */
    val pages: Int?,
    /** The earliest publication date, if present */
    val pubDate: LocalDate?,
) : Serializable {

    companion object {
        @Throws(GrNotFoundException::class)
        fun lookup(title: String, author: String? = null, includeAuthorInSearch: Boolean = author != null): GoodReadsMetadata =
            GoodReadsLookup(title, author, includeAuthorInSearch).findBestMatch().getMetadata()

        @Throws(GrParseException::class)
        fun fromUrl(url: String): GoodReadsMetadata =
            metaFromUrl(url)

        @Throws(GrParseException::class)
        fun fromGoodReadsId(id: String): GoodReadsMetadata =
            metaFromUrl(GoodReadsUrl.forBookId(id))
    }
}

fun GoodReadsSearchResult.getMetadata(): GoodReadsMetadata =
    metaFromUrl(url)

private fun metaFromUrl(url: String): GoodReadsMetadata {
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
    this.getElementsByAttribute("data-book-id").first()
        .required { "Could not find goodreads ID" }
        .attr("data-book-id")

private fun Document.getTitle(): String =
    this.getElementById("bookTitle")
        .required { "Could not find book title" }
        .text()

private fun Document.getAuthors(): List<String> =
    this.getElementById("bookAuthors")
        .required { "Could not find authors div" }
        .text().let { text ->
            getAuthorsFromString(text).ifEmpty {
                throw GrParseException("Could not extract authors from string: $text")
            }
        }

private fun Document.getIsbn(): String? =
    this.getElementById("bookDataBox")
        ?.getElementsByClass("infoBoxRowTitle")
        ?.find { it.text().contains("ISBN") }
        ?.nextElementSibling()
        ?.text()
        ?.let { getIsbnFromString(it) }

private fun Document.getNumberOfPages(): Int? =
    this.getElementById("details")
        ?.getElementsByAttribute("itemprop")
        ?.find { it.attr("itemprop") == "numberOfPages" }
        ?.text() // usually in the form XXX pages
        ?.split(" ")?.first()?.toInt()

private fun Document.getPublicationDate(): LocalDate? {
    // publication information is in the second div of #details
    val details = this.getElementById("details")
    if ((details?.childrenSize() ?: 0) < 2) return null

    return details?.child(1)?.text()?.let { getPublicationDateFromString(it) }
}