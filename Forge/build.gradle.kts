import hexdebug.utils.kotlinForgeRuntimeLibrary

plugins {
    id("hexdebug.conventions.platform")
    id("hexdebug.utils.kotlin-forge-runtime-library")
}

architectury {
    forge()
}

loom {
    forge {
        convertAccessWideners = true
        extraAccessWideners.add(loom.accessWidenerPath.get().asFile.name)

        mixinConfig("hexdebug-common.mixins.json", "hexdebug.mixins.json")
    }

    runs {
        register("commonDatagen") {
            data()
            programArgs(
                "--mod", hexdebugProperties.modId,
                "--all",
                // we're using forge to do the common datagen because fabric's datagen kind of sucks
                "--output", project(":Common").file("src/generated/resources").absolutePath,
                "--existing", file("src/main/resources").absolutePath,
                "--existing", project(":Common").file("src/main/resources").absolutePath,
            )
            property("hexdebug.apply-datagen-mixin", "true")
        }
    }
}

hexdebugPlatform {
    platform("forge")
}

hexdebugModDependencies {
    filesMatching.add("META-INF/mods.toml")

    anyVersion = ""
    mapVersions {
        replace(Regex("""\](\S+)"""), "($1")
        replace(Regex("""(\S+)\["""), "$1)")
    }

    requires("architectury-api")
    requires("cloth-config")
    requires(curseforge = "hexcasting", modrinth = "hex-casting")

    requires("kotlin-for-forge")
}

dependencies {
    forge(libs.forge)
    modApi(libs.architectury.forge)

    implementation(libs.kotlin.forge)

    modImplementation(libs.hexcasting.forge) { isTransitive = false }
    modImplementation(libs.paucal.forge)
    modImplementation(libs.patchouli.forge)
    modImplementation(libs.caelus)

    modApi(libs.clothConfig.forge)

    implementation(libs.mixinExtras)
    implementation(libs.bundles.lsp4j)
    implementation(libs.bundles.ktor)

    include(libs.mixinExtras)
    include(libs.bundles.lsp4j)
    include(libs.bundles.ktor)

    // GOD I HATE FORGE
    forgeRuntimeLibrary(libs.bundles.lsp4j)
    kotlinForgeRuntimeLibrary(libs.bundles.ktor)
}

tasks {
    shadowJar {
        exclude("fabric.mod.json")
    }

    named("runCommonDatagen") {
        doFirst {
            project(":Common").delete("src/generated/resources")
        }
    }
}

// SO SO SO SCUFFED.
val publishFile = publishMods.file.get().asFile
project(":Fabric").publishMods.github {
    additionalFiles.from(publishFile)
}

