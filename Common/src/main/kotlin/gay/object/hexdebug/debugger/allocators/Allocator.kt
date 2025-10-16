package gay.`object`.hexdebug.debugger.allocators

open class Allocator<T> : Iterable<T> {
    protected val values = mutableListOf<T>()

    protected fun toReference(index: Int) = index + 1
    protected fun toIndex(reference: Int) = reference - 1

    open fun add(value: T): Int {
        values.add(value)
        return toReference(values.lastIndex)
    }

    open operator fun get(reference: Int): T? {
        return values.getOrNull(toIndex(reference))
    }

    open fun clear() {
        values.clear()
    }

    override fun iterator() = values.iterator()
}
