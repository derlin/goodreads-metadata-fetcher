package ch.derlin.grmetafetcher

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
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
            assertThat(GoodReadsLookup("  !! SIMPLé TITLE  !!", "thE authoR, ").findBestMatch())
                .isEqualTo(results[1])
            // simple fuzzy match (2)
            assertThat(GoodReadsLookup("some title with !accents!", "space author").findBestMatch())
                .isEqualTo(results[3])
            // match with multiple authors
            listOf("One", "Two", "Three").forEach {
                assertThat(GoodReadsLookup("multiple authors", "author $it").findBestMatch())
                    .isEqualTo(results[4])
            }

            // no match on title
            listOf("doesnt exist", "simple title with another subtitle", "").forEach {
                assertThat { GoodReadsLookup(title = it, null).findBestMatch() }.isFailure()
            }
            // no match on author
            assertThat { GoodReadsLookup("simple title", "Another Author").findBestMatch() }.isFailure()
        }
    }

    @Test
    fun `clean title for GoodReads search`() {
        listOf(
            "Tïtlé (First of series #1)" to "title",
            "Lili &   Stitch " to "lili and stitch",
            "one:under_score" to "one under_score",
            "1,000 treasures" to "1000 treasures",
        ).forEach {  pair ->
            assertThat(pair.first).transform { cleanTitleForSearchQuery(it) }.isEqualTo(pair.second)
        }
    }

    private fun results(vararg results: Pair<String, String>) = results.map { pair ->
        GoodReadsSearchResult(title = pair.first, authors = pair.second.split("|").map { it.trim() }, url = "@URL@")
    }
}