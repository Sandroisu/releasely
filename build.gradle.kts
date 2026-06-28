plugins {
    kotlin("jvm") version "2.1.10"
    application
}

group = "io.github.sandroisu"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:5.0.3")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("io.github.sandroisu.releasely.MainKt")
}