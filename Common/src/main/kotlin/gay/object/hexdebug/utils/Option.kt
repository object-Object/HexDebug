package gay.`object`.hexdebug.utils

/** Similar to [java.util.Optional], but works with nullable values too. */
sealed class Option<out T> {
    abstract fun getOrNull(): T?
    abstract fun getOrElse(other: @UnsafeVariance T): T
    abstract fun ifPresent(fn: (T) -> Unit)
    abstract fun <U> map(fn: (T) -> U): Option<U>

    data class Some<T>(val value: T) : Option<T>() {
        override fun getOrNull() = value
        override fun getOrElse(other: T) = value
        override fun ifPresent(fn: (T) -> Unit) = fn(value)
        override fun <U> map(fn: (T) -> U) = Some(fn(value))
    }

    class None<T> : Option<T>() {
        override fun getOrNull() = null
        override fun getOrElse(other: T) = other
        override fun ifPresent(fn: (T) -> Unit) {}
        override fun <U> map(fn: (T) -> U) = None<U>()
    }
}
