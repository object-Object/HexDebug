package hexdebug.conventions

import gradle.kotlin.dsl.accessors._a774b807f82c23abb447cf166178410f.architectury
import gradle.kotlin.dsl.accessors._a774b807f82c23abb447cf166178410f.processIncludeJars
import hexdebug.hexdebugProperties

plugins {
    id("hexdebug.conventions.platform-base")
    id("hexdebug.utils.publish-dependencies")
}

val platform: String by project

architectury {
    platformSetupLoomIde()
}

dependencies {
    // include, not shadow
    localRuntime(project(":hexdebug-core-common", "namedElements"))
    project(":hexdebug-core-$platform", "namedElements").also {
        localRuntime(it)
        api(it)
    }
    include(project(":hexdebug-core-$platform"))
}

tasks {
    processIncludeJars {

    }
}

publishMods {
    dryRun = providers.zip(envOrEmpty("CI"), envOrEmpty("DRY_RUN")) { ci, dryRun ->
        ci.isBlank() || dryRun.isNotBlank()
    }

    type = BETA
    changelog = hexdebugProperties.getLatestChangelog()
    file = tasks.remapJar.flatMap { it.archiveFile }

    modLoaders.add(platform)

    displayName = modLoaders.map { values ->
        val loaders = values.joinToString(", ") { it.replaceFirstChar(Char::uppercase) }
        "v${project.version} [$loaders]"
    }

    curseforge {
        accessToken = envOrEmpty("CURSEFORGE_TOKEN")
        projectId = hexdebugProperties.curseforgeId
        minecraftVersions.add(hexdebugProperties.minecraftVersion)
        clientRequired = true
        serverRequired = true
    }

    modrinth {
        accessToken = envOrEmpty("MODRINTH_TOKEN")
        projectId = hexdebugProperties.modrinthId
        minecraftVersions.add(hexdebugProperties.minecraftVersion)
    }
}

fun Project.envOrEmpty(name: String) = this.providers.environmentVariable(name).orElse("")
