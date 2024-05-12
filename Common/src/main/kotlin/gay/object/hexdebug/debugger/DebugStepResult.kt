package gay.`object`.hexdebug.debugger

import org.eclipse.lsp4j.debug.Source
import org.eclipse.lsp4j.debug.LoadedSourceEventArgumentsReason as LoadedSourceReason

data class DebugStepResult(
    val reason: String,
    val type: DebugStepType? = null,
    val loadedSources: Map<Source, LoadedSourceReason> = mapOf(),
) {
    operator fun plus(other: DebugStepResult) = copy(
        loadedSources = loadedSources + other.loadedSources,
    )

    fun withLoadedSource(source: Source, reason: LoadedSourceReason) = withLoadedSources(mapOf(source to reason))

    fun withLoadedSources(sources: Map<Source, LoadedSourceReason>): DebugStepResult {
        return copy(loadedSources = loadedSources + sources)
    }
}
