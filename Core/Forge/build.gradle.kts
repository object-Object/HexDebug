plugins {
    id("hexdebug.conventions.platform-base")
}

architectury {
    forge()
}

hexdebugModDependencies {
    filesMatching.add("META-INF/mods.toml")

    anyVersion = ""
    mapVersions {
        replace(Regex("""\](\S+)"""), "($1")
        replace(Regex("""(\S+)\["""), "$1)")
    }
}

dependencies {
    forge(libs.forge)
    modApi(libs.hexcasting.forge) { isTransitive = false }
}
