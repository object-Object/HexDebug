package gay.`object`.hexdebug.debugger

import at.petrak.hexcasting.api.casting.eval.ExecutionClientView
import gay.`object`.hexdebug.core.api.debugging.DebugStepType
import gay.`object`.hexdebug.core.api.debugging.StopReason
import org.eclipse.lsp4j.debug.Source
import org.eclipse.lsp4j.debug.LoadedSourceEventArgumentsReason as LoadedSourceReason

data class DebugStepResult(
    /** If null, the debuggee has continued instead of stopping.  */
    val reason: StopReason?,
    val type: DebugStepType? = null,
    val clientInfo: ExecutionClientView? = null,
    val loadedSources: Map<Source, LoadedSourceReason> = mapOf(),
    val startedEvaluating: Boolean = false,
    val skipped: Boolean = false,
) {
    val isDone = reason == StopReason.TERMINATED

    fun done() = copy(reason = StopReason.TERMINATED)

    fun resumed() = copy(reason = null)

    fun skipped() = copy(skipped = true)

    operator fun plus(other: DebugStepResult) = copy(
        loadedSources = loadedSources + other.loadedSources,
    )

    fun withLoadedSource(source: Source, reason: LoadedSourceReason) = withLoadedSources(mapOf(source to reason))

    fun withLoadedSources(sources: Map<Source, LoadedSourceReason>): DebugStepResult {
        return copy(loadedSources = loadedSources + sources)
    }
}
