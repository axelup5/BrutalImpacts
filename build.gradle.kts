/**
 * NOTE: This is entirely optional and basics can be done in `settings.gradle.kts`
 */

import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar

repositories {
    // Any external repositories besides: MavenLocal, MavenCentral, HytaleMaven, and CurseMaven
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
}

// Keep API migrations visible as Hytale's pre-release server evolves.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:deprecation")
    options.compilerArgs.add("-Xlint:removal")
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

// Keep publishable JARs outside build/, because `clean` deletes that directory.
// Rebuilding the same version overwrites its archived JAR; changing the version
// produces a different filename and therefore preserves previous releases.
val jarTask = tasks.named<Jar>("jar")
val releaseDirectory = layout.projectDirectory.dir("releases")

val archiveRelease by tasks.registering {
    group = "build"
    description = "Copies the versioned mod JAR to the persistent releases directory."
    dependsOn(jarTask)

    val sourceJar = jarTask.flatMap { it.archiveFile }
    val archivedJar = sourceJar.map { releaseDirectory.file(it.asFile.name) }

    inputs.file(sourceJar)
    outputs.file(archivedJar)

    doLast {
        val source = sourceJar.get().asFile
        val destination = archivedJar.get().asFile
        destination.parentFile.mkdirs()
        source.copyTo(destination, overwrite = true)
        logger.lifecycle("Archived release: ${destination.absolutePath}")
    }
}

tasks.named("build") {
    dependsOn(archiveRelease)
}
