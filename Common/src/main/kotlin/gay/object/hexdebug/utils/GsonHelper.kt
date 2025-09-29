@file:JvmName("GsonHelper")

package gay.`object`.hexdebug.utils

import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import net.minecraft.commands.arguments.NbtPathArgument
import net.minecraft.commands.arguments.NbtPathArgument.NbtPath
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.GsonHelper

operator fun JsonObject.contains(memberName: String) = has(memberName)

fun JsonObject.getAsResourceLocation(memberName: String, fallback: ResourceLocation? = null): ResourceLocation {
    return try {
        ResourceLocation(GsonHelper.getAsString(this, memberName))
    } catch (e: Exception) {
        fallback ?: throw e
    }
}

fun JsonObject.getAsNbtPathOrNull(memberName: String, fallback: NbtPath? = null): NbtPath? {
    if (memberName in this) {
        return getAsNbtPath(memberName)
    }
    return fallback
}

fun JsonObject.getAsNbtPath(memberName: String, fallback: NbtPath? = null): NbtPath {
    val rawPath = GsonHelper.getAsString(this, memberName)
    return try {
        NbtPathArgument().parse(StringReader(rawPath))
    } catch (e: CommandSyntaxException) {
        fallback ?: throw JsonSyntaxException("Invalid $memberName, expected a valid NBT path", e)
    }
}
