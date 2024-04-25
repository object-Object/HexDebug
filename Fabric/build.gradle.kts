plugins {
    id("hexdebug.conventions.platform")
}

hexdebugArchitectury {
    platform = "fabric"
    mavenPublication("mavenFabric")
}

hexdebugPlatform {
    shadowCommonConfiguration("transformProductionFabric")
}

architectury {
    fabric()
}

repositories {
    flatDir { dir("libs") }
}

configurations {
    named("developmentFabric") {
        extendsFrom(get("common"))
    }
}

dependencies {
    modApi(libs.fabric.api)
    modImplementation(libs.fabric.loader)
    modRuntimeOnly(libs.kotlin.fabric)

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
    include(libs.serializationHooks)

    implementation(libs.mixinExtras)
    include(libs.mixinExtras)

    modImplementation(libs.trinkets)

    modApi(libs.clothConfig.fabric) {
        exclude(group = "net.fabricmc.fabric-api")
    }
}

publishMods {
    modLoaders.add("fabric")
    modLoaders.add("quilt")

    // Uncomment your desired platform(s)
//    curseforge {
//        accessToken = project.curseforgeApiToken
//        projectId = project.curseforgeID
//        minecraftVersions.add(minecraftVersion)
//
//        requires{
//            slug = "fabric-api"
//        }
//        requires {
//            slug = "architectury-api"
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
//            slug = "fabric-api"
//        }
//        requires {
//            slug = "architectury-api"
//        }
//        requires {
//            slug = "fabric-language-kotlin"
//        }
//        requires {
//            slug = "hex-casting"
//        }
//    }
}
