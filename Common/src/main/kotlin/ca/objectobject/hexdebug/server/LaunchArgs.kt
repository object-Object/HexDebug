package ca.objectobject.hexdebug.server

class LaunchArgs(rawData: Map<String, Any>) {
    private val data = rawData.withDefault {
        when (it) {
            "stopOnEntry" -> false
            "skipNonEvalFrames" -> true
            "indentWidth" -> 4
            else -> null
        }
    }

    val stopOnEntry: Boolean by data
    val skipNonEvalFrames: Boolean by data
    val indentWidth: Int by data
}
