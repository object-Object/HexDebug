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

    private val versions get() = project.libs.versions

    val javaVersion = versions.java.get().toInt()
    val minecraftVersion = versions.minecraft.get()

    val publishMavenRelease = System.getenv("PUBLISH_MAVEN_RELEASE") == "true"
    val localMavenUrl = System.getenv("local_maven_url")?.let(project::uri)

    fun getLatestChangelog() = project.rootProject.file("CHANGELOG.md").useLines { lines ->
        lines.dropWhile { !it.startsWith(SECTION_HEADER_PREFIX) }
            .withIndex()
            .takeWhile { it.index == 0 || !it.value.startsWith(SECTION_HEADER_PREFIX) }
            .joinToString("\n") { it.value }
            .trim()
    }
}
