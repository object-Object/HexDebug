import hexdebug.libs

plugins {
    id("hexdebug.conventions.platform")
}

architectury {
    fabric()
}

hexdebugPlatform {
    platform("fabric", "quilt")
}

hexdebugModDependencies {
    filesMatching.add("fabric.mod.json")

    anyVersion = "*"
    mapVersions {
        replace(",", " ")
        replace(Regex("""\s+"""), " ")
        replace(Regex("""\[(\S+)"""), ">=$1")
        replace(Regex("""(\S+)\]"""), "<=$1")
        replace(Regex("""\](\S+)"""), ">$1")
        replace(Regex("""(\S+)\["""), "<$1")
    }

    requires("architectury-api")
    requires("cloth-config")
    requires(curseforge = "hexcasting", modrinth = "hex-casting")

    requires("fabric-api")
    requires("fabric-language-kotlin")
    requires("modmenu")
}

dependencies {
    modApi(libs.fabric.api)
    modImplementation(libs.fabric.loader)

    modImplementation(libs.kotlin.fabric)

    modApi(libs.architectury.fabric) {
        // Fix for the "two fabric loaders" loading crash
        exclude(group = "net.fabricmc", module = "fabric-loader")
    }

    modImplementation(libs.hexcasting.fabric) {
        // If not excluded here, calls a nonexistent method and crashes the dev client
        exclude(module = "phosphor")
        exclude(module = "pehkui")
    }
    modImplementation(libs.paucal.fabric)
    modImplementation(libs.patchouli.fabric)
    modImplementation(libs.cardinalComponents)
    modImplementation(libs.serializationHooks)
    modImplementation(libs.entityReach)
    modImplementation(libs.trinkets)

    implementation(libs.mixinExtras)

    modApi(libs.clothConfig.fabric) {
        exclude(group = "net.fabricmc.fabric-api")
    }
    modApi(libs.modMenu)

    implementation(libs.bundles.lsp4j)
    implementation(libs.ktor.network)

    include(libs.serializationHooks)
    include(libs.entityReach)
    include(libs.mixinExtras)
    include(libs.bundles.lsp4j)
    include(libs.bundles.ktor)
}

// this fails if we do it for all projects, since the tag already exists :/
// see https://github.com/modmuss50/mod-publish-plugin/issues/3
publishMods {
    github {
        accessToken = providers.environmentVariable("GITHUB_TOKEN").orElse("")
        repository = providers.environmentVariable("GITHUB_REPOSITORY").orElse("")
        commitish = providers.environmentVariable("GITHUB_SHA").orElse("")

        type = STABLE
        displayName = "v${project.version}"
        tagName = "v${project.version}"

        additionalFiles.from(project(":Common").tasks.remapJar.get().archiveFile)
    }
}
