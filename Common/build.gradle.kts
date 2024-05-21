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

    modApi(libs.hexcasting.common)

    modApi(libs.hexal.common)

    modApi(libs.clothConfig.common)

    implementation(libs.mixinExtras)

    api(libs.bundles.lsp4j)

    implementation(libs.bundles.ktor)
}
