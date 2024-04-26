package gay.`object`.hexdebug.registry

import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import gay.`object`.hexdebug.HexDebug
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import java.util.function.Supplier

@Suppress("SameParameterValue")
abstract class HexDebugRegistry<T>(registryKey: ResourceKey<Registry<T>>) {
    private val register: DeferredRegister<T> = DeferredRegister.create(HexDebug.MODID, registryKey)

    protected open fun register(id: String, supplier: Supplier<T>): RegistrySupplier<T> {
        return register.register(id, supplier)
    }

    open fun init() {
        register.register()
    }
}
