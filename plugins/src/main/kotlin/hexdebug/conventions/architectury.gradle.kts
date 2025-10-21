package hexdebug.conventions

import hexdebug.hexdebugProperties
import hexdebug.libs

plugins {
    id("hexdebug.conventions.architectury-base")
    id("hexdebug.utils.OTJFPOPKPCPBP")
}

val platform: String by project

base.archivesName = "${hexdebugProperties.modId}-$platform"

loom {
    accessWidenerPath = project(":Common").file("src/main/resources/hexdebug.accesswidener")

    mixin {
        // the default name includes both archivesName and the subproject, resulting in the platform showing up twice
        // default: hexdebug-common-Common-refmap.json
        // fixed:   hexdebug-common.refmap.json
        defaultRefmapName = "${base.archivesName.get()}.refmap.json"
    }
}

dependencies {
    mappings(loom.layered {
        officialMojangMappings()
        parchment(libs.parchment)
    })
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
    publications {
        create<MavenPublication>("maven") {
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }
}
