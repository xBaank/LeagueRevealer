plugins {
    kotlin("jvm")
    application
}

group = "org.xBaank"
version = "1.0-SNAPSHOT"

val ktor_version: String by project

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-network:$ktor_version")
    implementation("io.ktor:ktor-network-tls:$ktor_version")
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