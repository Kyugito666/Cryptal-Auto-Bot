import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.23"
    application
    kotlin("plugin.serialization") version "1.9.23"
}

group = "com.kyugito666.cryptalbot" // Diubah
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Coroutines for asynchronous programming
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // Ktor for HTTP requests
    implementation("io.ktor:ktor-client-core:2.3.10")
    implementation("io.ktor:ktor-client-cio:2.3.10")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.10")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.10")

    // Mordant for beautiful terminal output (colors, styles)
    implementation("com.github.ajalt.mordant:mordant:2.4.0")

    // Progress bar
    implementation("me.tongfei:progressbar:0.10.1")
}

application {
    mainClass.set("com.kyugito666.cryptalbot.MainKt") // Diubah
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

// Task to create a fat JAR that includes all dependencies
tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.kyugito666.cryptalbot.MainKt" // Diubah
    }
    configurations.getByName("runtimeClasspath").forEach { file ->
        from(zipTree(file.absoluteFile))
    }
}
