package gay.`object`.hexdebug.debugger.allocators

import at.petrak.hexcasting.api.casting.iota.Iota
import org.eclipse.lsp4j.debug.Source

class SourceAllocator : Allocator<Pair<Source, List<Iota>>>() {
    fun add(iotas: List<Iota>) = Source().apply {
        sourceReference = add(this to iotas)
        name = filename(sourceReference)
        path = name
    }

    private fun filename(suffix: Any) = "source$suffix.hexpattern"
}
