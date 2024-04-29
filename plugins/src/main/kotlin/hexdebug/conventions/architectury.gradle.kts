package hexdebug.conventions

import gradle.kotlin.dsl.accessors._6658e65164901546bb7440bcefa138c2.main
import gradle.kotlin.dsl.accessors._6658e65164901546bb7440bcefa138c2.sourceSets
import hexdebug.hexdebugProperties
import hexdebug.libs

// plugin config

abstract class HexDebugArchitecturyExtension(private val project: Project) : IHexDebugArchitecturyExtension {
    override val artifactId = project.provider {
        project.hexdebugProperties.getArtifactId(platform.get())
    }

    fun mavenPublication(name: String) = project.run {
        publishing {
            publications {
                register<MavenPublication>(name) {
                    artifactId = this@HexDebugArchitecturyExtension.artifactId.get()
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

    implementation(libs.kotlin.stdlib)
    implementation(libs.findbugs.jsr305)

    implementation(libs.bundles.lsp4j)
    include(libs.bundles.lsp4j)

    implementation(libs.ktor.network)
    include(libs.ktor.network)
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

    }
}
