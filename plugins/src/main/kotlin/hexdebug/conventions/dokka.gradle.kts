package hexdebug.conventions

import hexdebug.libs
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

plugins {
    base
    id("org.jetbrains.dokka")
}

dependencies {
    dokkaPlugin(project(":DokkaPlugin"))
    dokkaPlugin(libs.dokka.kotlinAsJava)
}

dokka {
    moduleName = base.archivesName
    dokkaPublications.html {
        failOnWarning = true
    }
    dokkaSourceSets {
        configureEach {
            documentedVisibilities.set(setOf(VisibilityModifier.Public, VisibilityModifier.Protected))
            perPackageOption {
                suppress = true
            }
            perPackageOption {
                matchingRegex = """.*?\.api(\..+|$)"""
                suppress = false
            }
            sourceLink {
                val githubRepository = System.getenv("GITHUB_REPOSITORY") ?: "object-Object/HexDebug"
                val githubRef = System.getenv("GITHUB_SHA") ?: "main"
                val projectPath = projectDir
                    .relativeTo(rootProject.projectDir)
                    .toPath()
                    .joinToString("/")
                remoteUrl = uri("https://github.com/$githubRepository/tree/$githubRef/$projectPath")
                remoteLineSuffix = "#L"
            }
        }
    }
    pluginsConfiguration.html {
        templatesDir = rootProject.file("dokka/templates")
        customStyleSheets = rootProject.fileTree("dokka/styles").matching { include("**/*.css") }
        customAssets.from(
            project(":Common").file("src/main/resources/icon.png"),
        )
    }
}
