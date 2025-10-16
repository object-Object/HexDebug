// minimal set of classes required to implement debug support in another mod
// this is intended to be included via JiJ to allow HexDebug to be an optional dependency

plugins {
    id("hexdebug.conventions.architectury")
}

architectury {
    common("fabric", "forge")
}

dependencies {
    modApi(libs.hexcasting.common)
}
