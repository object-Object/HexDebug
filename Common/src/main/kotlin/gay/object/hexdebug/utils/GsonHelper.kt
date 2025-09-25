@file:JvmName("GsonHelper")

package gay.`object`.hexdebug.utils

import com.google.gson.JsonObject
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.GsonHelper

fun JsonObject.getAsResourceLocation(memberName: String, fallback: ResourceLocation?): ResourceLocation {
    return try {
        ResourceLocation(GsonHelper.getAsString(this, memberName))
    } catch (e: Exception) {
        fallback ?: throw e
    }
}
