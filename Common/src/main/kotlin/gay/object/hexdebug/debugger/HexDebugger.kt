package gay.`object`.hexdebug.debugger

import at.petrak.hexcasting.api.PatternRegistry
import at.petrak.hexcasting.api.spell.Action
import at.petrak.hexcasting.api.spell.SpellList
import at.petrak.hexcasting.api.spell.casting.CastingHarness
import at.petrak.hexcasting.api.spell.casting.CastingHarness.CastResult
import at.petrak.hexcasting.api.spell.casting.ResolvedPatternType
import at.petrak.hexcasting.api.spell.casting.eval.*
import at.petrak.hexcasting.api.spell.casting.eval.SpellContinuation.Done
import at.petrak.hexcasting.api.spell.casting.eval.SpellContinuation.NotDone
import at.petrak.hexcasting.api.spell.casting.sideeffects.OperatorSideEffect
import at.petrak.hexcasting.api.spell.iota.*
import at.petrak.hexcasting.api.spell.math.HexDir
import at.petrak.hexcasting.api.spell.math.HexPattern
import at.petrak.hexcasting.api.spell.mishaps.Mishap
import at.petrak.hexcasting.api.spell.mishaps.MishapInvalidPattern
import at.petrak.hexcasting.api.utils.PatternNameHelper
import at.petrak.hexcasting.common.casting.operators.eval.OpEval
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import gay.`object`.hexdebug.adapter.LaunchArgs
import gay.`object`.hexdebug.casting.eval.*
import gay.`object`.hexdebug.debugger.allocators.SourceAllocator
import gay.`object`.hexdebug.debugger.allocators.VariablesAllocator
import gay.`object`.hexdebug.utils.ceilToPow
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.gameevent.GameEvent
import org.eclipse.lsp4j.debug.*
import java.util.*
import kotlin.math.min
import org.eclipse.lsp4j.debug.LoadedSourceEventArgumentsReason as LoadedSourceReason

class HexDebugger(
    private val initArgs: InitializeRequestArguments,
    private val launchArgs: LaunchArgs,
    private val vm: CastingHarness,
    private val world: ServerLevel,
    private val onExecute: ((Iota) -> Unit)? = null,
    iotas: List<Iota>,
) {
    constructor(
        initArgs: InitializeRequestArguments,
        launchArgs: LaunchArgs,
        castArgs: CastArgs,
    ) : this(initArgs, launchArgs, CastingHarness(castArgs.env), castArgs.world, castArgs.onExecute, castArgs.iotas)

    @Suppress("CAST_NEVER_SUCCEEDS")
    val debugCastEnv = vm.ctx as IMixinCastingContext

    var lastEvaluatedMetadata: IotaMetadata? = null
        private set

    private val variablesAllocator = VariablesAllocator()
    private val sourceAllocator = SourceAllocator(iotas.hashCode())

    private val iotaMetadata = IdentityHashMap<Iota, IotaMetadata>()
    private val frameInvocationMetadata = IdentityHashMap<SpellContinuation, () -> IotaMetadata?>()
    private val virtualFrames = IdentityHashMap<SpellContinuation, MutableList<StackFrame>>()

    private val breakpoints = mutableMapOf<Int, MutableMap<Int, SourceBreakpointMode>>() // source id -> line number
    private val exceptionBreakpoints = mutableSetOf<ExceptionBreakpointType>()

    private val initialSource = registerNewSource(iotas)!!

    private var callStack = listOf<NotDone>()

    // Initialize the continuation stack to a single top-level eval for all iotas.
    private var nextContinuation: SpellContinuation = Done
        set(value) {
            field = value
            callStack = getCallStack(value)
        }

    init {
        nextContinuation = nextContinuation
            .run {
                if (launchArgs.stopOnExit) {
                    // FIXME: scuffed as hell
                    val lastIota = iotas.lastOrNull()
                    val column = lastIota?.let {
                        // +1 so it goes *after* the last character
                        indexToColumn(iotaToString(it, isSource = true).lastIndex + 1)
                    }
                    pushFrame(newFrameBreakpoint(stopBefore = true)).also { newCont ->
                        frameInvocationMetadata[newCont] = {
                            lastIota?.let { iotaMetadata[it] }?.copy(column = column)
                        }
                    }
                } else this
            }
            .pushFrame(FrameEvaluate(SpellList.LList(0, iotas), false))
    }

    private val nextFrame get() = (nextContinuation as? NotDone)?.frame

    private fun registerNewSource(frame: ContinuationFrame): Source? = getIotas(frame)?.let(::registerNewSource)

    private fun registerNewSource(iotas: Iterable<Iota>): Source? {
        val unregisteredIotas = iotas.filter { it !in iotaMetadata }
        if (unregisteredIotas.isEmpty()) return null

        val source = sourceAllocator.add(unregisteredIotas)
        for ((index, iota) in unregisteredIotas.withIndex()) {
            iotaMetadata[iota] = IotaMetadata(source, indexToLineNumber(index))
        }
        return source
    }

    private fun getIotas(frame: ContinuationFrame) = when (frame) {
        is FrameEvaluate -> frame.list
        is FrameForEach -> frame.code
        else -> null
    }

    private fun getFirstIotaMetadata(continuation: NotDone): IotaMetadata? =
        // for FrameEvaluate, show the next iota to be evaluated (if any)
        (continuation.frame as? FrameEvaluate)?.let(::getFirstIotaMetadata)
        // for everything else, show the caller if we have it
        ?: frameInvocationMetadata[continuation]?.invoke()
        // otherwise show the first contained iota
        ?: getFirstIotaMetadata(continuation.frame)

    private fun getFirstIotaMetadata(frame: ContinuationFrame) = getIotas(frame)?.let { iotaMetadata[it.car] }

    // current continuation is last
    private fun getCallStack(current: SpellContinuation) = generateSequence(current as? NotDone) {
        when (val next = it.next) {
            is Done -> null
            is NotDone -> next
        }
    }.toList().asReversed()

    fun getStackFrames(): Sequence<StackFrame> {
        var frameId = 1
        var virtualFrameId = (callStack.size + 1).ceilToPow(10)
        return callStack.flatMap { continuation ->
            listOf(
                StackFrame().apply {
                    id = frameId++
                    name = "[$id] ${continuation.frame.name}"
                    setSourceAndPosition(getFirstIotaMetadata(continuation))
                }
            ) + virtualFrames[continuation]?.map {
                it.apply {
                    id = virtualFrameId++
                    presentationHint = StackFramePresentationHint.SUBTLE
                }
            }.orEmpty()
        }.asReversed().asSequence()
    }

    fun getScopes(frameId: Int): List<Scope> {
        val scopes = mutableListOf(
            Scope().apply {
                name = "Data"
                variablesReference = vm.run {
                    variablesAllocator.add(
                        toVariable("Stack", stack.asReversed()),
                        toVariable("Ravenmind", ravenmind ?: NullIota()),
                    )
                }
            },
            Scope().apply {
                name = "State"
                variablesReference = vm.run {
                    val variables = mutableListOf(
                        toVariable("EscapeNext", escapeNext.toString()),
                        toVariable("ParenCount", parenCount.toString()),
                    )
                    if (parenCount > 0) {
                        variables += toVariable("Parenthesized", parenthesized)
                    }
                    variablesAllocator.add(variables)
                }
            },
        )

        // virtual frames are given ids past the end of the call stack; just ignore them here
        getContinuation(frameId)?.also { continuation ->
            getFrameVariables(continuation)?.also {
                scopes += Scope().apply {
                    name = "Frame"
                    variablesReference = variablesAllocator.add(it)
                }
            }
        }

        return scopes
    }

    fun getVariables(reference: Int) = variablesAllocator.getOrEmpty(reference)

    private fun getFrameVariables(continuation: NotDone): Sequence<Variable>? {
        val sourceLine = getFirstIotaMetadata(continuation)?.toString() ?: ""
        return when (val frame = continuation.frame) {
            is FrameEvaluate -> if (frame.isFrameBreakpoint) {
                sequenceOf(
                    toVariable("StopBefore", frame.stopBefore.toString()),
                    toVariable("IsFatal", frame.isFatal.toString()),
                )
            } else {
                sequenceOf(
                    toVariable("Code", frame.list, sourceLine),
                    toVariable("IsMetacasting", frame.isMetacasting.toString()),
                )
            }

            is FrameForEach -> sequenceOf(
                toVariable("Code", frame.code, sourceLine),
                toVariable("Data", frame.data),
                frame.baseStack?.let { toVariable("BaseStack", it) },
                toVariable("Result", frame.acc),
            ).filterNotNull()

            else -> if (sourceLine.isNotEmpty()) sequenceOf(
                toVariable("Code", sourceLine),
            ) else null
        }
    }

    private fun toVariables(iotas: Iterable<Iota>) = toVariables(iotas.asSequence())

    private fun toVariables(iotas: Sequence<Iota>) = iotas.mapIndexed(::toVariable)

    private fun toVariable(index: Number, iota: Iota) = toVariable("$index", iota)

    private fun toVariable(variableName: String, iota: Iota): Variable = Variable().apply {
        name = variableName
        type = iota::class.simpleName
        when (iota) {
            is ListIota -> {
                value = "(${iota.list.count()}) ${iotaToString(iota)}"
                variablesReference = allocateVariables(iota.list)
                indexedVariables = iota.list.size()
            }

            else -> value = iotaToString(iota)
        }
    }

    private fun toVariable(name: String, iotas: Iterable<Iota>, value: String = ""): Variable = Variable().also {
        it.name = name
        it.value = value
        it.variablesReference = allocateVariables(iotas)
    }

    private fun toVariable(name: String, value: String): Variable = Variable().also {
        it.name = name
        it.value = value
    }

    private fun allocateVariables(iotas: Iterable<Iota>) = variablesAllocator.add(toVariables(iotas))

    fun getSources() = sourceAllocator.map { it.first }

    fun getSourceContents(reference: Int): String? = sourceAllocator[reference]?.second?.let(::getSourceContents)

    private fun getSourceContents(iotas: Iterable<Iota>): String {
        return iotas.joinToString("\n") {
            val indent = iotaMetadata[it]?.indent(launchArgs.indentWidth) ?: ""
            indent + iotaToString(it, true)
        }
    }

    private fun getContinuation(frameId: Int) = callStack.elementAtOrNull(frameId - 1)

    // TODO: gross.
    // TODO: there's probably a bug here somewhere - shouldn't we be using the metadata?
    fun setBreakpoints(sourceReference: Int, sourceBreakpoints: Array<SourceBreakpoint>): List<Breakpoint> {
        val (source, iotas) = sourceAllocator[sourceReference] ?: (null to null)
        val breakpointLines = breakpoints.getOrPut(sourceReference, ::mutableMapOf).apply { clear() }
        return sourceBreakpoints.map {
            Breakpoint().apply {
                isVerified = false
                if (source == null || iotas == null) {
                    message = "Unknown source"
                    reason = BreakpointNotVerifiedReason.PENDING  // TODO: send Breakpoint event later
                } else if (it.line > indexToLineNumber(iotas.lastIndex)) {
                    message = "Line number out of range"
                    reason = BreakpointNotVerifiedReason.FAILED
                } else {
                    isVerified = true
                    this.source = source
                    line = it.line

                    breakpointLines[it.line] = it.mode
                        ?.let(SourceBreakpointMode::valueOf)
                        ?: SourceBreakpointMode.EVALUATED
                }
            }
        }
    }

    fun setExceptionBreakpoints(typeNames: Array<String>): List<Breakpoint> {
        exceptionBreakpoints.clear()
        return typeNames.map {
            exceptionBreakpoints.add(ExceptionBreakpointType.valueOf(it))
            Breakpoint().apply { isVerified = true }
        }
    }

    private fun indexToLineNumber(index: Int) = index + if (initArgs.linesStartAt1) 1 else 0

    private fun indexToColumn(index: Int) = index + if (initArgs.columnsStartAt1) 1 else 0

    private fun isAtBreakpoint(): Boolean {
        val nextIota = when (val frame = nextFrame) {
            is FrameEvaluate -> if (frame.isFrameBreakpoint) {
                return true
            } else {
                getIotas(frame)?.car
            }
            else -> null
        } ?: return false

        val breakpointMode = iotaMetadata[nextIota]
            ?.let { breakpoints[it.source.sourceReference]?.get(it.line) }
            ?: return false

        val escapeNext = vm.escapeNext || vm.parenCount > 0

        return when (breakpointMode) {
            SourceBreakpointMode.EVALUATED -> !escapeNext
            SourceBreakpointMode.ESCAPED -> escapeNext
            SourceBreakpointMode.ALL -> true
        }
    }

    fun start(): DebugStepResult? {
        val loadedSources = mapOf(initialSource to LoadedSourceReason.NEW)
        return if (launchArgs.stopOnEntry) {
            DebugStepResult("entry", loadedSources = loadedSources)
        } else if (isAtBreakpoint()) {
            DebugStepResult("breakpoint", loadedSources = loadedSources)
        } else {
            executeUntilStopped()?.withLoadedSources(loadedSources)
        }
    }

    fun executeUntilStopped(stepType: RequestStepType? = null): DebugStepResult? {
        var lastResult: DebugStepResult? = null
        var isEscaping: Boolean? = null
        var stepDepth = 0
        var shouldStop = false
        var hitBreakpoint = false

        while (true) {
            var result = executeOnce(exactlyOnce = true) ?: return null
            if (lastResult != null) result += lastResult
            lastResult = result

            if (isAtBreakpoint()) {
                hitBreakpoint = true
            }

            if (hitBreakpoint && shouldStopAtFrame(nextContinuation)) {
                return result.copy(reason = "breakpoint")
            }

            // if stepType is null, we should ONLY stop on breakpoints
            if (stepType == null) continue

            // alwinfy says: "beware Iris very much"
            if (result.type == DebugStepType.JUMP) {
                shouldStop = true
            }

            stepDepth += when (result.type) {
                DebugStepType.IN -> 1
                DebugStepType.OUT -> -1
                else -> 0
            }

            if (isEscaping == null) {
                isEscaping = result.type == DebugStepType.ESCAPE
            }

            shouldStop = shouldStop || if (isEscaping) {
                result.type != DebugStepType.ESCAPE
            } else when (stepType) {
                RequestStepType.OVER ->  stepDepth <= 0
                RequestStepType.OUT -> stepDepth < 0
            }

            if (shouldStop && shouldStopAtFrame(nextContinuation)) {
                return result
            }
        }
    }

    // Copy of CastingHarness.queueExecuteAndWrapIotas to allow stepping by one pattern at a time.
    fun executeOnce(exactlyOnce: Boolean = false): DebugStepResult? {
        var continuation = nextContinuation // bind locally so we can do smart casting
        if (continuation !is NotDone) return null

        var stepResult = DebugStepResult("step")

        variablesAllocator.clear()

        // Begin aggregating info
        val info = CastingHarness.TempControllerInfo(earlyExit = false)
        var sound = HexEvalSounds.NOTHING
        while (continuation is NotDone && !info.earlyExit) {
            debugCastEnv.reset()

            // Take the top of the continuation stack...
            val frame = continuation.frame

            // TODO: there's probably a less hacky way to do this
            if (frame is FrameEvaluate && frame.isFrameBreakpoint && frame.isFatal) {
                continuation = Done
                break
            }

            // ...and execute it.
            val castResult = try {
                frame.evaluate(continuation.next, world, vm)
            } catch (mishap: Mishap) {
                val pattern = getPatternForFrame(frame)
                val operator = try {
                    getOperatorForFrame(frame, world)
                } catch (e: Throwable) {
                    null
                }
                CastResult(
                    continuation,
                    null,
                    mishap.resolutionType(vm.ctx),
                    listOf(
                        OperatorSideEffect.DoMishap(
                            mishap,
                            Mishap.Context(pattern ?: HexPattern(HexDir.WEST), operator)
                        )
                    ),
                    HexEvalSounds.MISHAP,
                )
            }

            val newImage = castResult.newData

            // if something went wrong, push a breakpoint or stop immediately
            // and save the old image so we can see the stack BEFORE the mishap instead of after
            // TODO: maybe implement a way to see the stack at each call frame instead?
            val (newContinuation, preMishapImage) = if (castResult.resolutionType.success) {
                Pair(castResult.continuation, null)
            } else if (ExceptionBreakpointType.UNCAUGHT_MISHAPS in exceptionBreakpoints) {
                stepResult = stepResult.copy(reason = "exception")
                Pair(castResult.continuation.pushFrame(newFrameBreakpointFatal()), vm.getFunctionalData())
            } else {
                Pair(Done, vm.getFunctionalData())
            }

            // TODO: scuffed
            val cast = if (frame is FrameEvaluate && frame.list.nonEmpty) {
                frame.list.car
            } else NullIota()

            // we use this when printing to the console, so this should happen BEFORE vm.env.postExecution (ie. for mishaps)
            lastEvaluatedMetadata = iotaMetadata[cast]

            // Then write all pertinent data back to the harness for the next iteration.
            if (newImage != null) {
                stepResult = handleIndent(cast, castResult, vm.getFunctionalData(), newImage, stepResult)
                vm.applyFunctionalData(newImage)
            }

            if (castResult.resolutionType == ResolvedPatternType.EVALUATED) {
                onExecute?.invoke(cast)
            }

            val stepType = getStepType(castResult, continuation, newContinuation)
            if (newContinuation is NotDone) {
                setIotaOverrides(cast, continuation, newContinuation, stepType)

                // ensure all of the iotas to be evaluated in the next frame are mapped to a source
                registerNewSource(newContinuation.frame)?.also {
                    stepResult = stepResult.withLoadedSource(it, LoadedSourceReason.NEW)
                }

                // insert a virtual FrameFinishEval if OpEval didn't (ie. if we did a TCO)
                if (launchArgs.showTailCallFrames && debugCastEnv.`lastEvaluatedAction$hexdebug` is OpEval) {
                    val invokeMeta = iotaMetadata[cast]
                    val nextInvokeMeta = frameInvocationMetadata[newContinuation.next]?.invoke()
                    if (invokeMeta != null && invokeMeta != nextInvokeMeta) {
                        virtualFrames.getOrPut(continuation.next) { mutableListOf() }.add(
                            StackFrame().apply {
                                name = "FrameFinishEval"
                                setSourceAndPosition(invokeMeta)
                            }
                        )
                    }
                }
            }
            if (stepType != null) {
                stepResult = stepResult.copy(type = stepType)
            }

            continuation = newContinuation

            vm.performSideEffects(info, castResult.sideEffects)
            info.earlyExit = info.earlyExit || !castResult.resolutionType.success
            sound = if (castResult.sound == HexEvalSounds.MISHAP) {
                HexEvalSounds.MISHAP
            } else {
                sound.greaterOf(castResult.sound)
            }

            // this needs to be after performSideEffects, since that's where mishaps mess with the stack
            // (TODO: is that still true in 1.19?)
            if (preMishapImage != null) vm.applyFunctionalData(preMishapImage)

            if (exactlyOnce || shouldStopAtFrame(continuation)) {
                break
            }
        }

        sound.sound?.let {
            vm.ctx.world.playSound(
                null, vm.ctx.position.x, vm.ctx.position.y, vm.ctx.position.z, it,
                SoundSource.PLAYERS, 1f, 1f
            )
            // TODO: is it worth mixing in to the immut map and making our own game event with blackjack and hookers
            vm.ctx.world.gameEvent(vm.ctx.caster, GameEvent.ITEM_INTERACT_FINISH, vm.ctx.position)
        }

        // never show virtual frames above the top of the call stack
        virtualFrames[continuation]?.clear()

        nextContinuation = continuation

        return when (continuation) {
            is NotDone -> stepResult
            is Done -> null
        }
    }

    // directly copied from CastingHarness
    private fun getOperatorForPattern(iota: Iota, world: ServerLevel): Action? {
        if (iota is PatternIota)
            return PatternRegistry.matchPattern(iota.pattern, world)
        return null
    }

    // directly copied from CastingHarness
    private fun getPatternForFrame(frame: ContinuationFrame): HexPattern? {
        if (frame !is FrameEvaluate) return null

        return (frame.list.car as? PatternIota)?.pattern
    }

    // directly copied from CastingHarness
    private fun getOperatorForFrame(frame: ContinuationFrame, world: ServerLevel): Action? {
        if (frame !is FrameEvaluate) return null

        return getOperatorForPattern(frame.list.car, world)
    }

    private fun shouldStopAtFrame(continuation: SpellContinuation) =
        continuation !is NotDone || shouldStopAtFrame(continuation.frame)

    /**
     * Returns false if this frame is "internal", ie. it normally wouldn't be helpful to pause the debugger when
     * we reach it. Always returns true if `skipNonEvalFrames` is disabled. Does not take breakpoints into account.
     */
    private fun shouldStopAtFrame(frame: ContinuationFrame): Boolean {
        if (!launchArgs.skipNonEvalFrames) return true
        return when (frame) {
            is FrameEvaluate -> !frame.isFrameBreakpoint || frame.stopBefore
            else -> false
        }
    }

    private fun handleIndent(
        cast: Iota,
        castResult: CastResult,
        oldImage: FunctionalData,
        newImage: FunctionalData,
        stepResult: DebugStepResult,
    ) = when (castResult.resolutionType) {
        ResolvedPatternType.ESCAPED -> {
            // if the paren count changed, it was either an introspection or a retrospection
            // in both cases, the pattern that changed the indent level should be at the lower indent level
            val parenCount = min(oldImage.parenCount, newImage.parenCount)
            iotaMetadata[cast]?.trySetParenCount(parenCount)
            stepResult
        }

        ResolvedPatternType.EVALUATED -> if (newImage.parenCount == 0 && newImage.parenthesized.isEmpty() && oldImage.parenthesized.isNotEmpty()) {
            // closed list
            val sources = oldImage.parenthesized.asSequence().mapNotNull { iotaMetadata[it] }
                .filter { it.needsReload.also { _ -> it.needsReload = false } }
                .associate { it.source to LoadedSourceReason.CHANGED }

            stepResult.withLoadedSources(sources)
        } else stepResult

        else -> stepResult
    }

    private fun getStepType(
        castResult: CastResult,
        continuation: NotDone,
        newContinuation: SpellContinuation,
    ): DebugStepType? {
        val isEscaped = when (castResult.resolutionType) {
            ResolvedPatternType.ESCAPED -> true
            ResolvedPatternType.EVALUATED -> (castResult.newData?.parenCount ?: 0) > 0
            else -> false
        }
        if (isEscaped) {
            return DebugStepType.ESCAPE
        }

        if (newContinuation !is NotDone) {
            return null
        }

        if (newContinuation === continuation.next) {
            // don't emit OUT when finishing a Thoth inner loop
            if (newContinuation.frame is FrameForEach) {
                return null
            }
            return DebugStepType.OUT
        }

        if (continuation.next !== newContinuation.next || continuation.frame::class != newContinuation.frame::class) {
            // don't emit IN when starting a Thoth inner loop
            if (continuation.frame is FrameForEach) {
                return null
            }
            return DebugStepType.IN
        }

        return debugCastEnv.`lastDebugStepType$hexdebug`
    }

    private fun setIotaOverrides(
        cast: Iota,
        continuation: NotDone,
        newContinuation: NotDone,
        stepType: DebugStepType?,
    ) {
        val nextContinuation = newContinuation.next
        val frame = continuation.frame
        val newFrame = newContinuation.frame
        val nextFrame = nextContinuation.frame

        if (stepType == DebugStepType.IN) {
            trySetIotaOverride(newContinuation, cast)
            if (nextFrame !is FrameEvaluate) {
                trySetIotaOverride(nextContinuation, cast)
            }
        } else if (
            frame is FrameForEach
            && newFrame is FrameEvaluate
            && nextFrame is FrameForEach
            && frame.code === newFrame.list
            && frame.code === nextFrame.code
        ) {
            // carry over thoth metadata between iterations
            frameInvocationMetadata[nextContinuation] = frameInvocationMetadata[continuation]
        }
    }

    private fun trySetIotaOverride(continuation: SpellContinuation, cast: Iota): Boolean {
        return if (continuation !in frameInvocationMetadata && continuation is NotDone) {
            frameInvocationMetadata[continuation] = { iotaMetadata[cast] }
            true
        } else false
    }

    private fun iotaToString(iota: Iota, isSource: Boolean = false): String = when (iota) {
        is PatternIota -> try {
            PatternRegistry.matchPattern(iota.pattern, vm.ctx.world).displayName
        } catch (e: MishapInvalidPattern) {
            PatternNameHelper.representationForPattern(iota.pattern)
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

val SpellContinuation.frame get() = (this as? NotDone)?.frame

val SpellContinuation.next get() = (this as? NotDone)?.next

val ContinuationFrame.name get() = this::class.simpleName ?: "Unknown"

enum class RequestStepType {
    OVER,
    OUT,
}
