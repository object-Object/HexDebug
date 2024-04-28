package gay.`object`.hexdebug.registry

import com.google.common.base.Suppliers
import dev.architectury.registry.registries.Registrar
import dev.architectury.registry.registries.RegistrarManager
import dev.architectury.registry.registries.RegistrySupplier
import gay.`object`.hexdebug.HexDebug
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import java.util.function.Supplier

abstract class HexDebugRegistry<T>(private val getRegistrar: (RegistrarManager) -> Registrar<T>) {
    constructor(key: ResourceKey<Registry<T>>) : this({ it.get(key) })

    @Suppress("DEPRECATION")
    constructor(registry: Registry<T>) : this({ it.get(registry) })

    private val lazyValues = mutableListOf<Lazy<RegistrySupplier<T>>>()

    private lateinit var registrar: Registrar<T>

    protected open fun register(id: String, supplier: Supplier<T>): Lazy<RegistrySupplier<T>> {
        val lazyValue = lazy {
            registrar.register(HexDebug.id(id), supplier)
        }
        lazyValues.add(lazyValue)
        return lazyValue
    }

    open fun init() {
        registrar = getRegistrar(MANAGER.get())
        for (lazyValue in lazyValues) {
            lazyValue.value
        }
    }

    companion object {
        val MANAGER = Suppliers.memoize { RegistrarManager.get(HexDebug.MODID) }
    }
}
