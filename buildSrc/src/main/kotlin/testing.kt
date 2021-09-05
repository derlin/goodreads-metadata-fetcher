import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.kotlin.dsl.*

fun Project.configureJUnit() {
    tasks.named("test", Test::class) { configureJUnit() }
}

fun Project.configureE2eTests() {

    configure<SourceSetContainer> {
        val main by getting
        create("e2e") {
            compileClasspath += main.output
            runtimeClasspath += main.output
        }
    }

    val e2eImplementation by configurations.getting { // should be called "<sourceSet>Implementation"
        extendsFrom(configurations["implementation"])
        extendsFrom(configurations["testImplementation"])
    }

    val e2eTestTask = tasks.register<Test>("e2eTest") {
        description = "Runs e2e tests."
        group = "verification"
        useJUnitPlatform()

        val e2e = project.the<SourceSetContainer>()["e2e"]
        testClassesDirs = e2e.output.classesDirs
        classpath = e2e.runtimeClasspath

        shouldRunAfter(tasks.getByName("test"))
        configureJUnit()
    }

    tasks.getByName("check") { dependsOn(e2eTestTask) }
}


private fun Test.configureJUnit() {
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
