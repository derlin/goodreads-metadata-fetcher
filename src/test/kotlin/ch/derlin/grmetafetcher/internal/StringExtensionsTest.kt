package ch.derlin.grmetafetcher.internal

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class StringExtensionsTest {

    @Test
    fun `remove diacritics`() {
        String::removeDiacritics.givenFirstReturnsSecond(
            "äà ç éè ûü öö" to "aa c ee uu oo",
            "lés- ?? ôt_ @ ` ^" to "les- ?? ot_ @ ` ^",
            " Some title " to " Some title ",
            "L'Œuvre de Dieu, la part du Dœable" to "L'Oeuvre de Dieu, la part du Doeable",
        )
    }

    @Test
    fun `remove initials`() {
        String::removeInitials.givenFirstReturnsSecond(
            "J. Abrams" to " Abrams",
            "JJ. Abrams" to " Abrams",
            "Abrams J." to "Abrams ",
            "Abrams JJ." to "Abrams ",
            "Sigmund K. Freud` ^" to "Sigmund  Freud` ^",
            "This I S A UNT. OUCHED. strin g." to "This I S A UNT. OUCHED. strin g.",
            "K. X. AA. T. SS." to "    ",
            "-K.-Freud-JJ.-" to "--Freud--",
        )
    }

    @Test
    fun `remove content in parentheses`() {
        String::removeContentInParentheses.givenFirstReturnsSecond(
            "This is a title" to "This is a title",
            "Waking Gods (Themis Files, #2)" to "Waking Gods ",
            "Waking Chaos (Paldimori Gods Rising #1) X" to "Waking Chaos  X",
        )
    }

    @Test
    fun `remove special characters`() {
        String::replaceSpecialChars.givenFirstReturnsSecond(
            "Awakening:stuff's & tips" to "Awakening stuff's and tips",
            "20,000 Leagues under the sea" to "20000 Leagues under the sea",
        )
    }

    @Test
    fun `clean title for GoodReads search`() {
        ::cleanTitleForSearchQuery.givenFirstReturnsSecond(
            "é TïtlŒ (First of series #1)  " to "é tïtlœ",
            "Lilo &   Stitch " to "lilo and stitch",
            "one:under_score" to "one under_score",
            "1,000 treasures" to "1000 treasures",
        )
    }

    @Test
    fun `clean author for GoodReads search`() {
        ::cleanAuthorForSearchQuery.givenFirstReturnsSecond(
            "Irvin D. Yalom & Ursula K. LeGuin" to "Irvin Yalom Ursula LeGuin",
            // TODO "J.K. Jung, Author Two" to "J. Jung Author Two",
        )
    }
}

private fun <I, O> ((I) -> O).givenFirstReturnsSecond(vararg inputToExpected: Pair<I, O>) {
    assertAll {
        inputToExpected.forEach { pair ->
            assertThat(pair.first).transform { this(it) }.isEqualTo(pair.second)
        }
    }
}