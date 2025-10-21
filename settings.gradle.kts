pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.architectury.dev/") }
        maven { url = uri("https://maven.fabricmc.net/") }
        maven { url = uri("https://maven.minecraftforge.net/") }
        maven { url = uri("https://maven.blamejared.com/") }
    }

    includeBuild("plugins")
}

rootProject.name = "HexDebug"

include(
    "Common",
    "Common:Mojmap",
    "Fabric",
    "Forge",
)

// architectury uses the project name as the JarJar artifact id for include deps :/
// TODO: can we use capabilities instead? https://github.com/architectury/architectury-loom/blob/05e7217480d858cda2c56d1f17fc75b17beab1fa/src/main/java/net/fabricmc/loom/build/nesting/NestableJarGenerationTask.java#L154
includeNamed("hexdebug-core-common", "Core/Common")
includeNamed("hexdebug-core-common:Mojmap", "Core/Common/Mojmap")
includeNamed("hexdebug-core-fabric", "Core/Fabric")
includeNamed("hexdebug-core-forge", "Core/Forge")

fun includeNamed(name: String, path: String) {
    include(name)
    project(":$name").projectDir = file(path)
}
