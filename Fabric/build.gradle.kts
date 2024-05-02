import hexdebug.libs

plugins {
    id("hexdebug.conventions.platform")
}

architectury {
    fabric()
}

hexdebugArchitectury {
    platform = "fabric"
    mavenPublication("mavenFabric")
}

hexdebugPlatform {
    developmentConfiguration("developmentFabric")
    shadowCommonConfiguration("transformProductionFabric")
}

hexdebugModDependencies {
    filesMatching.add("fabric.mod.json")

    versions = versions.get().mapValues { (_, version) ->
        version
            .replace(",", " ")
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""\[(\S+)"""), ">=$1")
            .replace(Regex("""(\S+)\]"""), "<=$1")
            .replace(Regex("""\](\S+)"""), ">$1")
            .replace(Regex("""(\S+)\["""), "<$1")
    }
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
    include(libs.ktor.network)
}

publishMods {
    modLoaders.addAll("fabric", "quilt")

    // Uncomment your desired platform(s)
//    curseforge {
//        accessToken = project.curseforgeApiToken
//        projectId = project.curseforgeID
//        minecraftVersions.add(minecraftVersion)
//
//        requires{
//            slug = "fabric-debugger"
//        }
//        requires {
//            slug = "architectury-debugger"
//        }
//        requires {
//            slug = "fabric-language-kotlin"
//        }
//        requires {
//            slug = "hexcasting"
//        }
//    }
//
//    modrinth {
//        accessToken = project.modrinthApiToken
//        projectId = project.modrinthID
//        minecraftVersions.add("1.19.2")
//
//        requires{
//            slug = "fabric-debugger"
//        }
//        requires {
//            slug = "architectury-debugger"
//        }
//        requires {
//            slug = "fabric-language-kotlin"
//        }
//        requires {
//            slug = "hex-casting"
//        }
//    }
}
