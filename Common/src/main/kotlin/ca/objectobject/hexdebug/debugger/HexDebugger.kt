package ca.objectobject.hexdebug.debugger

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.casting.PatternShapeMatch
import at.petrak.hexcasting.api.casting.SpellList
import at.petrak.hexcasting.api.casting.eval.SpecialPatterns
import at.petrak.hexcasting.api.casting.eval.sideeffects.OperatorSideEffect
import at.petrak.hexcasting.api.casting.eval.vm.*
import at.petrak.hexcasting.api.casting.iota.*
import at.petrak.hexcasting.api.casting.mishaps.Mishap
import at.petrak.hexcasting.api.casting.mishaps.MishapInternalException
import at.petrak.hexcasting.common.casting.PatternRegistryManifest
import ca.objectobject.hexdebug.server.LaunchArgs
import org.eclipse.lsp4j.debug.*

class HexDebugger(
    private val initArgs: InitializeRequestArguments,
    private val launchArgs: LaunchArgs,
    cast: DebugCastArgs,
) {
    private val vm = cast.vm
    private val world = cast.world
    private val onExecute = cast.onExecute

    var continuation = SpellContinuation.Done.pushFrame(
        FrameEvaluate(SpellList.LList(0, cast.iotas), false)
    )

    var continuations: List<SpellContinuation.NotDone> = getContinuations(continuation)

    // current continuation is last
    private fun getContinuations(current: SpellContinuation) =
        generateSequence(current as? SpellContinuation.NotDone) {
            when (val next = it.next) {
                is SpellContinuation.Done -> null
                is SpellContinuation.NotDone -> next
            }
        }.toList().asReversed()

    var currentLineNumber = 0

    private val breakpoints = mutableMapOf<Int, Set<Int>>() // source id -> line number
    private val allocatedVariables = mutableListOf<Sequence<Variable>>()
    private var sources: List<Source>? = null

    fun getStackFrames(): Sequence<StackFrame> = continuations.mapIndexed { i, it ->
        StackFrame().apply {
            id = i + 1
            name = "Frame $id (${it.frame.name})"
            source = getSource(id, it.frame)
            if (id == continuations.count()) {
                line = serverToClientLineNumber(currentLineNumber)
            }
        }
    }.asReversed().asSequence()

    fun getScopes(frameId: Int): List<Scope> {
        val scopes = mutableListOf(
            Scope().apply {
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
                    allocateVariables(variables.asSequence())
                }
            }
        )

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
                variablesReference = allocateVariables(it)
            }
        }

        return scopes
    }

    fun getVariables(variablesReference: Int): Sequence<Variable> {
        return allocatedVariables.getOrElse(variablesReference - 1) { sequenceOf() }
    }

    private fun getRavenmind() = if (vm.image.userData.contains(HexAPI.RAVENMIND_USERDATA)) {
        IotaType.deserialize(vm.image.userData.getCompound(HexAPI.RAVENMIND_USERDATA), vm.env.world)
    } else {
        NullIota()
    }

    private fun toVariables(iotas: Iterable<Iota>) = toVariables(iotas.asSequence())

    private fun toVariables(iotas: Sequence<Iota>) = iotas.mapIndexed(::toVariable)

    private fun toVariable(index: Number, iota: Iota) = toVariable("$index", iota)

    private fun toVariable(name: String, iota: Iota): Variable = Variable().apply {
        this.name = name
        type = iota::class.simpleName
        value = when (iota) {
            is ListIota -> {
                variablesReference = allocateVariables(toVariables(iota.list))
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

    private fun allocateVariables(iotas: Iterable<Iota>) = allocateVariables(toVariables(iotas))

    private fun allocateVariables(vararg variables: Variable) = allocateVariables(sequenceOf(*variables))

    private fun allocateVariables(values: Sequence<Variable>): Int {
        allocatedVariables.add(values)
        return allocatedVariables.lastIndex + 1
    }

    fun getSources() = sources ?: continuations.mapIndexedNotNull { i, it ->
        getSource(i + 1, it.frame)
    }.also {
        this.sources = it
    }

    fun getSource(frameId: Int, frame: ContinuationFrame) = if (hasSource(frame)) {
        Source().apply {
            name = "frame${frameId}_${frame.name}.hexpattern"
            sourceReference = frameId
        }
    } else {
        null
    }

    fun hasSource(frameId: Int) = hasSource(getContinuationFrame(frameId))

    fun hasSource(frame: ContinuationFrame) = when (frame) {
        is FrameEvaluate, is FrameForEach -> true
        else -> false
    }

    fun getSourceContents(sourceReference: Int) = when (val frame = getContinuationFrame(sourceReference)) {
        is FrameEvaluate -> getSourceContents(frame.list)
        is FrameForEach -> getSourceContents(frame.code)
        is FrameFinishEval -> null
        else -> null
    }

    private fun getSourceContents(list: SpellList) = getSourceContents(list.toList())

    private fun getSourceContents(iotas: List<Iota>): String {
        return iotas.joinToString("\n") { iotaToString(it, true) }
    }

    private fun getContinuationFrame(frameId: Int) = continuations.elementAt(frameId - 1).frame

    fun setBreakpoints(sourceReference: Int?, sourceBreakpoints: Array<SourceBreakpoint>) = sourceBreakpoints.map {
        Breakpoint().apply {
            isVerified = true
            line = it.line
            column = it.column
        }
    }.also {
        breakpoints.clear()
        breakpoints[sourceReference ?: continuations.count()] = it.map {
            breakpoint -> clientToServerLineNumber(breakpoint.line)
        }.toSet()
    }

    private fun clientToServerLineNumber(line: Int) = if (initArgs.linesStartAt1) {
        line - 1
    } else {
        line
    }

    private fun serverToClientLineNumber(line: Int) = if (initArgs.linesStartAt1) {
        line + 1
    } else {
        line
    }

    fun executeUntilStopped(stopType: StopType? = null): String? {
        val callStackSize = continuations.count()
        val lineNumber = currentLineNumber
        while (executeOnce() != null) {
            if (isAtBreakpoint) return "breakpoint"
            val newCallStackSize = continuations.count()
            when (stopType) {
                StopType.STEP_OVER -> if (newCallStackSize == callStackSize) {
                    currentLineNumber = lineNumber + 1
                    return "step"
                } else if (newCallStackSize < callStackSize) return "step"
                StopType.STEP_OUT -> if (newCallStackSize < callStackSize) return "step"
                else -> {}
            }
        }
        return null
    }

    val isAtBreakpoint get() = breakpoints[continuations.count()]?.contains(currentLineNumber) == true

    // Copy of CastingVM.queueExecuteAndWrapIotas to allow stepping by one pattern at a time.
    fun executeOnce(): String? {
        allocatedVariables.clear()
        currentLineNumber += 1

        val callStackSize = continuations.count()

        val info = CastingVM.TempControllerInfo(earlyExit = false)
        var currentContinuation = continuation
        while (currentContinuation is SpellContinuation.NotDone) {
            // Take the top of the continuation stack...
            val next = currentContinuation.frame
            // ...and execute it.
            // TODO there used to be error checking code here; I'm pretty sure any and all mishaps should already
            // get caught and folded into CastResult by evaluate.
            val image2 = next.evaluate(currentContinuation.next, world, vm)
            // Then write all pertinent data back to the harness for the next iteration.
            if (image2.newData != null) {
                vm.image = image2.newData!!
            }
            vm.env.postExecution(image2)

            currentContinuation = image2.continuation
            val notDoneContinuation = currentContinuation as? SpellContinuation.NotDone

            try {
                vm.performSideEffects(info, image2.sideEffects)
            } catch (e: Exception) {
                e.printStackTrace()
                vm.performSideEffects(
                    info,
                    listOf(OperatorSideEffect.DoMishap(MishapInternalException(e), Mishap.Context(null, null)))
                )
            }

            // if we detect a nested evaluation, reset the line number and invalidate all sources
            // TODO: ask Alwinfy or someone if there's a better way to do this????
            val evaluatedFrame = next as? FrameEvaluate
            val currentFrame = notDoneContinuation?.frame as? FrameEvaluate
            if (
                evaluatedFrame != null
                && currentFrame != null
                && evaluatedFrame.list.cdr.toList() != currentFrame.list.toList()
                || currentFrame !is FrameEvaluate
            ) {
                currentLineNumber = 0
                sources = null
            }

            if (info.earlyExit) return null

            if (notDoneContinuation?.frame is FrameEvaluate) {
                onExecute?.invoke(image2.cast)
                break
            }
        }

        continuation = currentContinuation
        continuations = getContinuations(continuation)

        return if (continuation is SpellContinuation.NotDone) { "step" } else { null }
    }

    private fun iotaToString(iota: Iota, isSource: Boolean): String = when (iota) {
        // i feel like hex should have a thing for this...
        is PatternIota -> HexAPI.instance().run {
            when (val lookup = PatternRegistryManifest.matchPattern(iota.pattern, vm.env, false)) {
                is PatternShapeMatch.Normal -> getActionI18n(lookup.key, false)
                is PatternShapeMatch.PerWorld -> getActionI18n(lookup.key, true)
                is PatternShapeMatch.Special -> lookup.handler.name
                is PatternShapeMatch.Nothing -> when (iota.pattern) {
                    SpecialPatterns.INTROSPECTION -> getRawHookI18n(HexAPI.modLoc("open_paren"))
                    SpecialPatterns.RETROSPECTION -> getRawHookI18n(HexAPI.modLoc("close_paren"))
                    SpecialPatterns.INTROSPECTION -> getRawHookI18n(HexAPI.modLoc("escape"))
                    SpecialPatterns.INTROSPECTION -> getRawHookI18n(HexAPI.modLoc("undo"))
                    else -> iota.display()
                }
            }.string
        }

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
    STEP_OVER,
    STEP_OUT,
}
