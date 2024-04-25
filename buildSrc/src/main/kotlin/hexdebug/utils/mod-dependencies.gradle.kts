package hexdebug.utils

// plugin config

interface HexDebugModDependenciesExtension {
    val filesMatching: ListProperty<String>
    val versions: MapProperty<String, String>
}

val extension = extensions.create<HexDebugModDependenciesExtension>("hexdebugModDependencies")

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

    filesMatching(extension.filesMatching.get()) {
        expand(dependencyVersions)
    }
}
