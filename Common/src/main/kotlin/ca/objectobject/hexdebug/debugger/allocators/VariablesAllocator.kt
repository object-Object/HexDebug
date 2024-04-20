package ca.objectobject.hexdebug.debugger.allocators

import org.eclipse.lsp4j.debug.Variable

class VariablesAllocator : Allocator<Sequence<Variable>>() {
    fun getOrEmpty(reference: Int) = values.getOrElse(toIndex(reference)) { sequenceOf() }

    fun add(variables: Iterable<Variable>) = add(variables.asSequence())

    fun add(vararg variables: Variable) = add(sequenceOf(*variables))
}
