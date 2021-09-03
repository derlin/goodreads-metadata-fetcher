import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

object Config {
    const val groupId = "ch.derlin"
    const val version = "1.0.0-SNAPSHOT"
}

fun NamedDomainObjectProvider<Test>.configureJUnit() = configure {
    useJUnitPlatform()
    outputs.upToDateWhen { false } // always run tests !
    testLogging {
        // get actual information about failed tests in the console
        // should be used inside
        showStackTraces = true
        showCauses = true
        showExceptions = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}