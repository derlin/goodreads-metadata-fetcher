package ch.derlin.grmetafetcher

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Specify how retries should be performed.
 * See [Retry].
 */
data class RetryConfiguration(
    /** Maximum attempts before raising an exception. */
    val maxRetries: Int,
    /** Time to wait between attempts. */
    val interval: Long,
    /** By how much to increase the interval between each attempts (exponential backoff). */
    val multiplier: Float = 1f,
) {
    init {
        require(maxRetries > 0 && interval > 0 && multiplier > 0) {
            "maxRetries, interval and multiplier must be positive"
        }
    }

    /**
     * Generate a finite sequence of intervals from the configuration.
     */
    fun retryIntervals() = generateSequence(0) { retry -> (retry + 1).takeIf { it < maxRetries } }
        .drop(1) // drop the first, since runningFold always returns the initial acc value first
        .runningFold(interval) { acc, _ -> (acc * multiplier).toLong() }

    /** Default configurations. */
    companion object {
        /**
         * Retry 5 times, waiting 2s between each attempt.
         */
        val SIMPLE = RetryConfiguration(maxRetries = 5, interval = 2_000, multiplier = 1f)

        /**
         * Retry 5 times, waiting 500ms, 1s, 2s, 4s, 8s (x2 multiplier).
         */
        val EXPONENTIAL = RetryConfiguration(maxRetries = 5, interval = 500, multiplier = 2f)
    }
}

/**
 * Instantiate a "retrier", than can be used to wrap any call to GoodReads.
 * GoodReads tends to return 5XX errors codes sporadically, so this is useful when automating tasks.
 *
 * @sample samples.lookupWithRetry
 */
class Retry(
    /** Configure the max retries, intervals, etc. */
    val retryConfiguration: RetryConfiguration,
    /**
     * Lambda called to determine if the exception should be retried. Retry on error code 500 from GoodReads by default
     * (see [Retry.Companion.retryIfServerFault].
     */
    val retryWhen: (Throwable) -> Boolean = ::retryIfServerFault,
) {

    /** Run some code, and retry if a "retriable" exception is raised (see [Retry.retryWhen]). */
    @OptIn(ExperimentalContracts::class)
    fun <T> run(body: () -> T): T {
        contract {
            callsInPlace(body, InvocationKind.AT_LEAST_ONCE)
        }

        var lastError: Throwable? = null

        retryConfiguration.retryIntervals().forEach { sleepInterval ->
            try {
                return body()
            } catch (e: Throwable) {
                if (!retryWhen(e)) throw e
                lastError = e
                //println("Exception ${e.message} caught. Trying again in ${sleepInterval}ms.")
                Thread.sleep(sleepInterval)
            }
        }

        throw requireNotNull(lastError)
    }

    companion object {
        /**
         * Returns true only when the exception comes from an HTTP 5XX. Used as the default for the [Retry.retryWhen] lambda.
         */
        fun retryIfServerFault(throwable: Throwable): Boolean {
            val maybeStatusCode = throwable.message
                ?.let { "Server returned HTTP response code: (\\d+)".toRegex().find(it) }
                ?.groupValues?.get(1)?.toIntOrNull() ?: 0
            return maybeStatusCode in 500..599
        }
    }
}
