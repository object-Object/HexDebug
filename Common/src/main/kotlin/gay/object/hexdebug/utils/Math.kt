package gay.`object`.hexdebug.utils

import net.minecraft.world.phys.Vec2
import kotlin.math.ceil
import kotlin.math.log
import kotlin.math.pow

/** Rounds `this` up to the next power of `base`. */
fun Number.ceilToPow(base: Number): Int = toDouble().ceilToPow(base.toDouble())

// https://stackoverflow.com/q/19870067
private fun Double.ceilToPow(base: Double): Int = base.pow(ceil(log(this, base))).toInt()

fun pointInTriangle(pt: Vec2, triangle: Triple<Vec2, Vec2, Vec2>): Boolean {
    val (v1, v2, v3) = triangle
    return pointInTriangle(pt, v1, v2, v3)
}

// https://stackoverflow.com/a/2049593
fun pointInTriangle(pt: Vec2, v1: Vec2, v2: Vec2, v3: Vec2): Boolean {
    val d1 = sign(pt, v1, v2)
    val d2 = sign(pt, v2, v3)
    val d3 = sign(pt, v3, v1)

    val hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0)
    val hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0)

    return !(hasNeg && hasPos)
}

fun pointInRect(pt: Vec2, topLeft: Vec2, bottomRight: Vec2) =
    pt.x >= topLeft.x && pt.y >= topLeft.y && pt.x < bottomRight.x && pt.y < bottomRight.y

fun sign(p1: Vec2, p2: Vec2, p3: Vec2): Float {
    return (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y)
}

fun vec2(x: Number, y: Number) = Vec2(x.toFloat(), y.toFloat())
