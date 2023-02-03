package ch.derlin.grmetafetcher.internal

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.HttpURLConnection
import java.net.URL


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

internal fun getPublisherFromString(publicationString: String): String? =
    publicationString
        .substringAfter(" by ", "") // fail if by is missing
        .substringBefore("(first") // followed by (first published ...) so remove that
        .trim()
        .let { it.ifBlank { null } }
