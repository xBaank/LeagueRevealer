rootProject.name = "LeagueRtmp"
include(":LeagueClient", ":Proxy", ":RtmpClient")
pluginManagement {
    val kotlin_version: String by settings
    plugins {
        kotlin("jvm") version kotlin_version
    }
}
