package gay.`object`.hexdebug.debugger.allocators

import at.petrak.hexcasting.api.spell.iota.Iota
import gay.`object`.hexdebug.utils.nextHexString
import org.eclipse.lsp4j.debug.Source
import kotlin.random.Random

class SourceAllocator(prefixRandom: Random = Random) : Allocator<Pair<Source, List<Iota>>>() {
    constructor(prefixSeed: Int) : this(Random(prefixSeed))

    // random 8-digit hex value, to stop the client from using the wrong source for unrelated casts
    private val prefix: String = prefixRandom.nextHexString(8u)

    fun add(iotas: List<Iota>) = Source().apply {
        sourceReference = add(this to iotas)
        name = "source$sourceReference.hexpattern"
        path = "$prefix/$name"
    }
}
