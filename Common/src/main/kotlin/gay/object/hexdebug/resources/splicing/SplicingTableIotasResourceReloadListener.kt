package gay.`object`.hexdebug.resources.splicing

import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRenderers
import gay.`object`.hexdebug.utils.contains
import gay.`object`.hexdebug.utils.getAsResourceLocation
import gay.`object`.hexdebug.utils.getOrNull
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRendererParser as RendererParser
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRendererProvider as RendererProvider

private typealias AnyRendererParser = RendererParser<RendererProvider>

private val GSON = GsonBuilder()
    .registerTypeAdapter(ResourceLocation::class.java, ResourceLocation.Serializer())
    .create()

// this is effectively a topological sorting algorithm using depth-first search
// https://en.wikipedia.org/wiki/Topological_sorting#Depth-first_search

object SplicingTableIotasResourceReloadListener :
    SimpleJsonResourceReloadListener(GSON, "hexdebug_splicing_iotas")
{
    var fallback: RendererProvider? = null
        private set

    private val providersByType = mutableMapOf<IotaType<*>, RendererProvider>()

    private val objects = mutableMapOf<ResourceLocation, JsonObject>()
    private val parsersByProviderId = mutableMapOf<ResourceLocation, AnyRendererParser>()
    private val providersById = mutableMapOf<ResourceLocation, RendererProvider>()
    private val visitingProviders = linkedSetOf<ResourceLocation>() // use a linked set so the error message is ordered
    private val failedProviders = mutableSetOf<ResourceLocation>()

    fun getProvider(iotaType: IotaType<*>): RendererProvider? = providersByType[iotaType]

    @JvmStatic
    fun loadProvider(providerId: ResourceLocation): RendererProvider {
        check(visitingProviders.isNotEmpty()) {
            "Tried to call loadProvider outside of SplicingTableIotaRendererParser#parse"
        }
        val jsonObject = objects[providerId]
            ?: throw IllegalArgumentException("Provider $providerId not found")
        return visit(providerId, jsonObject)
    }

    @JvmStatic
    fun parseProvider(jsonObject: JsonObject): RendererProvider {
        check(visitingProviders.isNotEmpty()) {
            "Tried to call parseProvider outside of SplicingTableIotaRendererParser#parse"
        }
        return visit(jsonObject).second
    }

    override fun apply(
        map: MutableMap<ResourceLocation, JsonElement>,
        manager: ResourceManager,
        profiler: ProfilerFiller
    ) {
        HexDebug.LOGGER.info("Loading splicing table iota renderers...")

        fallback = null
        providersByType.clear()

        objects.clear()
        parsersByProviderId.clear()
        providersById.clear()
        visitingProviders.clear()
        failedProviders.clear()

        // filter out any weird non-object files
        for ((id, jsonElement) in map) {
            if (!jsonElement.isJsonObject) continue
            objects[id] = jsonElement.asJsonObject
        }

        // visit all providers
        for ((id, jsonObject) in objects) {
            if (id in failedProviders) continue
            try {
                visit(id, jsonObject)
            } catch (e: Exception) {
                HexDebug.LOGGER.error("Caught exception while loading renderer for $id, skipping", e)
                failedProviders.add(id)
                failedProviders.addAll(visitingProviders)
                visitingProviders.clear()
            }
        }

        HexDebug.LOGGER.info("Loaded ${providersById.size} splicing table iota renderers for ${providersByType.size} iota types")
    }

    private fun visit(providerId: ResourceLocation, jsonObject: JsonObject): RendererProvider {
        providersById[providerId]?.let { return it }

        if (!visitingProviders.add(providerId)) {
            throw IllegalStateException("Cycle detected: ${visitingProviders.joinToString()}, $providerId")
        }

        val (parser, provider) = try {
            visit(jsonObject)
        } catch (e: ProviderVisitException) {
            throw e // don't make a huge unnecessary stack trace
        } catch (e: Exception) {
            throw ProviderVisitException("Failed to parse provider $providerId", e)
        }

        visitingProviders.remove(providerId)

        parsersByProviderId[providerId] = parser
        providersById[providerId] = provider
        HexIotaTypes.REGISTRY.getOrNull(providerId)?.let {
            providersByType[it] = provider
        }
        if (providerId == HexDebug.id("builtin/generic")) {
            fallback = provider
        }

        return provider
    }

    private fun visit(jsonObject: JsonObject): Pair<AnyRendererParser, RendererProvider> {
        val (parser, parent) = when {
            "parent" in jsonObject -> {
                val parentId = jsonObject.getAsResourceLocation("parent")
                val parentObject = objects[parentId]
                    ?: throw JsonParseException("Parent $parentId not found")

                // DFS recursion - ensure the parent has been loaded first
                val provider = visit(parentId, parentObject)

                parsersByProviderId[parentId]!! to provider
            }

            "type" in jsonObject -> {
                val typeId = jsonObject.getAsResourceLocation("type")
                val parser = SplicingTableIotaRenderers.getParser(typeId)
                    ?: throw JsonParseException("Parser type $typeId not found")
                @Suppress("UNCHECKED_CAST") // shhhhhhh
                parser as AnyRendererParser to null
            }

            else -> throw JsonParseException("Expected parent or type field but got neither")
        }

        // there may be more recursion here if the parser calls loadProvider or parseProvider
        return parser to parser.parse(GSON, jsonObject, parent)
    }
}

class ProviderVisitException(message: String? = null, cause: Throwable? = null) : IllegalStateException(message, cause)
