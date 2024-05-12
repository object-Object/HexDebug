import hexdebug.libs

plugins {
    id("hexdebug.conventions.architectury")
}

architectury {
    common("fabric", "forge")
}

hexdebugArchitectury {
    platform("common")
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(kotlin("reflect"))

    // We depend on fabric loader here to use the fabric @Environment annotations and get the mixin dependencies
    // Do NOT use other classes from fabric loader
    modImplementation(libs.fabric.loader)
    modApi(libs.architectury)

    modImplementation(libs.hexcasting.common)
    modImplementation(libs.paucal.common)
    modImplementation(libs.patchouli.xplat)

    modImplementation(libs.clothConfig.common)

    implementation(libs.mixinExtras)

    implementation(libs.bundles.lsp4j)
    implementation(libs.ktor.network)
}
