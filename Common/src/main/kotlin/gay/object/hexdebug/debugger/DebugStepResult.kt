package gay.`object`.hexdebug.debugger

import org.eclipse.lsp4j.debug.Source
import org.eclipse.lsp4j.debug.LoadedSourceEventArgumentsReason as LoadedSourceReason

data class DebugStepResult(
    val reason: String,
    val type: DebugStepType? = null,
    val loadedSources: Map<Source, LoadedSourceReason> = mapOf(),
) {

    @Suppress("UNUSED_PARAMETER")
    operator fun plus(lastResult: DebugStepResult) = this

    fun withLoadedSource(source: Source, reason: LoadedSourceReason) = withLoadedSources(sequenceOf(source to reason))

    fun withLoadedSources(sources: Sequence<Pair<Source, LoadedSourceReason>>) =
        copy(loadedSources = loadedSources + sources)
}

enum class DebugStepType {
    IN,
    OUT,
    JUMP,
    ESCAPE,
}
