package hexdebug.conventions

import hexdebug.hexdebugProperties
import hexdebug.libs
import kotlin.io.path.div

// plugin config

abstract class HexDebugArchitecturyExtension(private val project: Project) : IHexDebugArchitecturyExtension {
    override fun platform(platform: String) = project.run {
        val archivesName = "${hexdebugProperties.modId}-$platform"

        base.archivesName = archivesName

        publishing {
            publications {
                named<MavenPublication>("maven") {
                    artifactId = archivesName
                }
            }
        }
    }
}

val extension = extensions.create<HexDebugArchitecturyExtension>("hexdebugArchitectury")

// build logic

plugins {
    id("hexdebug.conventions.kotlin")
    id("hexdebug.utils.OTJFPOPKPCPBP")

    `maven-publish`
    id("dev.architectury.loom")
}

loom {
    silentMojangMappingsLicense()
    accessWidenerPath = project(":Common").file("src/main/resources/hexdebug.accesswidener")
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

tasks {
    val jenkinsArtifacts = register<Copy>("jenkinsArtifacts") {
        from(remapJar, remapSourcesJar, get("javadocJar"))
        into(rootDir.toPath() / "build" / "jenkinsArtifacts")
    }

    build {
        dependsOn(jenkinsArtifacts)
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
            from(components["java"])
        }
    }
}
