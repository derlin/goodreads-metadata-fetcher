import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
    `maven-publish`
}

group = "ch.derlin"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
    testImplementation("com.willowtreeapps.assertk:assertk:0.24")
    testImplementation("io.mockk:mockk:1.12.0")

    implementation("org.jsoup:jsoup:1.14.1")
    implementation("com.google.code.gson:gson:2.8.7")
}

tasks.test {
    useJUnitPlatform()
    outputs.upToDateWhen { false }
    testLogging {
        showStackTraces = true
        // get actual information about failed tests in the console
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