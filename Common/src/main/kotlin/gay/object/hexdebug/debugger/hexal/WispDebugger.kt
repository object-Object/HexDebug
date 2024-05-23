package gay.`object`.hexdebug.debugger.hexal

import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.NullIota
import gay.`object`.hexdebug.adapter.LaunchArgs
import gay.`object`.hexdebug.debugger.CastArgs
import gay.`object`.hexdebug.debugger.HexDebugger
import org.eclipse.lsp4j.debug.InitializeRequestArguments
import org.eclipse.lsp4j.debug.Scope
import ram.talia.hexal.api.casting.wisp.WispCastingManager.WispCast
import java.util.*

// TODO: handling Done needs to be different
class WispDebugger(
    initArgs: InitializeRequestArguments,
    launchArgs: LaunchArgs,
    castArgs: CastArgs,
    private val wispUUID: UUID,
) : HexDebugger(initArgs, launchArgs, castArgs) {
    override var vm: CastingVM = super.vm

    private var wispCast: WispCast? = null

    fun updateWispCast(newCast: WispCast, newVM: CastingVM): Boolean {
        if (newCast.wispUUID != wispUUID) return false

        wispCast = newCast
        vm = newVM
        (vm.env as IMixinWispCastEnv).isDebugging = true
        return true
    }

    private fun getInitialRavenmind(wispCast: WispCast) =
        wispCast.initialRavenmind?.let { IotaType.deserialize(it, vm.env.world) } ?: NullIota()

    override fun getScopes(frameId: Int): List<Scope> {
        val scopes = super.getScopes(frameId)
        val wispCast = wispCast ?: return scopes
        return scopes + listOfNotNull(
            Scope().apply {
                name = "WispCast"
                variablesReference = variablesAllocator.add(
                    toVariable("Hex", wispCast.hex.getIotas(vm.env.world)),
                    toVariable("InitialStack", wispCast.initialStack.getIotas(vm.env.world)),
                    toVariable("InitialRavenmind", getInitialRavenmind(wispCast)),
                    toVariable("TimeAdded", wispCast.timeAdded),
                )
            },
            wispCast.wisp?.let { wisp ->
                Scope().apply {
                    name = "Wisp"
                    variablesReference = variablesAllocator.add(
                        toVariable("Type", wisp::class.simpleName ?: "Unknown"),
                        toVariable("Caster", wisp.caster?.name?.string ?: "None"),
                        toVariable("IsSeon", wisp.seon),
                        toVariable("HasActiveTrigger", !wisp.canScheduleCast()),
                        toVariable("SummonedChildThisCast", wisp.summonedChildThisCast),
                    )
                }
            },
        )
    }
}
