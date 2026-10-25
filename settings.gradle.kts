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

rootProject.name = "Reverb"

include(":apps:android")
include(":core:domain")
include(":core:model")
include(":core:prompt")
include(":core:network")
include(":core:designsystem")
include(":feature:onboarding")
