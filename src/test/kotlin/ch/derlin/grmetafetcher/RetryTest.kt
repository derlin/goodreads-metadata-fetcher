package ch.derlin.grmetafetcher

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import org.junit.jupiter.api.Test
import java.io.IOException

class RetryTest {

    @Test
    fun `retry only on HTTP 500`() {
        mapOf(
            true to IOException("Server returned HTTP response code: 504 for URL: https://www.goodreads.com/book/show/41940388-the-test"),
            true to RuntimeException("Server returned HTTP response code: 522"),
            false to RuntimeException("Server returned HTTP response code: 302"),
            false to IOException("Another message"),
            false to RuntimeException()
        ).forEach { (expected, exception) ->
            assertThat(exception)
                .transform { Retry.retryIfServerFault(it) }
                .isEqualTo(expected)
        }
    }

    @Test
    fun `retry respects max attempts`() {
        var attempts = 0
        val exception = RuntimeException("Server returned HTTP response code: 522")

        assertThat {
            Retry(RetryConfiguration(maxRetries = 3, interval = 1)).run {
                attempts += 1
                throw exception
            }
        }.isFailure().hasMessage(exception.message)

        assertThat(attempts).isEqualTo(3)
    }

    @Test
    fun `retry stops on non-retriable exceptions`() {
        var attempts = 0
        val exceptions = listOf(
            RuntimeException("Server returned HTTP response code: 522"),
            java.lang.RuntimeException("do not retry")
        )
        assertThat {
            Retry(RetryConfiguration(maxRetries = 10, interval = 1)).run {
                attempts += 1
                throw exceptions[attempts - 1]
            }
        }.isFailure().hasMessage(exceptions.last().message)

        assertThat(attempts).isEqualTo(exceptions.size)
    }

    @Test
    fun `retry stops on success`() {
        var attempts = 0

        assertThat(
            Retry(RetryConfiguration(maxRetries = 10, interval = 1)).run {
                attempts += 1
                if (attempts == 1) throw RuntimeException("Server returned HTTP response code: 522")
                "OK"
            }
        ).isEqualTo("OK")

        assertThat(attempts).isEqualTo(2)
    }
}
