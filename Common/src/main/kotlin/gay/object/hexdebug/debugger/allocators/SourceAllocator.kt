package gay.`object`.hexdebug.debugger.allocators

import at.petrak.hexcasting.api.casting.iota.Iota
import org.eclipse.lsp4j.debug.Source

class SourceAllocator : Allocator<Pair<Source, List<Iota>>>() {
    private val knownSources = mutableMapOf<Int, Int>()

    fun add(threadId: Int, iotas: List<Iota>) = Source().apply {
        // hash collision? what's that
        sourceReference = knownSources.getOrPut(iotas.hashCode()) { add(this to iotas) }
        name = "source$sourceReference.hexpattern"
        path = name
        adapterData = threadId
    }

    override fun clear() {
        super.clear()
        knownSources.clear()
    }
}
