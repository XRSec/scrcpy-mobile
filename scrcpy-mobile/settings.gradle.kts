pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.vanniktech.maven.publish") {
                useModule("com.vanniktech:gradle-maven-publish-plugin:0.25.3")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "scrcpy-mobile"
include(":app")
include(":dadb")
project(":dadb").projectDir = file("../external/dadb/dadb")
