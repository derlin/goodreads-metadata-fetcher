package ch.derlin.grmetafetcher

import org.jsoup.nodes.Element
import java.lang.RuntimeException

/**
 * Exception thrown when no match is found on GoodReads search results.
 */
class GrNotFoundException(message: String) : Exception(message)

/**
 * Exception thrown when an HTML element is missing on the page,
 * making the parsing impossible (e.g. no authors class).
 */
class GrMissingElementException(message: String) : RuntimeException(message)

/**
 * Exception thrown in case the HTML elements are present,
 * but something goes wrong while parsing the content (e.g. wrong authors format).
 */
class GrParseException(message: String) : RuntimeException(message)


internal fun Element?.required(message: () -> String): Element {
    return requireNotNull(this) {
        GrMissingElementException(message())
    }
}