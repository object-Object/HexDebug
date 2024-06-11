package hexdebug.conventions

import hexdebug.hexdebugProperties

plugins {
    id("hexdebug.conventions.architectury")
    id("hexdebug.utils.mod-dependencies")

    id("com.github.johnrengelman.shadow")
    id("me.modmuss50.mod-publish-plugin")
}

val platform: String by project
val platformCapitalized = platform.capitalize()

architectury {
    platformSetupLoomIde()
}

loom {
    runs {
        named("server") {
            runDir = "runServer"
        }
    }
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
    // this needs to wait until Loom has been configured
    afterEvaluate {
        named("development$platformCapitalized") {
            extendsFrom(get("common"))
        }
    }
}

dependencies {
    "common"(project(":Common", "namedElements")) { isTransitive = false }
    "shadowCommon"(project(":Common", "transformProduction$platformCapitalized")) { isTransitive = false }
}

// FIXME: find a less broken way to include common resources in platform devenv - this one breaks mixin refmaps
//sourceSets {
//    main {
//        resources {
//            source(project(":Common").sourceSets.main.get().resources)
//        }
//    }
//}

tasks {
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

fun Project.envOrEmpty(name: String) = this.providers.environmentVariable(name).orElse("")

publishMods {
    dryRun = providers.zip(envOrEmpty("CI"), envOrEmpty("DRY_RUN")) { ci, dryRun ->
        ci.isBlank() || dryRun.isNotBlank()
    }

    type = BETA
    changelog = hexdebugProperties.getLatestChangelog()
    file = tasks.remapJar.flatMap { it.archiveFile }

    modLoaders.add(platform)

    displayName = modLoaders.map { values ->
        val loaders = values.joinToString(", ") { it.capitalize() }
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

fun String.capitalize() = replaceFirstChar(Char::uppercase)
