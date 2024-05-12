package gay.`object`.hexdebug.adapter

class LaunchArgs(data: Map<String, Any>) {
    val stopOnEntry: Boolean by data.withDefault { true }
    val stopOnExit: Boolean by data.withDefault { false }
    val skipNonEvalFrames: Boolean by data.withDefault { true }
    val indentWidth: Int by data.withDefault { 4 }
}
