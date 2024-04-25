plugins {
    id("hexdebug.conventions.platform")
}

architectury {
    forge()
}

loom {
    forge {
        convertAccessWideners = true
        extraAccessWideners.add(loom.accessWidenerPath.get().asFile.name)

        mixinConfig("hexdebug-common.mixins.json")
        mixinConfig("hexdebug.mixins.json")
    }
}

hexdebugArchitectury {
    platform = "forge"
    mavenPublication("mavenForge")
}

hexdebugPlatform {
    developmentConfiguration("developmentForge")
    shadowCommonConfiguration("transformProductionForge")
}

hexdebugModDependencies {
    filesMatching.add("META-INF/mods.toml")
}

dependencies {
    forge(libs.forge)

    modApi(libs.architectury.forge)

    modImplementation(libs.hexcasting.forge) { isTransitive = false }

    modImplementation(libs.paucal.forge)
    modImplementation(libs.patchouli.forge)

    modRuntimeOnly(libs.kotlin.forge)

    modApi(libs.clothConfig.forge)
}

tasks {
    shadowJar {
        exclude("fabric.mod.json")
    }
}

publishMods {
    modLoaders.add("forge")

//    curseforge {
//        accessToken = project.curseforgeApiToken
//        projectId = project.curseforgeId
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
//        projectId = project.modrinthId
//        minecraftVersions.add("1.19.2")
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
