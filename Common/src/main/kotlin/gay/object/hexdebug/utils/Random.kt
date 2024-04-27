package gay.`object`.hexdebug.utils

import kotlin.math.pow
import kotlin.random.Random

fun Random.nextHexString(length: UShort): String {
    val prefixInt = nextInt(0, 16f.pow(length.toInt()).toInt())
    return "%0${length}x".format(prefixInt)
}
