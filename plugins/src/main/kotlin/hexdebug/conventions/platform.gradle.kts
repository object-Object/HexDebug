package hexdebug.conventions

import hexdebug.hexdebugProperties

// plugin config

abstract class HexDebugPlatformExtension(private val project: Project) {
    abstract val developmentConfiguration: Property<String>
    abstract val shadowCommonConfiguration: Property<String>

    private val hexdebugArchitectury by lazy {
        project.extensions.getByType<IHexDebugArchitecturyExtension>()
    }

    fun platform(platform: String, vararg extraModLoaders: String) = project.run {
        // "inheritance"
        hexdebugArchitectury.platform(platform)

        platform.replaceFirstChar(Char::uppercase).also {
            developmentConfiguration.convention("development$it")
            shadowCommonConfiguration.convention("transformProduction$it")
        }

        configurations {
            named(developmentConfiguration.get()) {
                extendsFrom(get("common"))
            }
        }

        dependencies {
            "shadowCommon"(project(":Common", shadowCommonConfiguration.get())) { isTransitive = false }
        }

        publishMods {
            modLoaders.addAll(platform, *extraModLoaders)
            displayName = modLoaders.map { values ->
                val loaders = values.joinToString(", ") { it.replaceFirstChar(Char::uppercase) }
                "v${project.version} [$loaders]"
            }
        }
    }
}

val extension = extensions.create<HexDebugPlatformExtension>("hexdebugPlatform")

// build logic

plugins {
    id("hexdebug.conventions.architectury")
    id("hexdebug.utils.mod-dependencies")

    id("com.github.johnrengelman.shadow")
    id("me.modmuss50.mod-publish-plugin")
}

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

sourceSets {
    main {
        resources {
            source(project(":Common").sourceSets.main.get().resources)
        }
    }
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
    dryRun = providers.environmentVariable("CI").orElse("").map { it.isBlank() }

    type = ALPHA
    changelog = hexdebugProperties.getLatestChangelog()
    file = tasks.remapJar.flatMap { it.archiveFile }

    curseforge {
        projectId = hexdebugProperties.curseforgeId
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN").orElse("")

        minecraftVersions.add(hexdebugProperties.minecraftVersion)
    }

    modrinth {
        projectId = hexdebugProperties.modrinthId
        accessToken = providers.environmentVariable("MODRINTH_TOKEN").orElse("")

        minecraftVersions.add(hexdebugProperties.minecraftVersion)
    }
}
