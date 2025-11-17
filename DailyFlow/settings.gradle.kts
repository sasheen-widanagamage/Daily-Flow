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
        // Needed for MPAndroidChart dependency
        maven(url = "https://jitpack.io")
    }
}
rootProject.name = "DailyFlow"
include(":app")
