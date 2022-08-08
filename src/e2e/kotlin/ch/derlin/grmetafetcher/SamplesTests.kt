package ch.derlin.grmetafetcher

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isSuccess
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import samples.findBookAutomatically
import samples.findBookInteractively
import samples.searchGoodReadsPaginated
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.Charset
import java.time.LocalDate

class SamplesTests {

    private val outStream: ByteArrayOutputStream = ByteArrayOutputStream().also { System.setOut(PrintStream(it)) }

    private val orwell1984 = GoodReadsMetadata(
        title = "1984",
        authors = listOf("George Orwell"),
        url = "https://www.goodreads.com/book/show/61439040-1984",
        id = "61439040",
        isbn = "9780452284234",
        pages = null,
        pubDate = LocalDate.parse("1949-04-08"),
    )

    private val mastersOfDoom = GoodReadsMetadata(
        title = "Masters of Doom: How Two Guys Created an Empire and Transformed Pop Culture",
        authors = listOf("David Kushner"),
        url = "https://www.goodreads.com/book/show/222146.Masters_of_Doom",
        id = "222146",
        isbn = "9780812972153",
        pages = 339,
        pubDate = LocalDate.parse("2003-01-01"),
    )

    @BeforeEach
    fun cleanup() {
        outStream.reset()
    }

    @Test
    fun `samples findBookAutomatically works`() {
        assertThat { findBookAutomatically() }.isSuccess()
        assertOutputContains(mastersOfDoom.toCompilableString())
    }

    @Test
    fun `samples findBookInteractively works`() {
        assertRunInteractiveWorks("1984")
        assertOutputContains(
            "[0] 1984 by George Orwell",
            orwell1984.toCompilableString()
        )
    }

    @Test
    fun `samples searchGoodReadsPaginated works`() {
        assertThat { searchGoodReadsPaginated() }.isSuccess()
        assertOutputContains(*((1..3).map { "page [$it]" }.toTypedArray()))
    }

    private fun assertRunInteractiveWorks(title: String, author: String? = null, index: Int = 0) {
        val oldStdin = System.`in`
        System.setIn(ByteArrayInputStream("$title\n${author ?: ""}\n$index".toByteArray()))
        try {
            assertThat { findBookInteractively() }.isSuccess()
        } finally {
            System.setIn(oldStdin)
        }
    }

    private fun assertOutputContains(vararg messages: String) {
        assertThat(outStream.toString(Charset.forName("utf-8"))).contains(*messages)
    }
}
