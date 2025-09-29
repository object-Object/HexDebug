package gay.`object`.hexdebug.utils

import com.mojang.blaze3d.vertex.PoseStack

fun PoseStack.pushPose(f: () -> Unit) {
    pushPose()
    f()
    popPose()
}

fun PoseStack.scale(scale: Float) {
    scale(scale, scale, scale) // scale
}
