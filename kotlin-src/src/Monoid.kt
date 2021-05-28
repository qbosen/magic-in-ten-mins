package monoid

import java.util.*

/**
 * @author  qiubaisen
 * @date  2021/5/28
 */

interface Monoid<A> {
    val zero: A
    operator fun A.plus(b: A): A
    fun sum(others: Iterable<A>): A {
        return others.fold(zero) { a, b -> a + b }
    }
}

// Optional 的加法 表示 获取第一个不为空的值
class OptionalM<T> : Monoid<Optional<T>> {
    override val zero: Optional<T> = Optional.empty()

    override fun Optional<T>.plus(b: Optional<T>): Optional<T> = if (this.isPresent) this else b
}

object OptionalMTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val opt = OptionalM<Int>()
        val res = opt.sum(
            listOf(
                Optional.empty(),
                Optional.ofNullable(null),
                Optional.of(1),
                Optional.of(2),
                Optional.empty()
            )
        )
        println(res)
    }
}