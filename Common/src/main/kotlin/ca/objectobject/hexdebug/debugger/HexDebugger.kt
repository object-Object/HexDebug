package ca.objectobject.hexdebug.debugger

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.casting.PatternShapeMatch
import at.petrak.hexcasting.api.casting.SpellList
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.eval.SpecialPatterns
import at.petrak.hexcasting.api.casting.eval.sideeffects.OperatorSideEffect
import at.petrak.hexcasting.api.casting.eval.vm.*
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation.Done
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation.NotDone
import at.petrak.hexcasting.api.casting.iota.*
import at.petrak.hexcasting.api.casting.mishaps.Mishap
import at.petrak.hexcasting.api.casting.mishaps.MishapInternalException
import at.petrak.hexcasting.common.casting.PatternRegistryManifest
import ca.objectobject.hexdebug.debugger.allocators.SourceAllocator
import ca.objectobject.hexdebug.debugger.allocators.VariablesAllocator
import ca.objectobject.hexdebug.server.LaunchArgs
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import org.eclipse.lsp4j.debug.*
import java.util.*
import kotlin.math.min

class HexDebugger(
    private val initArgs: InitializeRequestArguments,
    private val launchArgs: LaunchArgs,
    private val vm: CastingVM,
    private val world: ServerLevel,
    private val onExecute: ((Iota) -> Unit)? = null,
    iotas: List<Iota>,
) {
    constructor(initArgs: InitializeRequestArguments, launchArgs: LaunchArgs, castArgs: DebugCastArgs)
        : this(initArgs, launchArgs, castArgs.vm, castArgs.world, castArgs.onExecute, castArgs.iotas)

    // Initialize the continuation stack to a single top-level eval for all iotas.
    private var nextContinuation = Done.pushFrame(FrameEvaluate(SpellList.LList(0, iotas), false))
        set(value) {
            field = value
            callStack = getCallStack(value)
        }

    private var callStack = getCallStack(nextContinuation)

    private val variablesAllocator = VariablesAllocator()
    private val sourceAllocator = SourceAllocator()
    private val iotaMetadata = IdentityHashMap<Iota, IotaMetadata>()
    private val breakpoints = mutableMapOf<Int, Set<Int>>() // source id -> line number

    init {
        registerNewSource(iotas)
    }

    private fun registerNewSource(frame: ContinuationFrame): Iota? {
        val iotas = when (frame) {
            is FrameEvaluate -> frame.list
            is FrameForEach -> frame.code
            else -> return null
        }
        if (!iotas.nonEmpty) return null
        registerNewSource(iotas)
        return iotas.car
    }

    private fun registerNewSource(iotas: Iterable<Iota>) {
        val unregisteredIotas = iotas.filter { it !in iotaMetadata }
        if (unregisteredIotas.isEmpty()) return

        val source = sourceAllocator.add(unregisteredIotas)
        for ((line, iota) in unregisteredIotas.withIndex()) {
            iotaMetadata[iota] = IotaMetadata(source, line)
        }
    }

    // current continuation is last
    private fun getCallStack(current: SpellContinuation) = generateSequence(current as? NotDone) {
        when (val next = it.next) {
            is Done -> null
            is NotDone -> next
        }
    }.toList().asReversed()

    fun getStackFrames(): Sequence<StackFrame> = callStack.mapIndexed { i, continuation ->
        val frame = continuation.frame
        val nextIota = registerNewSource(frame)
        StackFrame().apply {
            id = i + 1
            name = "Frame $id (${frame.name})"
            if (nextIota != null) {
                val metadata = iotaMetadata[nextIota]!!
                source = metadata.source
                line = toClientLineNumber(metadata.line)
            }
        }
    }.asReversed().asSequence()

    fun getScopes(frameId: Int): List<Scope> {
        val scopes = mutableListOf(Scope().apply {
            name = "State"
            variablesReference = vm.image.run {
                val variables = mutableListOf(
                    toVariable("Stack", stack.asReversed()),
                    toVariable("Ravenmind", getRavenmind()),
                    toVariable("OpsConsumed", opsConsumed.toString()),
                    toVariable("EscapeNext", escapeNext.toString()),
                )
                if (parenCount > 0) {
                    variables += toVariable("Intro/Retro", parenthesized.map { it.iota })
                }
                variablesAllocator.add(variables)
            }
        })

        val frame = getContinuationFrame(frameId)
        when (frame) {
            is FrameEvaluate -> sequenceOf(
                toVariable("Code", frame.list),
                toVariable("IsMetacasting", frame.isMetacasting.toString()),
            )

            is FrameForEach -> {
                sequenceOf(
                    toVariable("Code", frame.code),
                    toVariable("Data", frame.data),
                    frame.baseStack?.let { toVariable("BaseStack", it) },
                    toVariable("Result", frame.acc),
                ).filterNotNull()
            }

            else -> null
        }?.also {
            scopes += Scope().apply {
                name = frame.name
                variablesReference = variablesAllocator.add(it)
            }
        }

        return scopes
    }

    fun getVariables(reference: Int) = variablesAllocator.getOrEmpty(reference)

    private fun getRavenmind() = vm.image.userData.let {
        if (it.contains(HexAPI.RAVENMIND_USERDATA)) {
            IotaType.deserialize(it.getCompound(HexAPI.RAVENMIND_USERDATA), vm.env.world)
        } else {
            NullIota()
        }
    }

    private fun toVariables(iotas: Iterable<Iota>) = toVariables(iotas.asSequence())

    private fun toVariables(iotas: Sequence<Iota>) = iotas.mapIndexed(::toVariable)

    private fun toVariable(index: Number, iota: Iota) = toVariable("$index", iota)

    private fun toVariable(name: String, iota: Iota): Variable = Variable().apply {
        this.name = name
        type = iota::class.simpleName
        value = when (iota) {
            is ListIota -> {
                variablesReference = allocateVariables(iota.list)
                indexedVariables = iota.list.size()
                "(${iota.list.count()}) ${iotaToString(iota, false)}"
            }

            else -> iotaToString(iota, false)
        }
    }

    private fun toVariable(name: String, iotas: Iterable<Iota>): Variable = Variable().apply {
        this.name = name
        value = ""
        variablesReference = allocateVariables(iotas)
    }

    private fun toVariable(name: String, value: String): Variable = Variable().also {
        it.name = name
        it.value = value
    }

    private fun allocateVariables(iotas: Iterable<Iota>) = variablesAllocator.add(toVariables(iotas))

    fun getSources() = sourceAllocator.asSequence().map { it.first }

    fun getSourceContents(reference: Int) = getSourceContents(sourceAllocator[reference].second)

    private fun getSourceContents(iotas: Iterable<Iota>): String {
        return iotas.joinToString("\n") {
            val indent = iotaMetadata[it]?.indent(launchArgs.indentWidth) ?: ""
            indent + iotaToString(it, true)
        }
    }

    private fun getContinuationFrame(frameId: Int) = callStack.elementAt(frameId - 1).frame

    fun setBreakpoints(sourceReference: Int, sourceBreakpoints: Array<SourceBreakpoint>): List<Breakpoint> {
        val (source, iotas) = sourceAllocator[sourceReference]
        return sourceBreakpoints.map {
            Breakpoint().apply {
                if (toServerLineNumber(it.line) <= iotas.lastIndex) {
                    isVerified = true
                    this.source = source
                    line = it.line
                } else {
                    isVerified = false
                    message = "Line number out of range"
                }
            }
        }
    }

    private fun toServerLineNumber(line: Int) = if (initArgs.linesStartAt1) {
        line - 1
    } else {
        line
    }

    private fun toClientLineNumber(line: Int) = if (initArgs.linesStartAt1) {
        line + 1
    } else {
        line
    }

    fun executeUntilStopped(stopType: StopType? = null): String? {
//        val callStackSize = continuations.count()
//        val lineNumber = currentLineNumber
        while (executeOnce() != null) {
            if (stopType == StopType.STEP_OVER) return "step" // TODO: fix
//            if (isAtBreakpoint) return "breakpoint"
//            val newCallStackSize = continuations.count()
//            when (stopType) {
//                StopType.STEP_OVER -> if (newCallStackSize == callStackSize) {
//                    currentLineNumber = lineNumber + 1
//                    return "step"
//                } else if (newCallStackSize < callStackSize) return "step"
//
//                StopType.STEP_OUT -> if (newCallStackSize < callStackSize) return "step"
//                else -> {}
//            }
        }
        return null
    }

    // Copy of CastingVM.queueExecuteAndWrapIotas to allow stepping by one pattern at a time.
    fun executeOnce(): String? {
        // bind locally so we can do smart casting
        var continuation = nextContinuation
        if (continuation !is NotDone) return null

        variablesAllocator.clear()

        // Begin aggregating info
        val info = CastingVM.TempControllerInfo(earlyExit = false)
        var lastResolutionType = ResolvedPatternType.UNRESOLVED
        do {
            // Take the top of the continuation stack...
            val frame = (continuation as NotDone).frame

            // ensure all of the iotas to be evaluated are mapped to sources
            registerNewSource(frame)

            // ...and execute it.
            // TODO there used to be error checking code here; I'm pretty sure any and all mishaps should already
            // get caught and folded into CastResult by evaluate.
            val castResult = frame.evaluate(continuation.next, world, vm)
            val newImage = castResult.newData

            // handle indentation for generated sources
            if (castResult.resolutionType == ResolvedPatternType.ESCAPED) {
                // if the paren count changed, it was either an introspection or a retrospection
                // in both cases, the pattern that changed the indent level should be at the lower indent level
                val parenCount = min(vm.image.parenCount, (newImage ?: vm.image).parenCount)

                iotaMetadata[castResult.cast]?.parenCount = parenCount
                // TODO: invalidate source (only after pushing the list)
            }

            // Then write all pertinent data back to the harness for the next iteration.
            if (newImage != null) {
                vm.image = newImage
            }
            vm.env.postExecution(castResult)

            if (continuation.frame is FrameEvaluate) {
                onExecute?.invoke(castResult.cast)
            }

            continuation = castResult.continuation
            lastResolutionType = castResult.resolutionType
            try {
                vm.performSideEffects(info, castResult.sideEffects)
            } catch (e: Exception) {
                e.printStackTrace()
                vm.performSideEffects(info, listOf(OperatorSideEffect.DoMishap(MishapInternalException(e), Mishap.Context(null, null))))
            }
            info.earlyExit = info.earlyExit || !lastResolutionType.success
        } while (
            // keep iterating if the current frame succeeded...
            !info.earlyExit
            // and skipNonEvalFrames is enabled...
            && launchArgs.skipNonEvalFrames
            // and we won't execute another eval frame
            && continuation is NotDone
            && continuation.frame !is FrameEvaluate
        )

        nextContinuation = continuation

        return when (continuation) {
            is NotDone -> {
                lastResolutionType = if (lastResolutionType.success) {
                    ResolvedPatternType.EVALUATED
                } else {
                    ResolvedPatternType.ERRORED
                }
                "step"
            }
            is Done -> null
        }
    }

    private fun iotaToString(iota: Iota, isSource: Boolean): String = when (iota) {
        // i feel like hex should have a thing for this...
        is PatternIota -> HexAPI.instance().run {
            when (val lookup = PatternRegistryManifest.matchPattern(iota.pattern, vm.env, false)) {
                is PatternShapeMatch.Normal -> getActionI18n(lookup.key, false)
                is PatternShapeMatch.PerWorld -> getActionI18n(lookup.key, true)
                is PatternShapeMatch.Special -> lookup.handler.name
                is PatternShapeMatch.Nothing -> when (iota.pattern) {
                    SpecialPatterns.INTROSPECTION -> Component.literal("{")
                    SpecialPatterns.RETROSPECTION -> Component.literal("}")
                    SpecialPatterns.CONSIDERATION -> getRawHookI18n(HexAPI.modLoc("escape"))
                    SpecialPatterns.EVANITION -> getRawHookI18n(HexAPI.modLoc("undo"))
                    else -> iota.display()
                }
            }
        }.string

        else -> {
            val result = when (iota) {
                is ListIota -> "[" + iota.list.joinToString { iotaToString(it, isSource) } + "]"
                is GarbageIota -> "Garbage"
                else -> iota.display().string
            }
            if (isSource) {
                "<$result>"
            } else {
                result
            }
        }
    }
}

val ContinuationFrame.name get() = this::class.simpleName ?: "Unknown"

enum class StopType {
    STEP_OVER, STEP_OUT,
}
