package gay.`object`.hexdebug.utils

import com.mojang.blaze3d.vertex.PoseStack

fun <R> PoseStack.letPushPose(f: (PoseStack) -> R): R {
    pushPose()
    val result = f(this)
    popPose()
    return result
}

fun <R> PoseStack.pushPose(f: () -> R): R =
    letPushPose { f() }

fun PoseStack.scale(scale: Float) {
    scale(scale, scale, scale) // scale
}
