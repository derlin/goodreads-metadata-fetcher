package samples

import ch.derlin.grmetafetcher.GoodReadsLookup
import ch.derlin.grmetafetcher.GoodReadsMetadata
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties

fun main() {
    findBookAutomatically()
}

fun findBookInteractively() {
    val reader = java.util.Scanner(System.`in`)

    print("Enter a book title: ")
    val title = reader.nextLine()
    print("Enter an author (press enter for none): ")
    val author = reader.nextLine().ifBlank { null }

    val matches = GoodReadsLookup(title = title, author = author).getMatches()

    println("\nResults:")
    matches.indices.take(10).forEach { println(" [$it] ${matches[it].title} by ${matches[it].authors.joinToString { "," }}") }
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
    p(GoodReadsMetadata.lookup(title = "Freakonomics"))

    // The same can be achieved using GoodReadsLookup // accents don't matter
    p(GoodReadsLookup(title = "la cle de salomon").findBestMatch().getMetadata())

    // If you know the URL or GoodReads ID, you can use them directly
    p(GoodReadsMetadata.fromGoodReadsId("41940388"))
}

fun lala() {
    val gr = GoodReadsMetadata(
        url = "https://www.goodreads.com/book/show/52757776-substance",
        id = "527577762",
        title = "Substance",
        authors = listOf("Claro"),
        isbn = null,
        pages = 350,
        pubDate = LocalDate.parse("2019-08-01")
    )
    //println(gr.toString().replace(", ", ",\n  ").replaceFirst("(", "(\n  ").dropLast(1) + "\n)")
    println(gr.toCompilableString())
    ppDataClass(gr)
}

fun <T : Any> ppDataClass(data: T) {
    val klass = data::class as KClass<T>

    val properties = klass.declaredMemberProperties.associate { it.name to it.get(data) }

    "([A-Za-z0-9_]+)=".toRegex().findAll(data.toString())
        .map { it.groupValues[1] }
        .forEach { name ->
            println("$name = ${properties[name]}")
        }
}
