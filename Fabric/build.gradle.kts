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

repositories {
    flatDir { dir("libs") }
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
    }
    modImplementation(libs.paucal.fabric)
    modImplementation(libs.patchouli.fabric)
    modImplementation(libs.cardinalComponents)
    modImplementation(libs.serializationHooks)
    modImplementation(libs.trinkets)

    implementation(libs.mixinExtras)

    modApi(libs.clothConfig.fabric) {
        exclude(group = "net.fabricmc.fabric-debugger")
    }
    modApi(libs.modMenu)

    implementation(libs.bundles.lsp4j)
    implementation(libs.ktor.network)

    include(libs.serializationHooks)
    include(libs.mixinExtras)
    include(libs.bundles.lsp4j)
    include(libs.bundles.ktor)
}
