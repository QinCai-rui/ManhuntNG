pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net")
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

rootProject.name = "ManhuntNG"

include("common", "paper", "fabric")
