package hexdebug.conventions

import hexdebug.hexdebugProperties

// plugin config

abstract class HexDebugPlatformExtension(private val project: Project) {
    fun shadowCommonConfiguration(configuration: String) = project.run {
        dependencies {
            "shadowCommon"(project(":Common", configuration)) { isTransitive = false }
        }
    }
}

val extension = extensions.create<HexDebugPlatformExtension>("hexdebugPlatform")

// build logic

plugins {
    id("hexdebug.conventions.architectury")
    id("hexdebug.utils.mod-dependency-versions")

    id("com.github.johnrengelman.shadow")
    id("me.modmuss50.mod-publish-plugin")
}

val hexdebugArchitectury = extensions.getByType<IHexDebugArchitecturyExtension>()

architectury {
    platformSetupLoomIde()
}

configurations {
    register("common")
    register("shadowCommon")
    compileClasspath {
        extendsFrom(get("common"))
    }
    runtimeClasspath {
        extendsFrom(get("common"))
    }
}

dependencies {
    "common"(project(":Common", "namedElements")) { isTransitive = false }
}

tasks {
    processResources {
        from(project(":Common").file("src/main/resources")) {
            include("data/*/patchouli_books/")
        }
    }

    shadowJar {
        exclude("architectury.common.json")
        configurations = listOf(project.configurations["shadowCommon"])
        archiveClassifier = "dev-shadow"
    }

    remapJar {
        dependsOn(shadowJar)
        inputFile = shadowJar.get().archiveFile
        archiveClassifier = null
    }

    jar {
        archiveClassifier = "dev"
    }

    kotlinSourcesJar {
        val commonSources = project(":Common").tasks.kotlinSourcesJar
        dependsOn(commonSources)
        from(commonSources.flatMap { it.archiveFile }.map(::zipTree))
    }
}

components {
    named<AdhocComponentWithVariants>("java") {
        withVariantsFromConfiguration(configurations.shadowRuntimeElements.get()) {
            skip()
        }
    }
}

publishMods {
    file = tasks.remapJar.flatMap { it.archiveFile }
    additionalFiles.from(tasks.kotlinSourcesJar.flatMap { it.archiveFile })
    changelog = "" // TODO
    type = ALPHA

    displayName = hexdebugArchitectury.platform.map { platform ->
        "HexDebug ${hexdebugProperties.modVersion} [$platform]"
    }
    version = hexdebugProperties.modVersion

//    curseforge {
//        accessToken = project.curseforgeApiToken
//        projectId = project.curseforgeID
//        minecraftVersions.add(minecraftVersion)
//
//        requires {
//            slug = "architectury-api"
//        }
//        requires {
//            slug = "kotlin-for-forge"
//        }
//        requires {
//            slug = "hexcasting"
//        }
//    }
//
//    modrinth {
//        accessToken = project.modrinthApiToken
//        projectId = project.modrinthID
//        minecraftVersions.add(minecraftVersion)
//
//        requires {
//            slug = "architectury-api"
//        }
//        requires {
//            slug = "kotlin-for-forge"
//        }
//        requires {
//            slug = "hex-casting"
//        }
//    }
}
