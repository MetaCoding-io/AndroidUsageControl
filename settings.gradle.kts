pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolution {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AndroidUsageControl"

include(":app")

// Core modules
include(":core:core-common")
include(":core:core-model")
include(":core:core-database")
include(":core:core-rules")
include(":core:core-ui")

// Feature modules
include(":feature:feature-notifications")
include(":feature:feature-learning")
include(":feature:feature-screentime")
include(":feature:feature-lockcontrol")
include(":feature:feature-settings")

// Service modules
include(":service:service-notification")
include(":service:service-usage")
include(":service:service-context")
