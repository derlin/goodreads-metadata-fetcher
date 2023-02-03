package ch.derlin.grmetafetcher.internal

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ParsingTest {


    @Test
    fun `get Authors from string`() {
        assertAll {
            listOf(
                "by José Carlos Somoza, Marianne Millon (Translator)" to listOf("José Carlos Somoza"),
                "by Audrey Niffenegger (GoodReads Author)" to listOf("Audrey Niffenegger"),
                "by Sylvain Neuvel, Georges Orwell, Max Pix" to listOf("Sylvain Neuvel", "Georges Orwell", "Max Pix"),
                "by Sylvain Neuvel (GoodReads Author), Georges Orwell, Max Pix (Illustrator)" to listOf("Sylvain Neuvel", "Georges Orwell"),
                "Hervé Girardô" to listOf("Hervé Girardô"),
                "Harlan Coben (Goodreads Author) (Goodreads Author)" to listOf("Harlan Coben"),
                "" to listOf(),
            ).forEach { pair ->
                assertThat(pair.first).transform { getAuthorsFromString(it) }.isEqualTo(pair.second)
            }
        }
    }

    @Test
    fun `get publisher from string`() {
        assertAll {
            listOf(
                "Published October 2nd 2020 (first published 1902)" to null,
                "Published August 30th 2006 by Gallimard (first published August 2006)" to "Gallimard",
                "Published 2005 by A Very Long Publisher 01" to "A Very Long Publisher 01",
                "Published by X Y Z (first published 1493)" to "X Y Z",
                "Published" to null,
                "" to null,
            ).forEach { pair ->
                assertThat(pair.first).transform { getPublisherFromString(it) }.isEqualTo(pair.second)
            }
        }
    }
}
