plugins {
    kotlin("jvm")
    application
}


group = "org.xBaank"
version = "1.0-SNAPSHOT"

val ktor_version: String by project

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.xBaank.simpleJson:core:9.0.0")
    //arrow
    implementation("io.arrow-kt:arrow-core:1.1.5")

    implementation("io.ktor:ktor-network:$ktor_version")
    implementation("io.ktor:ktor-network-tls:$ktor_version")
    //coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    implementation(project(":RtmpClient"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}