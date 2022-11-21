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
    fun `get ISBN from string`() {
        assertAll {
            listOf(
                "229014875X" to "229014875X",
                "1250145449" to "1250145449",
                "229014875X (ISBN13: 9782290148754)" to "9782290148754",
                "229014875X (ISBN112341234: 978229014875431290458125908)" to null,
                "" to null,
                "XX" to null,
            ).forEach { pair ->
                assertThat(pair.first).transform { getIsbnFromString(it) }.isEqualTo(pair.second)
            }
        }
    }

    @Test
    fun `get publication date from string`() {
        assertAll {
            listOf(
                "Published October 2nd 2020 (first published 1902)" to LocalDate.of(1902, 1, 1),
                "Published October 2nd 2003 (first published 2003)" to LocalDate.of(2003, 10, 2),
                "Published October 2nd 2003 by Derlin (first published 2003)" to LocalDate.of(2003, 10, 2),
                "Published August 30th 2006 by Gallimard (first published August 2006)" to LocalDate.of(2006, 8, 30),
                "Published 2005" to LocalDate.of(2005, 1, 1),
                "Published January 1997" to LocalDate.of(1997, 1, 1),
                "Published November 24th 1988 by Books On Tape" to LocalDate.of(1988, 11, 24),
                "Published June 2003 by XX <nobr class=\"greyText\">(first published 2003)</nobr>" to LocalDate.of(2003, 6, 1),
                "Published" to null,
                "" to null,
            ).forEach { pair ->
                assertThat(pair.first).transform { getPublicationDateFromString(it) }.isEqualTo(pair.second)
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
