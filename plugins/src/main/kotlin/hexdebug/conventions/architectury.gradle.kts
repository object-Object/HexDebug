package hexdebug.conventions

import hexdebug.hexdebugProperties
import hexdebug.libs
import kotlin.io.path.div

// plugin config

abstract class HexDebugArchitecturyExtension(private val project: Project) : IHexDebugArchitecturyExtension {
    override fun platform(platform: String) = project.run {
        base.archivesName = "${hexdebugProperties.modId}-$platform"
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
    register<Copy>("exportPrerelease") {
        dependsOn(remapJar)
        from(remapJar.flatMap { it.archiveFile })
        into(rootDir.toPath() / "dist")

        // TODO: this should really use jenkins' build number, if/when we move there
        val sha = providers.environmentVariable("GITHUB_SHA").map { it.take(10) }
        rename { filename ->
            if (sha.isPresent) {
                val base = filename.removeSuffix(".jar")
                "$base-${sha.get()}.jar"
            } else filename
        }
    }
}
