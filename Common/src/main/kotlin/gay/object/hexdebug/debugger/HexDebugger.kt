package gay.`object`.hexdebug.debugger

import at.petrak.hexcasting.api.PatternRegistry
import at.petrak.hexcasting.api.spell.Action
import at.petrak.hexcasting.api.spell.SpellList
import at.petrak.hexcasting.api.spell.casting.*
import at.petrak.hexcasting.api.spell.casting.CastingHarness.CastResult
import at.petrak.hexcasting.api.spell.casting.eval.*
import at.petrak.hexcasting.api.spell.casting.eval.SpellContinuation.Done
import at.petrak.hexcasting.api.spell.casting.eval.SpellContinuation.NotDone
import at.petrak.hexcasting.api.spell.casting.sideeffects.EvalSound
import at.petrak.hexcasting.api.spell.casting.sideeffects.OperatorSideEffect
import at.petrak.hexcasting.api.spell.iota.Iota
import at.petrak.hexcasting.api.spell.iota.ListIota
import at.petrak.hexcasting.api.spell.iota.NullIota
import at.petrak.hexcasting.api.spell.iota.PatternIota
import at.petrak.hexcasting.api.spell.math.HexDir
import at.petrak.hexcasting.api.spell.math.HexPattern
import at.petrak.hexcasting.api.spell.mishaps.Mishap
import at.petrak.hexcasting.common.casting.operators.eval.OpEval
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import gay.`object`.hexdebug.adapter.LaunchArgs
import gay.`object`.hexdebug.casting.eval.*
import gay.`object`.hexdebug.debugger.allocators.SourceAllocator
import gay.`object`.hexdebug.debugger.allocators.VariablesAllocator
import gay.`object`.hexdebug.utils.ceilToPow
import gay.`object`.hexdebug.utils.displayWithPatternName
import gay.`object`.hexdebug.utils.toHexpatternSource
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.gameevent.GameEvent
import org.eclipse.lsp4j.debug.*
import java.util.*
import kotlin.math.min
import org.eclipse.lsp4j.debug.LoadedSourceEventArgumentsReason as LoadedSourceReason

class HexDebugger(
    var initArgs: InitializeRequestArguments,
    var launchArgs: LaunchArgs,
    private val defaultEnv: CastingContext,
    private val world: ServerLevel,
    private val onExecute: ((Iota) -> Unit)? = null,
    iotas: List<Iota>,
    private var image: FunctionalData = emptyFunctionalData(),
) {
    constructor(
        initArgs: InitializeRequestArguments,
        launchArgs: LaunchArgs,
        castArgs: CastArgs,
        image: FunctionalData = emptyFunctionalData(),
    ) : this(initArgs, launchArgs, castArgs.env, castArgs.world, castArgs.onExecute, castArgs.iotas, image)

    var lastEvaluatedMetadata: IotaMetadata? = null
        private set

    // ensure we passed a debug cast env to help catch errors early
    init {
        @Suppress("CAST_NEVER_SUCCEEDS")
        if (!(defaultEnv as IMixinCastingContext).`isDebugging$hexdebug`) {
            throw IllegalArgumentException("defaultEnv.isDebugging\$hexdebug must be true")
        }
    }

    private val variablesAllocator = VariablesAllocator()
    private val sourceAllocator = SourceAllocator(iotas.hashCode())

    private val iotaMetadata = IdentityHashMap<Iota, IotaMetadata>()
    // FIXME: this is really terrible and gross and i don't like it
    private val frameInvocationMetadata = IdentityHashMap<SpellContinuation, () -> Pair<Iota, IotaMetadata?>?>()
    private val virtualFrames = IdentityHashMap<SpellContinuation, MutableList<StackFrame>>()

    private val breakpoints = mutableMapOf<Int, MutableMap<Int, SourceBreakpointMode>>() // source id -> line number
    private val exceptionBreakpoints = mutableSetOf<ExceptionBreakpointType>()

    private val initialSource = registerNewSource(iotas)!!

    private var callStack = listOf<NotDone>()

    val evaluatorUIPatterns = mutableListOf<ResolvedPattern>()

    private var evaluatorResetData: EvaluatorResetData? = null

    private var isAtCaughtMishap = false

    private var lastResolutionType = ResolvedPatternType.UNRESOLVED

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
                    val columnIndex = lastIota?.let {
                        // +1 so it goes *after* the last character
                        iotaToString(it, isSource = true).lastIndex + 1
                    }
                    pushFrame(newFrameBreakpoint(stopBefore = true)).also { newCont ->
                        frameInvocationMetadata[newCont] = {
                            lastIota?.let { it to iotaMetadata[it]?.copy(columnIndex = columnIndex) }
                        }
                    }
                } else this
            }
            .pushFrame(FrameEvaluate(SpellList.LList(0, iotas), false))
    }

    private val nextFrame get() = (nextContinuation as? NotDone)?.frame

    private fun getVM(env: CastingContext? = null) = CastingHarness(env ?: defaultEnv).apply {
        applyFunctionalData(image)
    }

    private fun registerNewSource(frame: ContinuationFrame): Source? = getIotas(frame)?.let(::registerNewSource)

    private fun registerNewSource(iotas: Iterable<Iota>): Source? {
        val unregisteredIotas = iotas.filter { it !in iotaMetadata }
        if (unregisteredIotas.isEmpty()) return null

        val source = sourceAllocator.add(unregisteredIotas)
        for ((index, iota) in unregisteredIotas.withIndex()) {
            iotaMetadata[iota] = IotaMetadata(source, index)
        }
        return source
    }

    private fun getIotas(frame: ContinuationFrame) = when (frame) {
        is FrameEvaluate -> if (frame.isFrameBreakpoint) null else frame.list
        is FrameForEach -> frame.code
        else -> null
    }

    private fun getFirstIotaMetadata(continuation: NotDone): Pair<Iota, IotaMetadata?>? =
        // for FrameEvaluate, show the next iota to be evaluated (if any)
        (continuation.frame as? FrameEvaluate)?.let(::getFirstIotaMetadata)
        // for everything else, show the caller if we have it
        ?: frameInvocationMetadata[continuation]?.invoke()
        // otherwise show the first contained iota
        ?: getFirstIotaMetadata(continuation.frame)

    private fun getFirstIotaMetadata(frame: ContinuationFrame) = getIotas(frame)?.let { it.car to iotaMetadata[it.car] }

    // current continuation is last
    private fun getCallStack(current: SpellContinuation) = generateSequence(current as? NotDone) {
        when (val next = it.next) {
            is Done -> null
            is NotDone -> next
        }
    }.toList().asReversed()

    /** (iota, index) */
    fun getNextIotaToEvaluate(): Pair<String, Int>? {
        val continuation = nextContinuation as? NotDone ?: return null
        val (iota, meta) = getFirstIotaMetadata(continuation) ?: return null
        return iotaToString(iota, isSource = false) to (meta?.lineIndex ?: -1)
    }

    fun getStackFrames(): Sequence<StackFrame> {
        var frameId = 1
        var virtualFrameId = (callStack.size + 1).ceilToPow(10)
        return callStack.flatMap { continuation ->
            listOf(
                StackFrame().apply {
                    id = frameId++
                    name = "[$id] ${continuation.frame.name}"
                    setSourceAndPosition(initArgs, getFirstIotaMetadata(continuation)?.second)
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
                variablesReference = image.run {
                    variablesAllocator.add(
                        toVariable("Stack", stack.asReversed()),
                        toVariable("Ravenmind", ravenmind ?: NullIota()),
                    )
                }
            },
            Scope().apply {
                name = "State"
                variablesReference = image.run {
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
                } else if (it.line > initArgs.indexToLine(iotas.lastIndex)) {
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
            ?.let { breakpoints[it.source.sourceReference]?.get(it.line(initArgs)) }
            ?: return false

        val escapeNext = image.escapeNext || image.parenCount > 0

        return when (breakpointMode) {
            SourceBreakpointMode.EVALUATED -> !escapeNext
            SourceBreakpointMode.ESCAPED -> escapeNext
            SourceBreakpointMode.ALL -> true
        }
    }

    fun generateDescs() = getVM().generateDescs()

    fun getClientView() = getClientView(getVM())

    private fun getClientView(vm: CastingHarness): ControllerInfo {
        val (stackDescs, parenthesized, ravenmind) = vm.generateDescs()
        val isStackClear = nextContinuation is Done // only close the window if we're done evaluating
        return ControllerInfo(isStackClear, lastResolutionType, stackDescs, parenthesized, ravenmind, vm.parenCount)
    }

    /**
     * Use [DebugAdapter.evaluate][gay.object.hexdebug.adapter.DebugAdapter.evaluate] instead.
     */
    internal fun evaluate(env: CastingContext, list: SpellList): DebugStepResult {
        val vm = getVM(env)

        if (isAtCaughtMishap) {
            playSound(vm, HexEvalSounds.MISHAP)
            return DebugStepResult(StopReason.EXCEPTION, clientInfo = getClientView(vm))
        }

        val startedEvaluating = evaluatorResetData == null
        if (startedEvaluating) {
            evaluatorResetData = EvaluatorResetData(nextContinuation, image, lastResolutionType, isAtCaughtMishap)
        }

        nextContinuation = nextContinuation.pushFrame(FrameEvaluate(list, false))
        return executeNextDebugStep(vm, doStaffMishaps = true).copy(startedEvaluating = startedEvaluating)
    }

    /**
     * Use [DebugAdapter.resetEvaluator][gay.object.hexdebug.adapter.DebugAdapter.resetEvaluator] instead.
     */
    internal fun resetEvaluator() = (evaluatorResetData != null).also { _ ->
        evaluatorResetData?.also {
            nextContinuation = it.continuation
            image = it.image
            lastResolutionType = it.lastResolutionType
            isAtCaughtMishap = it.isAtCaughtMishap
        }
        evaluatorResetData = null
        evaluatorUIPatterns.clear()
    }

    fun start(): DebugStepResult {
        return if (launchArgs.stopOnEntry) {
            DebugStepResult(StopReason.STARTED)
        } else if (isAtBreakpoint()) {
            DebugStepResult(StopReason.BREAKPOINT)
        } else {
            executeUntilStopped()
        }.withLoadedSource(initialSource, LoadedSourceReason.NEW)
    }

    fun executeUntilStopped(stepType: RequestStepType? = null): DebugStepResult {
        val vm = getVM()
        var lastResult: DebugStepResult? = null
        var isEscaping: Boolean? = null
        var stepDepth = 0
        var shouldStop = false
        var hitBreakpoint = false

        while (true) {
            var result = executeNextDebugStep(vm, exactlyOnce = true)
            if (lastResult != null) result += lastResult
            lastResult = result

            if (result.reason.stopImmediately) return result

            if (isAtBreakpoint()) {
                hitBreakpoint = true
            }

            if (hitBreakpoint && shouldStopAtFrame(nextContinuation)) {
                return result.copy(reason = StopReason.BREAKPOINT)
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

    fun executeOnce() = executeNextDebugStep(getVM())

    // Copy of CastingHarness.queueExecuteAndWrapIotas to allow stepping by one pattern at a time.
    private fun executeNextDebugStep(
        vm: CastingHarness,
        exactlyOnce: Boolean = false,
        doStaffMishaps: Boolean = false,
    ): DebugStepResult {
        var stepResult = DebugStepResult(StopReason.STEP)

        var continuation = nextContinuation // bind locally so we can do smart casting
        if (continuation !is NotDone) return stepResult.done()

        variablesAllocator.clear()

        // Begin aggregating info
        val info = CastingHarness.TempControllerInfo(earlyExit = false)
        var sound = HexEvalSounds.NOTHING
        while (continuation is NotDone && !info.earlyExit) {
            vm.debugCastEnv.reset()

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
            val (newContinuation, preMishapImage) = if (castResult.resolutionType.success || doStaffMishaps) {
                Pair(castResult.continuation, null)
            } else if (ExceptionBreakpointType.UNCAUGHT_MISHAPS in exceptionBreakpoints) {
                isAtCaughtMishap = true
                stepResult = stepResult.copy(reason = StopReason.EXCEPTION)
                Pair(castResult.continuation.pushFrame(newFrameBreakpointFatal()), vm.getFunctionalData())
            } else {
                Pair(Done, null)
            }

            // TODO: scuffed
            val cast = if (frame is FrameEvaluate && !frame.isFrameBreakpoint && frame.list.nonEmpty) {
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

            val stepType = getStepType(vm, castResult, continuation, newContinuation)
            if (newContinuation is NotDone) {
                setIotaOverrides(cast, continuation, newContinuation, stepType)

                // ensure all of the iotas to be evaluated in the next frame are mapped to a source
                registerNewSource(newContinuation.frame)?.also {
                    stepResult = stepResult.withLoadedSource(it, LoadedSourceReason.NEW)
                }

                // insert a virtual FrameFinishEval if OpEval didn't (ie. if we did a TCO)
                if (launchArgs.showTailCallFrames && vm.debugCastEnv.`lastEvaluatedAction$hexdebug` is OpEval) {
                    val invokeMeta = iotaMetadata[cast]
                    val nextInvokeMeta = frameInvocationMetadata[newContinuation.next]?.invoke()?.second
                    if (invokeMeta != null && invokeMeta != nextInvokeMeta) {
                        virtualFrames.getOrPut(continuation.next) { mutableListOf() }.add(
                            StackFrame().apply {
                                name = "FrameFinishEval"
                                setSourceAndPosition(initArgs, invokeMeta)
                            }
                        )
                    }
                }
            }
            if (stepType != null) {
                stepResult = stepResult.copy(type = stepType)
            }

            continuation = newContinuation
            lastResolutionType = castResult.resolutionType

            vm.performSideEffects(info, castResult.sideEffects)
            info.earlyExit = info.earlyExit || !castResult.resolutionType.success
            sound = if (castResult.sound == HexEvalSounds.MISHAP) {
                HexEvalSounds.MISHAP
            } else {
                sound.greaterOf(castResult.sound)
            }

            // this needs to be after performSideEffects, since that's where mishaps mess with the stack
            // (TODO: is that still true in 1.19?)
            // note: preMishapImage is only non-null if we actually encountered a mishap
            if (preMishapImage != null) vm.applyFunctionalData(preMishapImage)

            if (exactlyOnce || shouldStopAtFrame(continuation)) {
                break
            }
        }

        playSound(vm, sound)

        // never show virtual frames above the top of the call stack
        virtualFrames[continuation]?.clear()

        nextContinuation = continuation
        image = vm.getFunctionalData()

        return when (continuation) {
            is Done -> stepResult.done()
            is NotDone -> stepResult
        }.copy(clientInfo = getClientView(vm))
    }

    private fun playSound(vm: CastingHarness, sound: EvalSound) {
        sound.sound?.let {
            vm.ctx.world.playSound(
                null, vm.ctx.position.x, vm.ctx.position.y, vm.ctx.position.z, it,
                SoundSource.PLAYERS, 1f, 1f
            )
            // TODO: is it worth mixing in to the immut map and making our own game event with blackjack and hookers
            vm.ctx.world.gameEvent(vm.ctx.caster, GameEvent.ITEM_INTERACT_FINISH, vm.ctx.position)
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
        if (frame !is FrameEvaluate || frame.isFrameBreakpoint) return null

        return (frame.list.car as? PatternIota)?.pattern
    }

    // directly copied from CastingHarness
    private fun getOperatorForFrame(frame: ContinuationFrame, world: ServerLevel): Action? {
        if (frame !is FrameEvaluate || frame.isFrameBreakpoint) return null

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
        vm: CastingHarness,
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

        return vm.debugCastEnv.`lastDebugStepType$hexdebug`
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
            if (nextFrame !is FrameEvaluate || nextFrame.isFrameBreakpoint) {
                trySetIotaOverride(nextContinuation, cast)
            }
        } else if (
            frame is FrameForEach
            && newFrame is FrameEvaluate
            && !newFrame.isFrameBreakpoint
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
            frameInvocationMetadata[continuation] = { cast to iotaMetadata[cast] }
            true
        } else false
    }

    private fun iotaToString(iota: Iota, isSource: Boolean = false): String = if (isSource) {
        iota.toHexpatternSource(world)
    } else {
        iota.displayWithPatternName(world).string
    }

    data class EvaluatorResetData(
        val continuation: SpellContinuation,
        val image: FunctionalData,
        val lastResolutionType: ResolvedPatternType,
        val isAtCaughtMishap: Boolean,
    )
}

val SpellContinuation.frame get() = (this as? NotDone)?.frame

val SpellContinuation.next get() = (this as? NotDone)?.next

val ContinuationFrame.name get() = this::class.simpleName ?: "Unknown"

enum class RequestStepType {
    OVER,
    OUT,
}

fun emptyFunctionalData() = FunctionalData(listOf(), 0, listOf(), false, null)
