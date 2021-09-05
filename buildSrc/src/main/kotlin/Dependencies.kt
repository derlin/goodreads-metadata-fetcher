import org.gradle.kotlin.dsl.*
import org.gradle.plugin.use.PluginDependenciesSpec

// see https://proandroiddev.com/avoid-repetitive-dependency-declarations-with-gradle-kotlin-dsl-97c904704727
// a nice alternative is presented here: https://medium.com/swlh/gradles-kotlin-dsl-buildsrc-4434100a07d7
object Dependencies {
    // base versions
    const val kotlin = "1.5.0"
    const val dokka = "1.5.0"

    // main versions
    private const val jsoup = "1.14.1"

    // test versions
    private const val junit = "5.6.0"
    private const val assertk = "0.24"
    private const val mockk = "1.12.0"

    val plugins = listOf(
        "org.jetbrains.kotlin.jvm" to kotlin,
        "org.jetbrains.dokka" to dokka,
        "maven-publish" to null
    )

    private val mainDependencies = listOf(
        "org.jetbrains.kotlin:kotlin-reflect",
        "org.jsoup:jsoup:$jsoup"
    )

    private val testPlatforms = listOf(
        "org.junit:junit-bom:$junit"
    )

    private val testDependencies = listOf(
        "org.junit.jupiter:junit-jupiter",
        "com.google.code.gson:gson:2.8.7", // TODO
        "com.willowtreeapps.assertk:assertk:$assertk",
        "io.mockk:mockk:$mockk"
    )

    fun registerPlugins(scope: PluginDependenciesSpec) {
        plugins.forEach { (name, version) -> version?.let { scope.id(name) version version } ?: scope.id(name) }
    }

    fun registerAllDependencies(scope: DependencyHandlerScope) {
        mainDependencies.forEach { scope.add("implementation", it) }
        testPlatforms.forEach { scope.add("testImplementation", scope.platform(it)) }
        testDependencies.forEach { scope.add("testImplementation", it) }
    }
}