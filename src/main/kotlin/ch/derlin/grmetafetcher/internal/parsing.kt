package ch.derlin.grmetafetcher.internal

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale


// ------- GET HTML

internal object GetHtml {
    /**
     * Get the HTML content of a page, in the form of a JSoup Document
     */
    operator fun invoke(url: String): Document {
        val urlConnection = URL(url).openConnection() as HttpURLConnection
        urlConnection.instanceFollowRedirects = true
        try {
            val text = urlConnection.inputStream.bufferedReader().readText()
            return requireNotNull(Jsoup.parse(text)) {
                "Jsoup returned a null document"
            }
        } finally {
            urlConnection.disconnect()
        }
    }
}

// ------- PARSING

internal val pubDateFormat = DateTimeFormatterBuilder()
    // possible date formats for published date: 2004, December 2004, December 30th 2004
    // note that the "th" in 30th is not supported, must be stripped out beforehand
    .appendOptional(DateTimeFormatter.ofPattern("yyyy"))
    .appendOptional(DateTimeFormatter.ofPattern("MMMM yyyy"))
    .appendOptional(DateTimeFormatter.ofPattern("MMMM d yyyy"))
    .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
    .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
    .toFormatter(Locale.ENGLISH)

internal fun getAuthorsFromString(authors: String): List<String> =
// Authors is in the form "by First Last[, First Last]"
// Some authors have one or multiple roles, e.g. "First Last (GoodReads Author)", or "First Last (Illustrator)"
    // Here, only the main authors are returned, that no role OR GoodReads Author role
    authors.substringAfter("by ")
        .split(",")
        .mapNotNull { "([^(]+) ?(\\(.+\\))?".toRegex().find(it.trim())?.groupValues }
        .mapNotNull { (_, author, roles) ->
            author.trim().takeIf { roles.isBlank() || "(goodreads author)" in roles.lowercase() }
        }

internal fun getIsbnFromString(isbn: String): String? =
    // The last digit of ISBN10 is a checksum (modulo 11), and the roman numeral "X" is used to represent "10"
    "(\\d{9,10}X?)(?: *\\(ISBN13: (\\d{13})\\))?".toRegex().matchEntire(isbn.trim())?.groupValues?.last { it.isNotBlank() }

internal fun getPublisherFromString(publicationString: String): String? =
    publicationString
        .substringAfter(" by ", "") // fail if by is missing
        .substringBefore("(first") // followed by (first published ...) so remove that
        .trim()
        .let { it.ifBlank { null } }

internal fun getPublicationDateFromString(publicationString: String): LocalDate? {
    // In the form:
    //    published XXX
    //    published XXX by SomeOne (first published YYY)
    // The actual date format may vary: "2004", "December 2004", or "December 30th 2004"
    val published = "^Published[\\n\\s]*([\\w\\s]+)".toRegex()
        .find(publicationString.substringBefore(" by"))?.groupValues?.get(1)?.trim()
    val firstPublished = ".*\\(first published ([\\w\\s]+)".toRegex()
        .find(publicationString)?.groupValues?.get(1)?.trim()

    val pubDateString = if (published == null) {
        firstPublished
    } else {
        // If both are set AND we have same years, use the "published" as it could be more accurate
        if (firstPublished == null || published.takeLast(4) == firstPublished.takeLast(4))
            published else firstPublished
    }

    if (pubDateString == null) return null

    return runOrNull {
        LocalDate.parse(pubDateString.replace("(\\d)(st|nd|rd|th)".toRegex(), "$1"), pubDateFormat)
    }
}

// ------- OTHER

internal fun <R> runOrNull(r: () -> R): R? =
    try {
        r()
    } catch (e: Exception) {
        null
    }
