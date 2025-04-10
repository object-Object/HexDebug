package hexdebug.conventions

import hexdebug.hexdebugProperties
import hexdebug.libs

plugins {
    id("hexdebug.conventions.kotlin")
    id("hexdebug.utils.OTJFPOPKPCPBP")

    `maven-publish`
    id("dev.architectury.loom")
}

val platform: String by project

base.archivesName = "${hexdebugProperties.modId}-$platform"

loom {
    silentMojangMappingsLicense()
    accessWidenerPath = project(":Common").file("src/main/resources/hexdebug.accesswidener")

    mixin {
        // the default name includes both archivesName and the subproject, resulting in the platform showing up twice
        // default: hexdebug-common-Common-refmap.json
        // fixed:   hexdebug-common.refmap.json
        defaultRefmapName = "${base.archivesName.get()}.refmap.json"
    }
}

dependencies {
    minecraft(libs.minecraft)

    mappings(loom.layered {
        officialMojangMappings()
        parchment(libs.parchment)
    })

    annotationProcessor(libs.bundles.asm)
}

sourceSets {
    main {
        kotlin {
            srcDir(file("src/main/java"))
        }
        resources {
            srcDir(file("src/generated/resources"))
        }
    }
}

publishing {
    repositories {
        hexdebugProperties.localMavenUrl?.let {
            maven {
                url = it
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }
}
