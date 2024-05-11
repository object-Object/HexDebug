package hexdebug

import org.gradle.api.Project
import org.gradle.kotlin.dsl.provideDelegate

const val SECTION_HEADER_PREFIX = "## "

open class HexDebugProperties(private val project: Project) {
    val modId: String by project
    val modVersion: String by project
    val mavenGroup: String by project
    val curseforgeId: String by project
    val modrinthId: String by project

    val javaVersion = project.libs.versions.java.get().toInt()
    val minecraftVersion = project.libs.versions.minecraft.get()

    fun getLatestChangelog() = project.rootProject.file("CHANGELOG.md").useLines { lines ->
        lines.dropWhile { !it.startsWith(SECTION_HEADER_PREFIX) }
            .withIndex()
            .takeWhile { it.index == 0 || !it.value.startsWith(SECTION_HEADER_PREFIX) }
            .joinToString("\n") { it.value }
            .trim()
    }
}
