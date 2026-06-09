// Tells Gradle WHERE to download plugins and libraries from.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// The name of the whole project, and the single app module it contains.
rootProject.name = "Saavdhan"
include(":app")

// Harmless detection-target fixture used to test the watchdog (see decoyapp/build.gradle.kts).
include(":decoyapp")
