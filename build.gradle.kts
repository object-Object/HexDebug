import kotlin.script.experimental.jvm.util.classpathFromClass

plugins {
    id("hexdebug.conventions.kotlin")
    id("hexdebug.conventions.dokka")
    alias(libs.plugins.dotenv)
}

val minecraftVersion: String by project

// scuffed sanity check, because we need minecraftVersion to be in gradle.properties for the hexdoc plugin
libs.versions.minecraft.get().also {
    if (minecraftVersion != it) {
        throw IllegalArgumentException("Mismatched Minecraft version: gradle.properties ($minecraftVersion) != libs.versions.toml ($it)")
    }
}

architectury {
    minecraft = minecraftVersion
}

// remember to add the other subprojects here if we add per-platform APIs in the future
dependencies {
    dokka(project(":Common"))
    dokka(project(":hexdebug-core-common"))
}

tasks {
    register("viewLatestChangelog") {
        group = "documentation"
        description = "Print the topmost single version section from the full CHANGELOG.md file."
        doLast {
            println(hexdebugProperties.getLatestChangelog())
        }
    }

    register("runAllDatagen") {
        dependsOn(":Forge:runCommonDatagen")
        dependsOn(":Forge:runForgeDatagen")
        dependsOn(":Fabric:runDatagen")
    }
}

val copyPlantUml by tasks.registering(Sync::class) {
    group = "plantuml"

    from("plantuml")
    into("build/plantuml")
}

val renderPlantUml by tasks.registering(JavaExec::class) {
    group = "plantuml"

    val outputDir = copyPlantUml.get().destinationDir

    inputs.files(copyPlantUml)
    outputs.dir(outputDir)

    classpath = files(classpathFromClass<net.sourceforge.plantuml.Run>())
    args(
        "${outputDir}/**/*.puml",
        "--format", "png",
        "--define", "PLANTUML_LIMIT_SIZE=8192",
        "--skinparam", "dpi=300",
        "--exclude", "**/_*",
        "--exclude", "${outputDir}/utils/**",
        "--exclude", "${outputDir}/continuations.puml", // broken?
    )
}

dokka {
    pluginsConfiguration.html {
        customAssets.from(renderPlantUml)
    }
}
