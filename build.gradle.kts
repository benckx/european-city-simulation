plugins {
    val kotlinVersion = "2.2.20"
    kotlin("jvm") version kotlinVersion
    id("com.github.ben-manes.versions") version "0.52.0"
    id("com.adarshr.test-logger") version "4.0.0"
}

repositories {
    google()
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "1g"
}
