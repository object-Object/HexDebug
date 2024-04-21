package gay.`object`.hexdebug.debugger.allocators

import at.petrak.hexcasting.api.casting.iota.Iota
import gay.`object`.hexdebug.HexDebug
import org.eclipse.lsp4j.debug.Source

class SourceAllocator : Allocator<Pair<Source, List<Iota>>>() {
    fun add(iotas: List<Iota>) = Source().apply {
        sourceReference = add(this to iotas)
        name = "source$sourceReference.hexpattern"
    }

    fun reallocate(reference: Int) {
        val (source, iotas) = get(reference)
        source.apply {
            sourceReference = add(this to iotas)
            HexDebug.LOGGER.debug("Reallocating {} from {} to {}", name, reference, sourceReference)
        }
    }
}
