package samples

import ch.derlin.grmetafetcher.GoodReadsLookup
import ch.derlin.grmetafetcher.GoodReadsMetadata
import ch.derlin.grmetafetcher.GoodReadsPaginatedSearchResults
import ch.derlin.grmetafetcher.Retry
import ch.derlin.grmetafetcher.RetryConfiguration

fun findBookInteractively() {
    val reader = java.util.Scanner(System.`in`)

    print("Enter a book title: ")
    val title = reader.nextLine()
    print("Enter an author (press enter for none): ")
    val author = reader.nextLine().ifBlank { null }

    val matches = GoodReadsLookup(title = title, author = author).getMatches()

    println("\nResults:")
    matches.indices.take(10).forEach { println(" [$it] ${matches[it].title} by ${matches[it].authorsStr}") }
    print("\nEnter the index of the book you want: ")
    val index = reader.nextInt()

    println("\nMetadata:")
    println(matches[index].getMetadata().toCompilableString())
}

fun findBookAutomatically() {
    val p: (GoodReadsMetadata) -> Unit = { println(it.toCompilableString()) }

    // very shot titles need authors to match
    p(GoodReadsMetadata.lookup(title = "substance", author = "claro"))
    // Authors will match without the initials, and can be ignored in search for specific/long enough titles
    p(GoodReadsMetadata.lookup("House of Leaves", "Mark Danielewski", includeAuthorInSearch = false))
    // Subtitle can be ignored in lookup
    p(GoodReadsMetadata.lookup(title = "Masters of Doom"))

    // The same can be achieved using GoodReadsLookup // accents don't matter
    p(GoodReadsLookup(title = "la cle de salomon").findBestMatch().getMetadata())

    // If you know the URL or GoodReads ID, you can use them directly
    p(GoodReadsMetadata.fromGoodReadsId("41940388"))
}

fun searchGoodReadsPaginated() {
    val paginatedResults: GoodReadsPaginatedSearchResults = GoodReadsLookup("how time war").getMatchesPaginated()
    println("Found ${paginatedResults.totalResults} results across ${paginatedResults.totalPages} pages")
    println("Showing only first result of all pages...")
    println()

    while (paginatedResults.hasNext()) {
        paginatedResults.next().first().let {
            println("page [${paginatedResults.currentPage}] --> \"${it.title}\" by ${it.authorsStr}")
        }
    }
}

@Suppress("unused")
fun lookupWithRetry() {
    // A Retry must be instantiated with a configuration.
    // It also supports a custom lambda to tell when to retry. By default, it retries on any 5XX server error.
    var retrier: Retry
    // Two configurations exist: EXPONENTIAL, and SIMPLE
    retrier = Retry(RetryConfiguration.EXPONENTIAL)
    // Each default configuration can be customized, e.g.
    retrier = Retry(RetryConfiguration.EXPONENTIAL.copy(maxRetries = 10))
    // Or a completely custom configuration can be provided:
    retrier = Retry(RetryConfiguration(maxRetries = 3, interval = 200, multiplier = 1f))

    // Once we have a Retry instance, we can use .run, like this:
    val result = retrier.run { GoodReadsMetadata.lookup(title = "Project Hail Mary") }
    println(result.toCompilableString())
}
