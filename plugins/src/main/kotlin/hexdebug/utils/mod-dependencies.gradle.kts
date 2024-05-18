package hexdebug.utils

plugins {
    id("me.modmuss50.mod-publish-plugin")
}

// plugin config

abstract class HexDebugModDependenciesExtension(private val project: Project) {
    private val versionCatalog by lazy {
        project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    }

    abstract val filesMatching: ListProperty<String>
    abstract val anyVersion: Property<String>
    abstract val mapVersions: Property<VersionMapper.() -> Unit>

    fun mapVersions(action: VersionMapper.() -> Unit) {
        mapVersions = action
    }

    fun getVersions(): Map<String, String> = versionCatalog.versionAliases.associate {
        val mapper = VersionMapper(
            name = it.replace(".", "_"),
            constraint = versionCatalog.findVersion(it).get(),
        )
        mapVersions.get().invoke(mapper)
        mapper.name to (mapper.version ?: anyVersion.get())
    }

    fun requires(slug: String) = requires(slug, slug)
    fun requires(curseforge: String, modrinth: String) = dependency(curseforge, modrinth, true)

    fun optional(slug: String) = optional(slug, slug)
    fun optional(curseforge: String, modrinth: String) = dependency(curseforge, modrinth, false)

    fun dependency(
        curseforge: String,
        modrinth: String,
        required: Boolean = true,
    ) {
        project.publishMods {
            curseforge {
                if (required) requires(curseforge) else optional(curseforge)
            }
            modrinth {
                if (required) requires(modrinth) else optional(modrinth)
            }
        }
    }

    data class VersionMapper(
        val name: String,
        val constraint: VersionConstraint,
    ) {
        var version: String? = constraint.requiredVersion.takeIf { it.isNotEmpty() }

        fun replace(old: String, new: String) {
            version = version?.replace(old, new)
        }

        fun replace(old: Regex, new: String) {
            version = version?.replace(old, new)
        }
    }
}

val extension = extensions.create<HexDebugModDependenciesExtension>("hexdebugModDependencies")

// build logic

val modVersion: String by project

tasks.withType<ProcessResources>().configureEach {
    // allow referencing values from libs.versions.toml in Fabric/Forge mod configs
    val dependencyVersions = mapOf(
        "modVersion" to modVersion,
        "versions" to extension.getVersions(),
    )

    // for incremental builds
    inputs.properties(dependencyVersions)

    filesMatching(extension.filesMatching.get()) {
        expand(dependencyVersions)
    }
}
