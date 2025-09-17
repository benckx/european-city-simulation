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
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "1g"
}
