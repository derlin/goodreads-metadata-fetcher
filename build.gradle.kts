import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration

plugins {
    kotlin("jvm") version "1.4.32"
    id("org.jetbrains.dokka") version "1.5.0"
    `maven-publish`
}

buildscript {
    // This is to be able to configure the HTML Dokka plugin (custom styles, etc.), see `pluginConfiguration` below
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:1.5.0")
    }
}

group = "ch.derlin"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.32")
    implementation("org.jsoup:jsoup:1.14.1")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
    testImplementation("com.willowtreeapps.assertk:assertk:0.24")
    testImplementation("io.mockk:mockk:1.12.0")

    testImplementation("com.google.code.gson:gson:2.8.7") // TODO
}

tasks.test {
    useJUnitPlatform()
    outputs.upToDateWhen { false } // always run tests !
    testLogging {
        // get actual information about failed tests in the console
        showStackTraces = true
        showCauses = true
        showExceptions = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

// see https://kotlin.github.io/dokka/1.4.32/user_guide/gradle/usage/
tasks.dokkaHtml.configure {
    dokkaSourceSets {
        named("main") {
            // by changing the moduleName here, the heading in the markdown (README.md) must also be changed
            moduleName.set("GoodReads Metadata Fetcher")
            // since the readme has a level 1 heading matching "Module <moduleName>",
            // it will be considered as module doc and rendered in dokka
            includes.from("README.md")
            // show warnings on undocumented public members
            reportUndocumented.set(true)

            // when provided, Dokka generates "source" links for each declaration
            // see https://github.com/Kotlin/dokka/blob/master/examples/gradle/dokka-gradle-example/build.gradle.kts
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(URL("https://github.com/derlin/goodreads-metadata-fetcher/tree/main/src/main/kotlin"))
                remoteLineSuffix.set("#L") // this is specific to Github
            }

            // the samples can then be referenced using @sample package.methodName,
            // where "package" is actually the package defined in each file
            samples.from("${file("src/test/kotlin/samples/samples.kt")}")

            // configure the dokka HTML plugin, see https://kotlin.github.io/dokka/1.4.32/user_guide/gradle/usage/#applying-plugins
            // this syntax is only possible because "org.jetbrains.dokka:dokka-base" is referenced in the buildscript {}
            // without it, use:
            //pluginsMapConfiguration.set(
            //    mapOf(
            //        "org.jetbrains.dokka.base.DokkaBase" to """{
            //            "customStyleSheets" : ["${file("dokka/custom-styles.css")}"],
            //            "customAssets": ["${file("dokka/logo-icon.svg")}", "${file("dokka/logo.svg")}"],
            //            "footerMessage": "Made with love by Derlin"
            //            }"""
            //    )
            //)
            pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
                customAssets = listOf(file("dokka/logo-icon.svg"), file("dokka/logo.svg"))
                customStyleSheets = listOf(file("dokka/custom-styles.css"))
                footerMessage = "Made with ❤ by Derlin"
            }
        }
    }
}
