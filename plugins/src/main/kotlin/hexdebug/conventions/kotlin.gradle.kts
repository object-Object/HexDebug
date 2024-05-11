package hexdebug.conventions

import hexdebug.hexdebugProperties

plugins {
    id("hexdebug.utils.properties")

    java
    kotlin("jvm")
    id("architectury-plugin")
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://squiddev.cc/maven") }
    maven { url = uri("https://maven.terraformersmc.com") }
    maven { url = uri("https://maven.terraformersmc.com/releases") }
    maven { url = uri("https://maven.shedaniel.me") }
    maven { url = uri("https://maven.blamejared.com") }
    maven { url = uri("https://maven.jamieswhiteshirt.com/libs-release") }
    maven { url = uri("https://mvn.devos.one/snapshots") }
    maven { url = uri("https://maven.ladysnake.org/releases") }
    maven { url = uri("https://thedarkcolour.github.io/KotlinForForge") }
    maven { url = uri("https://maven.theillusivec4.top") }
    maven { url = uri("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven") }
    maven { url = uri("https://maven.parchmentmc.org") }
    exclusiveContent {
        forRepository {
            maven { url = uri("https://api.modrinth.com/maven") }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

//dependencies {
//    testImplementation("org.jetbrains.kotlin:kotlin-test")
//}

hexdebugProperties.also {
    group = it.mavenGroup
    version = "${it.modVersion}+${it.minecraftVersion}"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(hexdebugProperties.javaVersion)
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    jvmToolchain(hexdebugProperties.javaVersion)
}

tasks {
    compileJava {
        options.apply {
            encoding = "UTF-8"
            release = hexdebugProperties.javaVersion
        }
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    withType<GenerateModuleMetadata>().configureEach {
        enabled = false
    }

    javadoc {
        options {
            this as StandardJavadocDocletOptions
            addStringOption("Xdoclint:none", "-quiet")
        }
    }

    processResources {
        exclude(".cache")
    }

//    test {
//        useJUnitPlatform()
//    }

    processTestResources {
        exclude(".cache")
    }
}
