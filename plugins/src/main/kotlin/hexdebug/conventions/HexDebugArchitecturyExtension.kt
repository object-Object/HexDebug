package hexdebug.conventions

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

interface IHexDebugArchitecturyExtension {
    val platform: Property<String>

    val artifactId: Provider<String>

    fun mavenPublication(name: String)
}
