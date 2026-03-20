/**
 * NOTE: This is entirely optional and basics can be done in `settings.gradle.kts`
 */

import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.api.file.DuplicatesStrategy

repositories {
    // Any external repositories besides: MavenLocal, MavenCentral, HytaleMaven, and CurseMaven
}

dependencies {
    // Any external dependency you also want to include
}

// Include the local Hytale asset pack in the produced JAR so the server can load it from the mod.
// Note: We only add it to ProcessResources; adding it again to `jar { from("assets") ... }`
// will duplicate entries because the JAR already packages `build/resources/main`.
tasks.named<ProcessResources>("processResources") {
    from("assets") {
        // Put the asset pack at the root of the JAR: Server/, Common/, etc. (no `assets/` prefix)
        into("")
    }

    // In case other tooling also contributes the same files, avoid failing the build.
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
