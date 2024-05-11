plugins {
    id("hexdebug.conventions.kotlin")
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
    }
}
