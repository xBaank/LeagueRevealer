rootProject.name = "LeagueRtmp"
include(":Client", ":Proxy")
pluginManagement {
    val kotlin_version: String by settings
    plugins {
        kotlin("jvm") version kotlin_version
    }
}
