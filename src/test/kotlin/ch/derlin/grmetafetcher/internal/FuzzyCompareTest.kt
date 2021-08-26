package ch.derlin.grmetafetcher.internal

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class FuzzyCompareTest {

    @Test
    fun `fuzzyCompare strict positive matches`() {
        assertAll {
            listOf(
                "Camel Case" to "camel case",
                "H. G. Wells" to "H.G. Wells",
                "  S O M E @? S P A C E\n!;" to "somespace",
                "éàôç..." to "eaoc",
            ).forEach { pair ->
                assertThat(pair).transform { fuzzyCompare(it.first, it.second, strict = true) }.isTrue()
            }
        }
    }

    @Test
    fun `fuzzyCompare strict negative matches`() {
        assertAll {
            listOf(
                "Title One" to "Title Two",
                "mispeling" to "mispelling",
                "prefix suffix" to "prefix",
                "title" to "title: with subtitle",
            ).forEach { pair ->
                assertThat(pair).transform { fuzzyCompare(it.first, it.second, strict = true) }.isFalse()
            }
        }
    }

    @Test
    fun `fuzzyCompare in strict mode should match whole string`() {
        listOf(
            "prefix" to "prefix suffix",
            "Author One" to "Author One, Author Two",
            "Author One" to "Author Two, Author One",
            "x-y" to "xyz",
        ).forEach { pair ->
            assertThat(pair, name = "not strict").transform { fuzzyCompare(it.first, it.second, strict = false) }.isTrue()
            assertThat(pair, name = "strict").transform { fuzzyCompare(it.first, it.second, strict = true) }.isFalse()
        }
    }
}