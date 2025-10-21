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

        mixinConfig(
            "hexdebug-common.mixins.json",
        )
    }

    runs {
        for ((name, outputProject) in arrayOf(
            // we're using forge to do the common datagen because fabric's datagen kind of sucks
            "common" to project(":Common"),
            "forge" to project,
        )) {
            register("${name}Datagen") {
                data()
                programArgs(
                    "--mod", hexdebugProperties.modId,
                    "--all",
                    "--output", outputProject.file("src/generated/resources").absolutePath,
                    "--existing", file("src/main/resources").absolutePath,
                    "--existing", project(":Common").file("src/main/resources").absolutePath,
                    "--existing-mod", "hexcasting",
                )
                property("hexdebug.apply-datagen-mixin", "true")
                property("hexdebug.$name-datagen", "true")
            }
        }
    }
}

hexdebugModDependencies {
    filesMatching.add("META-INF/mods.toml")

    anyVersion = ""
    mapVersions {
        replace(Regex("""\](\S+)"""), "($1")
        replace(Regex("""(\S+)\["""), "$1)")
    }
}

hexdebugPublishDependencies {
    requires("architectury-api")
    requires("cloth-config")
    requires(curseforge = "hexcasting", modrinth = "hex-casting")
    requires("ioticblocks")

    requires("kotlin-for-forge")

    optional("emi")
}

dependencies {
    forge(libs.forge)
    modApi(libs.architectury.forge)

    implementation(libs.kotlin.forge)

    modApi(libs.hexcasting.forge) { isTransitive = false }
    modImplementation(libs.paucal.forge)
    modLocalRuntime(libs.patchouli.forge)
    modLocalRuntime(libs.caelus)
    modLocalRuntime(libs.inline.forge) { isTransitive = false }

    modApi(libs.clothConfig.forge)

    libs.mixinExtras.common.also {
        compileOnly(it)
        annotationProcessor(it)
    }

    libs.mixinExtras.forge.also {
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

    modLocalRuntime(libs.devAuth.forge)

    modImplementation(libs.ioticblocks.forge)

    libs.emi.forge.also {
        modCompileOnly(it)
        modLocalRuntime(it)
    }

    forgeRuntimeLibrary(project(":hexdebug-core-common", "namedElements")) { isTransitive = false }
    forgeRuntimeLibrary(project(":hexdebug-core-forge", "namedElements")) { isTransitive = false }
}
