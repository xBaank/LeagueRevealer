import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    implementation("io.arrow-kt:suspendapp:0.4.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

//fat jar
tasks {
    val fatJar = register<Jar>("fatJar") {
        dependsOn.addAll(
            listOf(
                "compileJava",
                "compileKotlin",
                "processResources"
            )
        ) // We need this for Gradle optimization to work
        archiveClassifier.set("standalone") // Naming the jar
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest { attributes(mapOf("Main-Class" to application.mainClass)) } // Provided we set it up in the application plugin configuration
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) } +
                sourcesMain.output
        from(contents)
    }
    build {
        dependsOn(fatJar) // Trigger fat jar creation during build
    }
}

application {
    mainClass.set("MainKt")
}