package ch.derlin.grmetafetcher

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isSuccess
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.jupiter.api.Test

class SearchTest {

    @Test
    fun `best match finds exact match for title and fuzzy match with author`() {

        val results = results(
            Pair("simple title: with a subtitle", "The Author"),
            Pair("simple title", "The Author"),
            Pair("other title", "Other Author"),
            Pair("SOME T I T L E   With !àccents!", "Space Authôr"),
            Pair("Multiple Authors", "Author One, Author Two, Author Three"),
            Pair("Another: with subtitle", "The Author")
        )

        mockkStatic(this::class.java.packageName + ".SearchKt") // this is needed to mock functions outside classes
        every { search(any()) } returns (results.asSequence())

        assertAll {
            // simple exact match
            assertThat(GoodReadsLookup("simple title with a subtitle", "The Author").findBestMatch())
                .isEqualTo(results[0])
            assertThat(GoodReadsLookup("simple title", "The Author").findBestMatch())
                .isEqualTo(results[1])
            // match without author
            assertThat(GoodReadsLookup("simple title", null).findBestMatch())
                .isEqualTo(results[1])
            // simple fuzzy match (1)
            assertThat(GoodReadsLookup("  !! SIMPLé TITLE  !!", "thE K. authoR, ").findBestMatch())
                .isEqualTo(results[1])
            // simple fuzzy match (2)
            assertThat(GoodReadsLookup("some title with !accents!", "space author").findBestMatch())
                .isEqualTo(results[3])
            // match with multiple authors
            listOf("One", "Two", "Three").forEach {
                assertThat(GoodReadsLookup("multiple authors", "author $it").findBestMatch())
                    .isEqualTo(results[4])
            }
            // match without subtitle
            assertThat(GoodReadsLookup("another").findBestMatch())
                .isEqualTo(results[5])
            assertThat(GoodReadsLookup("another", "the author").findBestMatch())
                .isEqualTo(results[5])

            // no match on title
            listOf("doesnt exist", "simple title with another subtitle", "").forEach {
                assertThat { GoodReadsLookup(title = it, null).findBestMatch() }.isFailure()
            }
            // no match on author
            assertThat { GoodReadsLookup("simple title", "Another Author").findBestMatch() }.isFailure()
        }
    }

    private fun results(vararg results: Pair<String, String>) = results.map { pair ->
        GoodReadsSearchResult(title = pair.first, authors = pair.second.split("|").map { it.trim() }, url = "@URL@")
    }
}