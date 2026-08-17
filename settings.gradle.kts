pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.scaffoldit.dev/releases")
    }
}

rootProject.name = "Brutal Impacts (pre-release) v2.3.5"

plugins {
    // See documentation on https://scaffoldit.dev
    id("dev.scaffoldit") version "0.2.16"
}

// Would you like to do a split project?
// Create a folder named "common", then configure details with `common { }`

hytale {
    usePatchline("pre-release")
    useVersion("latest")

    repositories {
        // Any external repositories besides: MavenLocal, MavenCentral, HytaleMaven, and CurseMaven
    }

    dependencies {
        // Any external dependency you also want to include
    }

    manifest {
        Group = "Axelup"
        Name = "Brutal Impacts (pre-release)"
        Main = "dev.hytalemodding.BrutalImpacts"
    }
}
