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
    optional("hexal")

    requires("kotlin-for-forge")
}

dependencies {
    forge(libs.forge)
    modApi(libs.architectury.forge)

    implementation(libs.kotlin.forge)

    modApi(libs.hexcasting.forge) { isTransitive = false }
    modImplementation(libs.paucal.forge)
    modLocalRuntime(libs.patchouli.forge)
    modLocalRuntime(libs.caelus)

    modApi(libs.hexal.forge)

    modApi(libs.clothConfig.forge)

    libs.mixinExtras.also {
        localRuntime(it)
        include(it)
    }

    libs.bundles.lsp4j.also {
        api(it)
        include(it)
        forgeRuntimeLibrary(it)
    }

    libs.bundles.ktor.also {
        implementation(it)
        include(it)
        kotlinForgeRuntimeLibrary(it)
    }
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

