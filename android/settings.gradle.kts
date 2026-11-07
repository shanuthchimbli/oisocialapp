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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Oi"

// Application Shell
include(":app")

// Core Foundation Modules
include(":core:domain")
include(":core:database")
include(":core:crypto")
include(":core:network")

// Feature Modules
include(":feature:auth")
include(":feature:chat")
include(":feature:status")
