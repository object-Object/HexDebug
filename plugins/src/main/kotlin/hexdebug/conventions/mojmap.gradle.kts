// augh
// https://github.com/emilyploszaj/emi/blob/0be856e06f84ccab659e3d1369ad03c899491281/xplat/mojmap/build.gradle

package hexdebug.conventions

import hexdebug.hexdebugProperties
import net.fabricmc.loom.LoomGradleExtension
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace
import net.fabricmc.loom.task.AbstractRemapJarTask
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RemapSourcesJarTask

plugins {
    id("hexdebug.conventions.architectury-base")
}

val parent = project.parent!!
evaluationDependsOn(parent.path)

val platform: String by project

base.archivesName = "${hexdebugProperties.modId}-$platform-mojmap"

dependencies {
    // the mojmap tasks fail if layered parchment mappings are used here
    mappings(loom.officialMojangMappings())
}

val mojmapJar by tasks.registering(RemapJarTask::class) {
    setupMojmapTask("remapJar")
}

val mojmapSourcesJar by tasks.registering(RemapSourcesJarTask::class) {
    setupMojmapTask("remapSourcesJar")
    archiveClassifier = "sources"
}

@Suppress("UnstableApiUsage")
private inline fun <reified T: AbstractRemapJarTask> T.setupMojmapTask(sourceTaskName: String) {
    val sourceTask = parent.tasks.named<T>(sourceTaskName)

    classpath.from((loom as LoomGradleExtension).getMinecraftJarsCollection(MappingsNamespace.INTERMEDIARY))

    dependsOn(sourceTask)

    inputFile = sourceTask.flatMap { it.archiveFile }

    sourceNamespace = "intermediary"
    targetNamespace = "named"

    remapperIsolation = true
}

tasks {
    build {
        dependsOn(mojmapJar, mojmapSourcesJar)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = base.archivesName.get()

            artifact(mojmapJar) {
                classifier = ""
            }

            artifact(mojmapSourcesJar) {
                classifier = "sources"
            }
        }
    }
}
