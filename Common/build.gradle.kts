plugins {
    id("hexdebug.conventions.architectury")
}

architectury {
    common("fabric", "forge")
}

hexdebugArchitectury {
    platform = "common"
    mavenPublication("mavenCommon")
}

dependencies {
    // We depend on fabric loader here to use the fabric @Environment annotations and get the mixin dependencies
    // Do NOT use other classes from fabric loader
    modImplementation(libs.fabric.loader)
    modApi(libs.architectury)

    modCompileOnly(libs.paucal.common)
    modCompileOnly(libs.hexcasting.common)
    modCompileOnly(libs.patchouli.xplat)
    modCompileOnly(libs.clothConfig.common)
}
