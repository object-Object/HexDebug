package hexdebug.conventions

import kotlin.io.path.div

plugins {
    id("hexdebug.conventions.architectury")
    id("hexdebug.utils.mod-dependencies")

    id("com.github.johnrengelman.shadow")
}

val commonProject: String by project
val platform: String by project
// hack: core-fabric -> fabric
val platformCapitalized = platform.split("-").last().replaceFirstChar(Char::uppercase)

loom {
    runs {
        named("server") {
            runDir = "runServer"
        }
    }
}

configurations {
    register("common")
    register("shadowCommon")
    compileClasspath {
        extendsFrom(get("common"))
    }
    runtimeClasspath {
        extendsFrom(get("common"))
    }
    // this needs to wait until Loom has been configured
    afterEvaluate {
        named("development$platformCapitalized") {
            extendsFrom(get("common"))
        }
    }
}

dependencies {
    "common"(project(commonProject, "namedElements")) { isTransitive = false }
    "shadowCommon"(project(commonProject, "transformProduction$platformCapitalized")) { isTransitive = false }
}

// FIXME: find a less broken way to include common resources in platform devenv - this one breaks mixin refmaps
//sourceSets {
//    main {
//        resources {
//            source(project(":Common").sourceSets.main.get().resources)
//        }
//    }
//}

tasks {
    val jenkinsArtifacts = register<Copy>("jenkinsArtifacts") {
        from(remapJar)
        into(rootDir.toPath() / "build" / "jenkinsArtifacts")
    }

    build {
        dependsOn(jenkinsArtifacts)
    }

    shadowJar {
        exclude("architectury.common.json")
        configurations = listOf(project.configurations["shadowCommon"])
        archiveClassifier = "dev-shadow"
    }

    remapJar {
        dependsOn(shadowJar)
        inputFile = shadowJar.get().archiveFile
        archiveClassifier = null
    }

    jar {
        archiveClassifier = "dev"
    }

    kotlinSourcesJar {
        val commonSources = project(commonProject).tasks.kotlinSourcesJar
        dependsOn(commonSources)
        from(commonSources.flatMap { it.archiveFile }.map(::zipTree))
    }
}

components {
    named<AdhocComponentWithVariants>("java") {
        withVariantsFromConfiguration(configurations.shadowRuntimeElements.get()) {
            skip()
        }
    }
}
