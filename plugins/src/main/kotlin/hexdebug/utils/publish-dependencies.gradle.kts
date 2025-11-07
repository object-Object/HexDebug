package hexdebug.utils

plugins {
    id("me.modmuss50.mod-publish-plugin")
}

abstract class HexDebugPublishDependenciesExtension(private val project: Project) {
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
}

extensions.create<HexDebugPublishDependenciesExtension>("hexdebugPublishDependencies")
