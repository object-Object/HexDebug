package ca.objectobject.hexdebug.server

data class LaunchArgs(val data: Map<String, Any>) {
    val stopOnEntry = data.getOrDefault("stopOnEntry", false) as Boolean
}
