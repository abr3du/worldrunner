enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

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

rootProject.name = "worldrunner"

include(
    ":app",
    ":core:model",
    ":core:data",
    ":core:designsystem",
    ":feature:home",
    ":feature:teams",
    ":feature:standings",
    ":feature:profile",
)
