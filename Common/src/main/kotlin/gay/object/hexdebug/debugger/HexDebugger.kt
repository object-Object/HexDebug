package gay.`object`.hexdebug.debugger

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.casting.PatternShapeMatch
import at.petrak.hexcasting.api.casting.SpellList
import at.petrak.hexcasting.api.casting.eval.CastResult
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
import gay.`object`.hexdebug.adapter.CastArgs
import gay.`object`.hexdebug.adapter.LaunchArgs
import gay.`object`.hexdebug.casting.eval.DebugCastEnv
import gay.`object`.hexdebug.casting.eval.FrameBreakpoint
import gay.`object`.hexdebug.debugger.allocators.SourceAllocator
import gay.`object`.hexdebug.debugger.allocators.VariablesAllocator
import gay.`object`.hexdebug.registry.HexDebugActions
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import org.eclipse.lsp4j.debug.*
import java.util.*
import kotlin.math.min
import org.eclipse.lsp4j.debug.LoadedSourceEventArgumentsReason as LoadedSourceReason

class HexDebugger(
    private val initArgs: InitializeRequestArguments,
    private val launchArgs: LaunchArgs,
    private val vm: CastingVM,
    private val world: ServerLevel,
    private val onExecute: ((Iota) -> Unit)? = null,
    iotas: List<Iota>,
) {
    constructor(
        initArgs: InitializeRequestArguments,
        launchArgs: LaunchArgs,
        castArgs: CastArgs,
    ) : this(initArgs, launchArgs, CastingVM.empty(castArgs.env), castArgs.world, castArgs.onExecute, castArgs.iotas)

    init {
        if (vm.env !is DebugCastEnv) {
            throw IllegalArgumentException("HexDebugger requires env to implement DebugCastEnv, but got ${vm.env}")
        }
    }

    // Initialize the continuation stack to a single top-level eval for all iotas.
    private var nextContinuation = Done.pushFrame(FrameEvaluate(SpellList.LList(0, iotas), false))
        set(value) {
            field = value
            callStack = getCallStack(value)
        }

    private var callStack = getCallStack(nextContinuation)

    private val variablesAllocator = VariablesAllocator()
    private val sourceAllocator = SourceAllocator(iotas.hashCode())
    private val iotaMetadata = IdentityHashMap<Iota, IotaMetadata>()
    private val frameIotaOverrides = IdentityHashMap<ContinuationFrame, Iota>()
    private val breakpoints = mutableMapOf<Int, MutableSet<Int>>() // source id -> line number

    private val nextFrame get() = (nextContinuation as? NotDone)?.frame

    private val initialSource = registerNewSource(iotas)!!

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

    private fun getFirstIotaMetadata(frame: ContinuationFrame): IotaMetadata? {
        if (frame in frameIotaOverrides) {
            return iotaMetadata[frameIotaOverrides[frame]]
        }
        val iotas = getIotas(frame) ?: return null
        return iotaMetadata[iotas.car]
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
        StackFrame().apply {
            id = i + 1
            name = "Frame $id (${frame.name})"
            getFirstIotaMetadata(frame)?.also {
                source = it.source
                line = it.line
            }
        }
    }.asReversed().asSequence()

    fun getScopes(frameId: Int): List<Scope> {
        val scopes = mutableListOf(
            Scope().apply {
                name = "Data"
                variablesReference = vm.image.run {
                    variablesAllocator.add(
                        toVariable("Stack", stack.asReversed()),
                        toVariable("Ravenmind", getRavenmind()),
                    )
                }
            },
            Scope().apply {
                name = "State"
                variablesReference = vm.image.run {
                    val variables = mutableListOf(
                        toVariable("OpsConsumed", opsConsumed.toString()),
                        toVariable("EscapeNext", escapeNext.toString()),
                        toVariable("ParenCount", parenCount.toString()),
                    )
                    if (parenCount > 0) {
                        variables += toVariable("Parenthesized", parenthesized.map { it.iota })
                    }
                    variablesAllocator.add(variables)
                }
            },
        )

        val frame = getContinuationFrame(frameId)
        getFrameVariables(frame)?.also {
            scopes += Scope().apply {
                name = "Frame"
                variablesReference = variablesAllocator.add(it)
            }
        }

        return scopes
    }

    fun getVariables(reference: Int) = variablesAllocator.getOrEmpty(reference)

    private fun getContinuationVariable(
        continuation: SpellContinuation,
        variableName: String = "",
    ): Variable = Variable().apply {
        name = variableName
        when (continuation) {
            is Done -> value = "Done"
            is NotDone -> {
                value = "NotDone"
                variablesReference = variablesAllocator.add(
                    Variable().apply {
                        name = "Frame"
                        value = continuation.frame.name
                        getFrameVariables(continuation.frame)?.let {
                            variablesReference = variablesAllocator.add(it)
                        }
                    },
                    getContinuationVariable(continuation.next, "Next"),
                )
            }
        }
    }

    private fun getFrameVariables(frame: ContinuationFrame): Sequence<Variable>? {
        val sourceLine = getFirstIotaMetadata(frame)?.toString() ?: ""
        return when (frame) {
            is FrameEvaluate -> sequenceOf(
                toVariable("Code", frame.list, sourceLine),
                toVariable("IsMetacasting", frame.isMetacasting.toString()),
            )

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

    private fun toVariable(variableName: String, iota: Iota): Variable = Variable().apply {
        name = variableName
        type = iota::class.simpleName
        when (iota) {
            is ListIota -> {
                value = "(${iota.list.count()}) ${iotaToString(iota)}"
                variablesReference = allocateVariables(iota.list)
                indexedVariables = iota.list.size()
            }

            is ContinuationIota -> getContinuationVariable(iota.continuation).also {
                value = "${iotaToString(iota)} -> ${it.value}"
                variablesReference = it.variablesReference
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

    fun getSourceContents(reference: Int) = getSourceContents(sourceAllocator[reference].second)

    private fun getSourceContents(iotas: Iterable<Iota>): String {
        return iotas.joinToString("\n") {
            val indent = iotaMetadata[it]?.indent(launchArgs.indentWidth) ?: ""
            indent + iotaToString(it, true)
        }
    }

    private fun getContinuationFrame(frameId: Int) = callStack.elementAt(frameId - 1).frame

    // TODO: gross.
    // TODO: there's probably a bug here somewhere - shouldn't we be using the metadata?
    fun setBreakpoints(sourceReference: Int, sourceBreakpoints: Array<SourceBreakpoint>): List<Breakpoint> {
        val (source, iotas) = sourceAllocator[sourceReference]
        val breakpointLines = breakpoints.getOrPut(sourceReference, ::mutableSetOf).apply { clear() }
        return sourceBreakpoints.map {
            Breakpoint().apply {
                if (it.line <= indexToLineNumber(iotas.lastIndex)) {
                    breakpointLines.add(it.line)
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

    private fun indexToLineNumber(index: Int) = if (initArgs.linesStartAt1) {
        index + 1
    } else {
        index
    }

    private fun isAtBreakpoint(): Boolean {
        val nextIota = when (val frame = nextFrame) {
            is FrameBreakpoint -> return true
            is FrameEvaluate -> getIotas(frame)?.car
            else -> null
        } ?: return false

        if (
            nextIota is PatternIota
            && nextIota.pattern == HexDebugActions.BREAKPOINT_BEFORE.get().prototype
            && vm.image.parenCount == 0
            && !vm.image.escapeNext
        ) return true

        return iotaMetadata[nextIota]
            ?.let { breakpoints[it.source.sourceReference]?.contains(it.line) }
            ?: false
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

            if (hitBreakpoint && !isSkippedByLaunchArgs(nextContinuation)) {
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

            shouldStop = shouldStop || when (stepType) {
                RequestStepType.OVER -> if (isEscaping) {
                    result.type != DebugStepType.ESCAPE
                } else {
                    stepDepth <= 0
                }

                RequestStepType.OUT -> {
                    stepDepth < 0
                }
            }

            if (shouldStop && !isSkippedByLaunchArgs(nextContinuation)) {
                return result
            }
        }
    }

    // Copy of CastingVM.queueExecuteAndWrapIotas to allow stepping by one pattern at a time.
    fun executeOnce(exactlyOnce: Boolean = false): DebugStepResult? {
        var continuation = nextContinuation // bind locally so we can do smart casting
        if (continuation !is NotDone) return null

        var stepResult = DebugStepResult("step")

        variablesAllocator.clear()

        // Begin aggregating info
        val info = CastingVM.TempControllerInfo(earlyExit = false)
        while (continuation is NotDone && !info.earlyExit) {
            // Take the top of the continuation stack...
            val frame = continuation.frame

            // ...and execute it.
            val castResult = frame.evaluate(continuation.next, world, vm)
            val newImage = castResult.newData
            val newContinuation = castResult.continuation

            // Then write all pertinent data back to the harness for the next iteration.
            if (newImage != null) {
                stepResult = handleIndent(castResult, vm.image, newImage, stepResult)
                vm.image = newImage
            }
            vm.env.postExecution(castResult)

            if (castResult.resolutionType == ResolvedPatternType.EVALUATED) {
                onExecute?.invoke(castResult.cast)
            }

            val stepType = getStepType(castResult, continuation, newContinuation)
            if (newContinuation is NotDone) {
                setIotaOverrides(castResult, frame, newContinuation, stepType)

                // ensure all of the iotas to be evaluated in the next frame are mapped to a source
                registerNewSource(newContinuation.frame)?.also {
                    stepResult = stepResult.withLoadedSource(it, LoadedSourceReason.NEW)
                }
            }
            if (stepType != null) {
                stepResult = stepResult.copy(type = stepType)
            }

            continuation = newContinuation

            try {
                vm.performSideEffects(info, castResult.sideEffects)
            } catch (e: Exception) {
                e.printStackTrace()
                vm.performSideEffects(
                    info,
                    listOf(OperatorSideEffect.DoMishap(MishapInternalException(e), Mishap.Context(null, null)))
                )
            }
            info.earlyExit = info.earlyExit || !castResult.resolutionType.success

            if (exactlyOnce || !isSkippedByLaunchArgs(continuation)) {
                break
            }
        }

        nextContinuation = continuation

        if (continuation is Done) return null

        return stepResult
    }

    private fun isSkippedByLaunchArgs(continuation: SpellContinuation): Boolean {
        if (continuation !is NotDone) return false
        return launchArgs.skipNonEvalFrames && continuation.frame !is FrameEvaluate
    }

    private fun handleIndent(
        castResult: CastResult,
        oldImage: CastingImage,
        newImage: CastingImage,
        stepResult: DebugStepResult,
    ) = when (castResult.resolutionType) {
        ResolvedPatternType.ESCAPED -> {
            // if the paren count changed, it was either an introspection or a retrospection
            // in both cases, the pattern that changed the indent level should be at the lower indent level
            val parenCount = min(oldImage.parenCount, newImage.parenCount)
            iotaMetadata[castResult.cast]?.trySetParenCount(parenCount)
            stepResult
        }

        ResolvedPatternType.EVALUATED -> if (
            newImage.parenCount == 0
            && newImage.parenthesized.isEmpty()
            && oldImage.parenthesized.isNotEmpty()
        ) {
            // closed list
            val sources = oldImage.parenthesized.asSequence()
                .mapNotNull { iotaMetadata[it.iota] }
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

        if (castResult.cast is ContinuationIota) {
            return DebugStepType.JUMP
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

        if (
            continuation.next !== newContinuation.next
            || continuation.frame.type != newContinuation.frame.type
        ) {
            // don't emit IN when starting a Thoth inner loop
            if (continuation.frame is FrameForEach) {
                return null
            }
            return DebugStepType.IN
        }

        @Suppress("KotlinConstantConditions")
        val newImage = castResult.newData as IMixinCastingImage
        return newImage.`hexDebug$getDebugStepType`()
    }

    private fun setIotaOverrides(
        castResult: CastResult,
        frame: ContinuationFrame,
        newContinuation: NotDone,
        stepType: DebugStepType?,
    ) {
        val newFrame = newContinuation.frame
        val newNextFrame = newContinuation.next.frame
        if (stepType == DebugStepType.IN) {
            if (newFrame !is FrameEvaluate) {
                frameIotaOverrides[newFrame] = castResult.cast
            } else if (newNextFrame != null && newNextFrame !is FrameEvaluate) {
                frameIotaOverrides[newNextFrame] = castResult.cast
            }
        } else if (frame is FrameForEach && newNextFrame != null && newNextFrame is FrameForEach) {
            frameIotaOverrides[newNextFrame] = frameIotaOverrides[frame]
        }
    }

    private fun iotaToString(iota: Iota, isSource: Boolean = false): String = when (iota) {
        // i feel like hex should have a thing for this...
        is PatternIota -> HexAPI.instance().run {
            when (val lookup = PatternRegistryManifest.matchPattern(iota.pattern, vm.env, false)) {
                is PatternShapeMatch.Normal -> getActionI18n(lookup.key, false)
                is PatternShapeMatch.PerWorld -> getActionI18n(lookup.key, true)
                is PatternShapeMatch.Special -> lookup.handler.name
                is PatternShapeMatch.Nothing -> when (iota.pattern) {
                    SpecialPatterns.INTROSPECTION -> if (isSource) {
                        Component.literal("{")
                    } else {
                        getRawHookI18n(HexAPI.modLoc("open_paren"))
                    }

                    SpecialPatterns.RETROSPECTION -> if (isSource) {
                        Component.literal("}")
                    } else {
                        getRawHookI18n(HexAPI.modLoc("close_paren"))
                    }

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

val SpellContinuation.frame get() = (this as? NotDone)?.frame

val SpellContinuation.next get() = (this as? NotDone)?.next

val ContinuationFrame.name get() = this::class.simpleName ?: "Unknown"

enum class RequestStepType {
    OVER,
    OUT,
}
