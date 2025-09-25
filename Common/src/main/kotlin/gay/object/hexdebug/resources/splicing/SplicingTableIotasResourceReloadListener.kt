package gay.`object`.hexdebug.resources.splicing

import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRendererParser
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRendererProvider
import gay.`object`.hexdebug.api.client.splicing.SplicingTableIotaRenderers
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.util.profiling.ProfilerFiller

private val GSON = GsonBuilder()
    .registerTypeAdapter(ResourceLocation::class.java, ResourceLocation.Serializer())
    .create()

object SplicingTableIotasResourceReloadListener :
    SimpleJsonResourceReloadListener(GSON, "hexdebug_splicing_iotas")
{
    val PROVIDERS = mutableMapOf<IotaType<*>, SplicingTableIotaRendererProvider>()
    var FALLBACK: SplicingTableIotaRendererProvider? = null

    override fun apply(
        map: MutableMap<ResourceLocation, JsonElement>,
        manager: ResourceManager,
        profiler: ProfilerFiller
    ) {
        HexDebug.LOGGER.info("Loading splicing table iota renderers...")
        PROVIDERS.clear()
        FALLBACK = null

        // first, filter out any weird non-object files
        val objects = mutableMapOf<ResourceLocation, JsonObject>()
        for ((id, jsonElement) in map) {
            if (!jsonElement.isJsonObject) continue
            objects[id] = jsonElement.asJsonObject
        }

        // next, find a topological order to load the providers
        val stack = mutableListOf<ResourceLocation>()
        val parsers = mutableMapOf<ResourceLocation, SplicingTableIotaRendererParser<SplicingTableIotaRendererProvider>>()
        for (id in objects.keys) {
            findParents(objects, id)?.let { (toAdd, parser) ->
                for (it in toAdd) {
                    stack.add(it)
                    @Suppress("UNCHECKED_CAST")
                    parsers[it] = parser as SplicingTableIotaRendererParser<SplicingTableIotaRendererProvider>
                }
            }
        }

        // finally, actually load the providers
        val providers = mutableMapOf<ResourceLocation, SplicingTableIotaRendererProvider>()
        val failed = mutableSetOf<ResourceLocation>()
        for (id in stack.asReversed()) {
            if (id in providers || id in failed) continue

            val provider = try {
                val parser = parsers[id]!!
                val jsonObject = objects[id]!!
                val parent = if (jsonObject.has("parent")) {
                    val parentId = ResourceLocation(jsonObject.getAsJsonPrimitive("parent").asString)
                    // we're loading parents first, so this should always be present unless the parent failed
                    providers[parentId] ?: continue
                } else null
                parser.parse(GSON, jsonObject, parent)
            } catch (e: Exception) {
                HexDebug.LOGGER.error("Caught exception while parsing $id, ignoring", e)
                failed.add(id)
                continue
            }

            providers[id] = provider
            if (HexIotaTypes.REGISTRY.containsKey(id)) {
                PROVIDERS[HexIotaTypes.REGISTRY.get(id)!!] = provider
            }
            if (id == HexDebug.id("builtin/generic")) {
                FALLBACK = provider
            }
        }

        HexDebug.LOGGER.info("Loaded ${providers.size} splicing table iota renderers for ${PROVIDERS.size} iota types")
    }

    private fun findParents(
        objects: Map<ResourceLocation, JsonObject>,
        id: ResourceLocation,
    ): Pair<List<ResourceLocation>, SplicingTableIotaRendererParser<*>>? {
        var jsonObject = objects[id]!!
        val knownIds = mutableSetOf(id)
        val stack = mutableListOf(id)

        while (jsonObject.has("parent")) {
            val parentId = try {
                ResourceLocation(jsonObject.getAsJsonPrimitive("parent").asString)
            } catch (e: Exception) {
                HexDebug.LOGGER.error("Failed to parse parent field of ${stack.last()} while loading $id, ignoring", e)
                return null
            }

            if (!knownIds.add(parentId)) {
                HexDebug.LOGGER.error("Loop found in parents of $id ($parentId appears twice), ignoring")
                return null
            }

            if (parentId !in objects) {
                HexDebug.LOGGER.error("Parent id $parentId not found while loading $id, ignoring")
                return null
            }

            stack.add(parentId)
            jsonObject = objects[parentId]!!
        }

        val typeId = try {
            ResourceLocation(jsonObject.getAsJsonPrimitive("type").asString)
        } catch (e: Exception) {
            HexDebug.LOGGER.error("Failed to parse type field of ${stack.last()} while loading $id, ignoring", e)
            return null
        }

        val parser = SplicingTableIotaRenderers.getParser(typeId)
        if (parser == null) {
            HexDebug.LOGGER.error("Unrecognized type $typeId for ${stack.last()} while loading $id, ignoring")
            return null
        }

        return stack to parser
    }
}
