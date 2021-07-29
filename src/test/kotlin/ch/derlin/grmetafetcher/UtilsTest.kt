package ch.derlin.grmetafetcher

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class UtilsTest {

    @Test
    fun `fuzzyCompare positive matches`() {
        assertAll {
            listOf(
                "Camel Case" to "camel case",
                "H. G. Wells" to "H.G. Wells",
                "  S O M E @? S P A C E\n!;" to "somespace",
                "éàôç..." to "eaoc",
            ).forEach { pair ->
                assertThat(pair).transform { fuzzyCompare(it.first, it.second) }.isTrue()
            }
        }
    }

    @Test
    fun `fuzzyCompare negative matches`() {
        assertAll {
            listOf(
                "Title One" to "Title Two",
                "mispeling" to "mispelling",
                "prefix suffix" to "prefix",
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

    @Test
    fun `remove diacritics strips only accents`() {
        listOf(
            "äà ç éè ûü öö" to "aa c ee uu oo",
            "lés- ?? ôt_ @ ` ^" to "les- ?? ot_ @ ` ^",
            " Untouched " to " Untouched ",
        ).forEach { pair ->
            assertThat(pair.first).transform { it.removeDiacritics() }.isEqualTo(pair.second)
        }
    }
}