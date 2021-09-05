import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import java.net.URL

group = Config.groupId
version = Config.version

buildscript {
    // Import the scripts defining the `DokkaBaseConfiguration` class and the like.
    // This is to be able to configure the HTML Dokka plugin (custom styles, etc.)
    // Note: this can't be put in buildSrc unfortunately
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:${Dependencies.dokka}")
    }
}

plugins {
    Dependencies.registerPlugins(this)
}

repositories {
    mavenCentral()
}

dependencies {
    Dependencies.registerAllDependencies(this)
}

project.configureJUnit()
project.configureE2eTests()

/* ========================================
 * publishing
 * ========================================= */

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

/* ========================================
 * documentation (dokka)
 * ========================================= */

// ./gradlew dokkaHtml
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
            samples.from("${file("src/e2e/kotlin/samples/samples.kt")}")

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