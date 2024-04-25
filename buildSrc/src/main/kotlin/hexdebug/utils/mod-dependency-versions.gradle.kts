package hexdebug.utils

// plugin config

interface ModDependencyVersionsExtension {
    val filesMatching: ListProperty<String>
    val versions: MapProperty<String, String>
}

val extension = extensions.create<ModDependencyVersionsExtension>("modDependencyVersions")

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
extension.versions.convention(provider {
    versionCatalog.versionAliases.associate {
        // both "." and "-" cause issues with expand :/
        it.replace(".", "_") to versionCatalog.findVersion(it).get().requiredVersion
    }
})

// build logic

val modVersion: String by project

tasks.withType<ProcessResources>().configureEach {
    // allow referencing values from libs.versions.toml in Fabric/Forge mod configs
    val dependencyVersions = mapOf(
        "modVersion" to modVersion,
        "versions" to extension.versions.get(),
    )

    // for incremental builds
    inputs.properties(dependencyVersions)

    filesMatching(listOf("fabric.mod.json", "META-INF/mods.toml")) {
        expand(dependencyVersions)
    }
}
