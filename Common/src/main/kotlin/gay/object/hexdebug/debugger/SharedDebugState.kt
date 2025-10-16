package gay.`object`.hexdebug.debugger

import gay.`object`.hexdebug.adapter.LaunchArgs
import gay.`object`.hexdebug.debugger.allocators.SourceAllocator
import org.eclipse.lsp4j.debug.Breakpoint
import org.eclipse.lsp4j.debug.BreakpointNotVerifiedReason
import org.eclipse.lsp4j.debug.InitializeRequestArguments
import org.eclipse.lsp4j.debug.SourceBreakpoint

// state shared between all debuggers in a debug adapter
class SharedDebugState {
    var launchArgs = LaunchArgs()
    var initArgs = defaultInitArgs()

    // source id -> line number
    val breakpoints = mutableMapOf<Int, MutableMap<Int, SourceBreakpointMode>>()
    val exceptionBreakpoints = mutableSetOf<ExceptionBreakpointType>()

    val sourceAllocator = SourceAllocator()

    fun onDisconnect() {
        // don't clear sources here, we might still need them
        launchArgs = LaunchArgs()
        initArgs = defaultInitArgs()
        breakpoints.clear()
        exceptionBreakpoints.clear()
    }

    fun getSources() = sourceAllocator.map { it.first }

    // TODO: gross.
    // TODO: there's probably a bug here somewhere - shouldn't we be using the metadata?
    fun setBreakpoints(
        sourceReference: Int,
        sourceBreakpoints: Array<SourceBreakpoint>,
    ): List<Breakpoint> {
        val breakpointLines = breakpoints.getOrPut(sourceReference, ::mutableMapOf)
        breakpointLines.clear()

        val (source, iotas) = sourceAllocator[sourceReference] ?: (null to null)

        return sourceBreakpoints.map { breakpoint ->
            Breakpoint().apply {
                isVerified = false
                if (source == null || iotas == null) {
                    message = "Unknown source"
                    reason = BreakpointNotVerifiedReason.PENDING  // TODO: send Breakpoint event later
                } else if (breakpoint.line > initArgs.indexToLine(iotas.lastIndex)) {
                    message = "Line number out of range"
                    reason = BreakpointNotVerifiedReason.FAILED
                } else {
                    isVerified = true
                    this.source = source
                    line = breakpoint.line

                    breakpointLines[breakpoint.line] = breakpoint.mode
                        ?.let(SourceBreakpointMode::valueOf)
                        ?: SourceBreakpointMode.EVALUATED
                }
            }
        }
    }
}

private fun defaultInitArgs() = InitializeRequestArguments().apply {
    linesStartAt1 = true
    columnsStartAt1 = true
}
