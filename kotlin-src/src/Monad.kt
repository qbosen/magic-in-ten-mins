package monad

import hkt.HKT
import hkt.HKTList
import hkt.HKTList.K.narrow
import monad.HKTListMonad.flatMap
import monad.Maybe.K.narrow
import monad.MaybeMonad.flatMap
import kotlin.test.assertEquals

/**
 * @author qiubaisen
 * @date 2021/5/29
 */
interface Monad<F> {
    fun <A> pure(a: A): HKT<F, A>
    fun <A, B> HKT<F, A>.flatMap(f: (A) -> HKT<F, B>): HKT<F, B>
}

object HKTListMonad : Monad<HKTList.K> {
    override fun <A> pure(a: A): HKT<HKTList.K, A> {
        return HKTList(listOf(a))
    }

    override fun <A, B> HKT<HKTList.K, A>.flatMap(f: (A) -> HKT<HKTList.K, B>): HKT<HKTList.K, B> {
        return this.narrow().value.stream()
                .flatMap { f(it).narrow().value.stream() }
                .collect(HKTList.K.collector())
    }
}

object HKTListMonadTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val listA: HKT<HKTList.K, Int> = HKTList(listOf(1, 2, 3))
        val func: (Int) -> HKT<HKTList.K, String> = {
            HKTList(listOf("s${it + 1}", "s${it + 2}"))
        }
        val listB: HKT<HKTList.K, String> = listA.flatMap(func)

        println(listA.narrow().value)   // [1, 2, 3]
        println(listB.narrow().value)   // [s2, s3, s3, s4, s4, s5]
    }
}

// Maybe Monad
object AddI {
    operator fun Maybe<Int>.plus(other: Maybe<Int>): Maybe<Int> {
        return when {
            this.value == null || other.value == null -> Maybe()
            else -> Maybe(this.value + other.value)
        }
    }
}


data class Maybe<A>(val value: A? = null) : HKT<Maybe.K, A> {
    object K {
        fun <A> HKT<K, A>.narrow(): Maybe<A> = this as Maybe<A>
    }
}

object MaybeMonad : Monad<Maybe.K> {
    override fun <A> pure(a: A): HKT<Maybe.K, A> = Maybe(a)

    override fun <A, B> HKT<Maybe.K, A>.flatMap(f: (A) -> HKT<Maybe.K, B>): HKT<Maybe.K, B> {
        val a: A? = this.narrow().value
        return a?.let(f) ?: Maybe()
    }
}

object AddM {
    operator fun Maybe<Int>.plus(other: Maybe<Int>): Maybe<Int> {
        return this.flatMap { a ->
            other.flatMap { b ->
                MaybeMonad.pure(a + b)
            }
        }.narrow()
    }
}

object MaybeTest {
    @JvmStatic
    fun main(args: Array<String>) {
        AddI.run {
            assertEquals(Maybe(7), Maybe(3) + Maybe(4))
            assertEquals(Maybe(), Maybe<Int>() + Maybe(3) + Maybe(4))
        }
        AddM.run {
            assertEquals(Maybe(7), Maybe(3) + Maybe(4))
            assertEquals(Maybe(), Maybe<Int>() + Maybe(3) + Maybe(4))
        }
    }
}