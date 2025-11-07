package hexdebug.conventions

import hexdebug.hexdebugProperties
import hexdebug.libs

plugins {
    id("hexdebug.conventions.kotlin")

    `maven-publish`
    id("dev.architectury.loom")
}

loom {
    silentMojangMappingsLicense()
}

dependencies {
    minecraft(libs.minecraft)

    annotationProcessor(libs.bundles.asm)
}

publishing {
    repositories {
        hexdebugProperties.localMavenUrl?.let {
            maven {
                url = it
            }
        }
    }
}
