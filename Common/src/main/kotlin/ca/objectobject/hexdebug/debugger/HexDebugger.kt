package ca.objectobject.hexdebug.debugger

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
import ca.objectobject.hexdebug.HexDebug
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
    private val breakpoints = mutableMapOf<Int, MutableSet<Int>>() // source id -> line number

    private val nextFrame get() = (nextContinuation as? NotDone)?.frame

    init {
        registerNewSource(iotas)
    }

    private fun registerNewSource(frame: ContinuationFrame): Iota? {
        val iotas = getIotas(frame) ?: return null
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

    private fun getIotas(frame: ContinuationFrame) = when (frame) {
        is FrameEvaluate -> frame.list
        is FrameForEach -> frame.code
        else -> null
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
    fun setBreakpoints(sourceReference: Int, sourceBreakpoints: Array<SourceBreakpoint>): List<Breakpoint> {
        val (source, iotas) = sourceAllocator[sourceReference]
        val breakpointLines = breakpoints.getOrPut(sourceReference, ::mutableSetOf).apply { clear() }
        return sourceBreakpoints.map {
            Breakpoint().apply {
                if (toServerLineNumber(it.line) <= iotas.lastIndex) {
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

    val isAtBreakpoint get(): Boolean = nextFrame
        ?.let(::getIotas)
        ?.car
        ?.let(iotaMetadata::get)
        ?.let { breakpoints[it.source.sourceReference]?.contains(it.line) }
        ?: false

    fun executeUntilStopped(stepType: RequestStepType? = null): DebugStepResult? {
        var stepDepth = 0
        var lastResult: DebugStepResult? = null

        while (true) {
            var result = executeOnce() ?: return null
            if (lastResult != null) result += lastResult
            lastResult = result

            if (isAtBreakpoint) return result.copy(reason = "breakpoint")

            stepDepth = when (result.type) {
                DebugStepType.IN -> stepDepth + 1
                DebugStepType.OUT -> stepDepth - 1
                DebugStepType.JUMP -> -1
                else -> stepDepth
            }

            val shouldStop = when (stepType) {
                RequestStepType.OVER -> stepDepth <= 0 && result.type != DebugStepType.ESCAPE
                RequestStepType.OUT -> stepDepth < 0
                else -> false
            }
            if (shouldStop) return result
        }
    }

    // Copy of CastingVM.queueExecuteAndWrapIotas to allow stepping by one pattern at a time.
    fun executeOnce(): DebugStepResult? {
        var continuation = nextContinuation // bind locally so we can do smart casting
        if (continuation !is NotDone) return null

        var stepResult = DebugStepResult("step")

        variablesAllocator.clear()

        // Begin aggregating info
        val info = CastingVM.TempControllerInfo(earlyExit = false)
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
            // Then write all pertinent data back to the harness for the next iteration.
            if (newImage != null) {
                handleIndent(castResult, vm.image, newImage)?.also {
                    stepResult = stepResult.copy(type = it)
                }
                vm.image = newImage
            }
            vm.env.postExecution(castResult)

            val stepType = if (castResult.resolutionType == ResolvedPatternType.EVALUATED) {
                onExecute?.invoke(castResult.cast)

                // TODO: aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
//                if (castResult.cast is ContinuationIota) {
//                    DebugStepType.JUMP
//                } else if (continuation.next === castResult.continuation) {
//                    DebugStepType.OUT
//                } else if (castResult.continuation.frame is FrameFinishEval) {
//                    DebugStepType.OUT
//                } else if (continuation.next !== castResult.continuation.next) {
//                    DebugStepType.IN
//                } else null
            } else null
//            if (stepType != null) stepResult = stepResult.copy(type = stepType)

            HexDebug.LOGGER.info("{} {}", iotaToString(castResult.cast, false), stepType)

            continuation = castResult.continuation
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

        if (continuation is Done) return null

        return stepResult
    }

    private fun handleIndent(
        castResult: CastResult,
        oldImage: CastingImage,
        newImage: CastingImage,
    ) = when (castResult.resolutionType) {
        ResolvedPatternType.ESCAPED -> {
            // if the paren count changed, it was either an introspection or a retrospection
            // in both cases, the pattern that changed the indent level should be at the lower indent level
            val parenCount = min(oldImage.parenCount, newImage.parenCount)
            iotaMetadata[castResult.cast]?.trySetParenCount(parenCount)
            DebugStepType.ESCAPE
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
                .map { it.source }
                .toSet()

            for (source in sources) {
                // this feels scuffed...
                // reassign the source a new id so the client thinks it has to reload it
                // but the filename stays the same, so the user shouldn't really notice
                sourceAllocator.reallocate(source.sourceReference)
            }

            null
        } else if (newImage.parenCount > 0) {
            // opened list
            DebugStepType.ESCAPE
        } else null

        else -> null
    }

    private fun iotaToString(iota: Iota, isSource: Boolean): String = when (iota) {
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
