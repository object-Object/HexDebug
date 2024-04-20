package ca.objectobject.hexdebug.debugger.allocators

import at.petrak.hexcasting.api.casting.iota.Iota
import org.eclipse.lsp4j.debug.Source

class SourceAllocator : Allocator<Pair<Source, List<Iota>>>() {
    fun add(iotas: List<Iota>) = Source().apply {
        sourceReference = add(Pair(this, iotas))
        name = "source$sourceReference.hexpattern"
    }
}
