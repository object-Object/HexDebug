package gay.`object`.hexdebug.registry

import gay.`object`.hexdebug.HexDebug
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation

// scuffed.

// use Any upper bound to forbid nullable types, because Registry.register needs that???
abstract class HexDebugRegistrar<T : Any>(
    val registryKey: ResourceKey<Registry<T>>,
    getRegistry: () -> Registry<T>,
) {
    /** Do not access until the mod has been initialized! */
    val registry by lazy(getRegistry)

    private var isInitialized = false

    private val mutableEntries = mutableMapOf<ResourceLocation, Lazy<T>>()
    val entries: Map<ResourceLocation, Lazy<T>> = mutableEntries

    open fun init(registerer: (ResourceLocation, T) -> Unit) {
        if (isInitialized) throw IllegalStateException("$this has already been initialized!")
        isInitialized = true
        for ((id, lazyValue) in entries) {
            registerer(id, lazyValue.value)
        }
    }

    fun <U : T> register(name: String, getValue: () -> U): Entry<U> = register(HexDebug.id(name), getValue)

    fun <U : T> register(id: ResourceLocation, getValue: () -> U): Entry<U> = register(id, lazy {
        if (!isInitialized) throw IllegalStateException("$this has not been initialized!")
        getValue()
    })

    fun <U : T> register(id: ResourceLocation, lazyValue: Lazy<U>): Entry<U> = Entry(id, lazyValue).also {
        if (mutableEntries.putIfAbsent(id, lazyValue) != null) {
            throw IllegalArgumentException("Duplicate id: $id")
        }
    }

    data class Entry<T>(val id: ResourceLocation, private val lazyValue: Lazy<T>) {
        /** Do not access until the mod has been initialized! */
        val value by lazyValue
    }
}
