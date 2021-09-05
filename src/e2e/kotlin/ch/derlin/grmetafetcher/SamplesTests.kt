package ch.derlin.grmetafetcher

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isSuccess
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import samples.findBookAutomatically
import samples.findBookInteractively
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
        url = "https://www.goodreads.com/book/show/40961427-1984",
        id = "40961427",
        isbn = null,
        pages = 298,
        pubDate = LocalDate.parse("1949-04-08"),
    )

    private val freakonomics = GoodReadsMetadata(
        title = "Freakonomics: A Rogue Economist Explores the Hidden Side of Everything",
        authors = listOf("Steven D. Levitt", "Stephen J. Dubner"),
        url = "https://www.goodreads.com/book/show/1202.Freakonomics",
        id = "1202",
        isbn = "9780061234002",
        pages = 268,
        pubDate = LocalDate.parse("2005-04-12"),
    )

    @BeforeEach
    fun cleanup() {
        outStream.reset()
    }

    @Test
    fun `samples findBookAutomatically works`() {
        assertThat { findBookAutomatically() }.isSuccess()
        assertOutputContains(freakonomics.toCompilableString())
    }

    @Test
    fun `samples findBookInteractively works`() {
        assertRunInteractiveWorks("1984")
        assertOutputContains(
            "[0] 1984 by George Orwell",
            orwell1984.toCompilableString()
        )
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