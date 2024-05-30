package gay.`object`.hexdebug.registry

import dev.architectury.platform.Platform
import gay.`object`.hexdebug.HexDebug
import net.fabricmc.api.EnvType
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
        if (Platform.getEnv() == EnvType.CLIENT) {
            initClient()
        }
    }

    open fun initClient() {}

    fun <V : T> register(name: String, builder: () -> V) = register(HexDebug.id(name), builder)

    fun <V : T> register(id: ResourceLocation, builder: () -> V) = register(id, lazy {
        if (!isInitialized) throw IllegalStateException("$this has not been initialized!")
        builder()
    })

    fun <V : T> register(id: ResourceLocation, lazyValue: Lazy<V>) = Entry(id, lazyValue, registryKey).also {
        if (mutableEntries.putIfAbsent(id, lazyValue) != null) {
            throw IllegalArgumentException("Duplicate id: $id")
        }
    }

    inner class Entry<V : T>(
        val id: ResourceLocation,
        lazyValue: Lazy<V>,
        registryKey: ResourceKey<Registry<T>>,
    ) {
        val key = ResourceKey.create(registryKey, id)

        /** Do not access until the mod has been initialized! */
        val value by lazyValue
    }
}
