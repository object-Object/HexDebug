# HexDebug

[![powered by hexdoc](https://img.shields.io/endpoint?url=https://hexxy.media/api/v0/badge/hexdoc?label=1)](https://github.com/hexdoc-dev/hexdoc)

[CurseForge](https://curseforge.com/minecraft/mc-mods/hexdebug) | [Modrinth](https://modrinth.com/mod/hexdebug)

A [Hex Casting](https://github.com/FallingColors/HexMod) addon that runs a debug server using the [Debug Adapter Protocol](https://microsoft.github.io/debug-adapter-protocol), allowing you to use real debugging tools like VSCode to find bugs in your hexes.

[![YouTube video thumbnail: HexDebug Mod Showcase](http://img.youtube.com/vi/FEsmrYoNV0A/0.jpg)](http://www.youtube.com/watch?v=FEsmrYoNV0A "HexDebug Mod Showcase")

## Features

- **Debugging**: Step through your hexes pattern-by-pattern with [vscode-hex-casting](https://marketplace.visualstudio.com/items?itemName=object-Object.hex-casting) or [any other DAP-compatible editor](https://microsoft.github.io/debug-adapter-protocol/implementors/tools/)!
- **Step Modes**: Use any of the well-known step modes (step in/out/over, continue, restart, stop) with the ingame item and/or through VSCode.
- **Variables**: See the stack, ravenmind, and even some internal values like the evaluated pattern count.
- **Call Stack**: Debug complex meta-evaluating hexes with the call stack, generated from the next continuation to be executed. Learn how Hex Casting's internals work!
- **Breakpoints**: Set breakpoints on specific patterns, or use the Uncaught Mishaps option to pause the debugger when a mishap occurs and see what went wrong.
- **Multiplayer**: Debug your hexes in multiplayer! The debug client connects to a port opened by the game client (configurable, defaults to 4444), and each player can have up to one active debugger instance at a time.

## Maven

Build artifacts are published to the [BlameJared repository](https://maven.blamejared.com/gay/object/hexdebug/) via [Jenkins](https://ci.blamejared.com/job/object-Object/job/HexDebug/).

To depend on HexDebug, add something like this to your build script:

```groovy
repositories {
    maven { url = uri("https://maven.blamejared.com") }
}
dependencies {
    modImplementation("gay.object.hexdebug:hexdebug-$platform:$hexdebugVersion")
}
```

Full examples:

```groovy
// released versions
modImplementation("gay.object.hexdebug:hexdebug-common:0.1.2+1.20.1")
modImplementation("gay.object.hexdebug:hexdebug-fabric:0.1.2+1.20.1")
modImplementation("gay.object.hexdebug:hexdebug-forge:0.1.2+1.20.1")

// bleeding edge builds
modImplementation("gay.object.hexdebug:hexdebug-common:0.1.2+1.20.1-SNAPSHOT")
modImplementation("gay.object.hexdebug:hexdebug-fabric:0.1.2+1.20.1-SNAPSHOT")
modImplementation("gay.object.hexdebug:hexdebug-forge:0.1.2+1.20.1-SNAPSHOT")
```

Try to avoid using things outside of the `gay.object.hexdebug.api` package, since they may change at any time.

## Attribution

* Textures: SamsTheNerd! ([GitHub](https://github.com/SamsTheNerd), [Modrinth](https://modrinth.com/user/SamsTheNerd))
* Translations:
  * `zh_cn`: ChuijkYahus ([GitHub](https://github.com/ChuijkYahus))
