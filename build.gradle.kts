plugins {
    application
    kotlin("jvm") version "1.9.21"
}

group = "n3p"
version = "1.0-SNAPSHOT"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-RC2")
    implementation("com.fazecast:jSerialComm:2.10.4")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("n3p.MainKt")
}
