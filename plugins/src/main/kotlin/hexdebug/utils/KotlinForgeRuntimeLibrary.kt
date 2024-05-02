package hexdebug.utils

import org.gradle.api.attributes.Attribute
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope

val patchedFMLModType = Attribute.of("patchedFMLModType", Boolean::class.javaObjectType)

// TODO: is there a better way to do this? `extendsFrom` seems to ignore attributes, so that doesn't work.
fun DependencyHandlerScope.kotlinForgeRuntimeLibrary(dependency: Provider<*>) {
    "localRuntime"(dependency) {
        isTransitive = false
        attributes {
            attribute(patchedFMLModType, true)
        }
    }
}
