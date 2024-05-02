plugins {
    id("hexdebug.conventions.kotlin")
    alias(libs.plugins.dotenv)
}

architectury {
    minecraft = libs.versions.minecraft.get()
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
