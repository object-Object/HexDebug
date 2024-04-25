plugins {
    id("hexdebug.conventions.platform")
}

hexdebugArchitectury {
    platform = "forge"
    mavenPublication("mavenForge")
}

hexdebugPlatform {
    shadowCommonConfiguration("transformProductionForge")
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

configurations {
    named("developmentForge") {
        extendsFrom(get("common"))
    }
}

dependencies {
    forge(libs.forge)

    modApi(libs.architectury.forge)

    modImplementation(libs.hexcasting.forge) { isTransitive = false }

    modImplementation(libs.paucal.forge)
    modImplementation(libs.patchouli.forge)

//    modCompileOnly libs.paucal.forge
//    modCompileOnly libs.patchouli.forge
//
//    modRuntimeOnly libs.paucal.forge
//    modRuntimeOnly libs.patchouli.forge

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
