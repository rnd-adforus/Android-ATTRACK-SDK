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

// Where kr.co.attrack:attrack-tracker is published for your organization.
// Leave unset when the artifact is in Maven Central or when you use app/libs.
val attrackMavenUrl: String? = providers.gradleProperty("attrack.mavenUrl").orNull

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        attrackMavenUrl?.takeIf { it.isNotBlank() }?.let { url -> maven(url) }
    }
}

rootProject.name = "attrack-android-sample"
include(":app")
