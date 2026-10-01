pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// ATTRACK Maven repository URL supplied with your integration details.
val repositoryProperties = java.util.Properties().apply {
    val file = settingsDir.resolve("local.properties")
    if (file.isFile) file.inputStream().use(::load)
}
val attrackMavenUrl = providers.gradleProperty("attrack.mavenUrl").orNull
    ?: repositoryProperties.getProperty("attrack.mavenUrl")
    ?: System.getenv("ATTRACK_MAVEN_URL")
    ?: "https://nexus.adforus.com/repository/attrack/"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(attrackMavenUrl)
    }
}

rootProject.name = "attrack-android-sample"
include(":app")
