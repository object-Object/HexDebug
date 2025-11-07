# HexDebug

[![powered by hexdoc](https://img.shields.io/endpoint?url=https://hexxy.media/api/v0/badge/hexdoc?label=1)](https://github.com/hexdoc-dev/hexdoc)

[CurseForge](https://curseforge.com/minecraft/mc-mods/hexdebug) | [Modrinth](https://modrinth.com/mod/hexdebug)

A [Hex Casting](https://github.com/FallingColors/HexMod) addon for debugging and editing hexes. The Debugger runs a debug server using the [Debug Adapter Protocol](https://microsoft.github.io/debug-adapter-protocol), allowing you to use real debugging tools like VSCode to find bugs in your hexes; and the Splicing Table provides an easy-to-use GUI for finding and fixing mistakes.

[![YouTube video thumbnail: HexDebug Mod Showcase](http://img.youtube.com/vi/FEsmrYoNV0A/0.jpg)](http://www.youtube.com/watch?v=FEsmrYoNV0A "HexDebug Mod Showcase")

## Features

- **Debugging**: Step through your hexes pattern-by-pattern with [vscode-hex-casting](https://marketplace.visualstudio.com/items?itemName=object-Object.hex-casting) or [any other DAP-compatible editor](https://microsoft.github.io/debug-adapter-protocol/implementors/tools/)!
- **Step Modes**: Use any of the well-known step modes (step in/out/over, continue, restart, stop) with the ingame item and/or through VSCode.
- **Variables**: See the stack, ravenmind, and even some internal values like the evaluated pattern count.
- **Call Stack**: Debug complex meta-evaluating hexes with the call stack, generated from the next continuation to be executed. Learn how Hex Casting's internals work!
- **Breakpoints**: Set breakpoints on specific patterns, or use the Uncaught Mishaps option to pause the debugger when a mishap occurs and see what went wrong.
- **Editing**: Use the Splicing Table to edit hexes just like in a text editor, with buttons and keyboard shortcuts to select, move, duplicate, delete, and copy/paste iotas, and even draw patterns in a mini staff grid.
- **Multiplayer**: Debug your hexes in multiplayer! The debug client connects to a port opened by the game client (configurable, defaults to 4444), and each player can have up to one active debugger instance at a time.


## FAQ

### Quilt Kotlin Libraries version

If you're having issues with the version of Quilt Kotlin Libraries / Fabric Language Kotlin on Quilt 1.19.2, try installing *both* of the following:

* [QKL 1.0.2](https://modrinth.com/mod/qkl/version/1.0.2+kt.1.8.0+flk.1.9.0)
* [QKL Core 4.0.0](https://modrinth.com/mod/qkl/version/4.0.0+kt.1.9.23+flk.1.10.19) or higher (the file starting with `quilt-kotlin-libraries-core`)

QKL targets a specific version of Minecraft, while QKL Core is what replaces FLK and can be used with any Minecraft version.

## Maven

Build artifacts are published to the [BlameJared repository](https://maven.blamejared.com/gay/object/hexdebug/) via [Jenkins](https://ci.blamejared.com/job/object-Object/job/HexDebug/).

To depend on HexDebug, add something like this to your build script:

```groovy
repositories {
    maven { url = uri("https://maven.blamejared.com") }
}
dependencies {
    modImplementation("gay.object.hexdebug:hexdebug-$platform:$hexdebugVersion+$minecraftVersion")
}
```

Full examples:

```groovy
// released versions
modImplementation("gay.object.hexdebug:hexdebug-common:0.7.0+1.20.1")
modImplementation("gay.object.hexdebug:hexdebug-fabric:0.7.0+1.20.1")
modImplementation("gay.object.hexdebug:hexdebug-forge:0.7.0+1.20.1")

// bleeding edge builds
modImplementation("gay.object.hexdebug:hexdebug-common:0.7.0+1.20.1-SNAPSHOT")
modImplementation("gay.object.hexdebug:hexdebug-fabric:0.7.0+1.20.1-SNAPSHOT")
modImplementation("gay.object.hexdebug:hexdebug-forge:0.7.0+1.20.1-SNAPSHOT")
```

HexDebug Core is a minimal, Java-only mod containing only the API classes required to implement debug support. The intention is for HexDebug Core to be included in other addons via Jar-in-Jar, allowing addons to implement optional debugging support in their casting environments (eg. wisps, cassettes) while minimizing the amount of overhead and added complexity from checking whether HexDebug is loaded or not. To include HexDebug Core in your mod via Jar-in-Jar, add the following to your Fabric and Forge subprojects **instead of** the above snippets:

```groovy
// Fabric
modImplementation(include("gay.object.hexdebug:hexdebug-core-fabric:$hexdebugVersion+$minecraftVersion"))
modLocalRuntime("gay.object.hexdebug:hexdebug-fabric:$hexdebugVersion+$minecraftVersion")

// Forge (Architectury)
modImplementation(include("gay.object.hexdebug:hexdebug-core-forge:$hexdebugVersion+$minecraftVersion"))
modLocalRuntime("gay.object.hexdebug:hexdebug-forge:$hexdebugVersion+$minecraftVersion")

// Forge (ForgeGradle)
// see also: https://docs.minecraftforge.net/en/fg-6.x/dependencies/jarinjar/
implementation(fg.deobf("gay.object.hexdebug:hexdebug-core-forge:$hexdebugVersion+$minecraftVersion"))
jarJar("gay.object.hexdebug:hexdebug-core-forge:$hexdebugVersion+$minecraftVersion") {
    transitive = false
    jarJar.ranged(it, "[$hexdebugVersion,)")
}
runtimeOnly(fg.deobf("gay.object.hexdebug:hexdebug-forge:$hexdebugVersion+$minecraftVersion"))
```

Mojmap jars for `common` are also available, for use in VanillaGradle-based xplat projects:

```groovy
modImplementation("gay.object.hexdebug:hexdebug-common-mojmap:$hexdebugVersion+$minecraftVersion")
modImplementation("gay.object.hexdebug:hexdebug-core-common-mojmap:$hexdebugVersion+$minecraftVersion")
```

Note that anything outside of the `gay.object.hexdebug.api` and `gay.object.hexdebug.core.api` packages may be changed, renamed, or removed at any time without warning. 

## Attribution

* Textures: SamsTheNerd! ([GitHub](https://github.com/SamsTheNerd), [Modrinth](https://modrinth.com/user/SamsTheNerd))
* Translations:
  * `ru_ru`: JustS-js ([GitHub](https://github.com/JustS-js), [Modrinth](https://modrinth.com/user/JustS-js)) 
  * `zh_cn`: ChuijkYahus ([GitHub](https://github.com/ChuijkYahus))
