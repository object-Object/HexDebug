package hexdebug

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.the

val Project.libs get() = the<LibrariesForLibs>()

val Project.hexdebugProperties get() = HexDebugProperties(this)
