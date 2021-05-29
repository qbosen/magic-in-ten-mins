package statemonad

import hkt.HKT
import monad.Monad
import statemonad.State.K.narrow

/**
 * @author qiubaisen
 * @date 2021/5/30
 */

typealias HKT2<F, A, B> = HKT<HKT<F, A>, B>

data class StateData<A, S>(val value: A, val state: S)

class State<S, A>(val runState: (S) -> StateData<A, S>) : HKT2<State.K, S, A> {
    object K {
        fun <S, A> HKT2<K, S, A>.narrow() = this as State<S, A>
    }
}

class StateMonad<S> : Monad<HKT<State.K, S>> {
    // 返回当前状态 和 给定的值
    override fun <A> pure(a: A): HKT2<State.K, S, A> {
        return State { s -> StateData(a, s) }
    }

    //fun <A,B> State<S, A>.flatMap(f: (A) -> State<S,B>) : State<S, B>
    override fun <A, B> HKT2<State.K, S, A>.flatMap(f: (A) -> HKT2<State.K, S, B>): HKT2<State.K, S, B> {
        return State { s ->
            val r: StateData<A, S> = this.narrow().runState(s)
            f(r.value).narrow().runState(r.state)
        }
    }

    val get: HKT2<State.K, S, S> = State { s -> StateData(s, s) }
    fun put(s: S): HKT2<State.K, S, S> = State { StateData(it, s) }
    fun modify(f: (S) -> S): HKT2<State.K, S, S> = State { StateData(it, f(it)) }
}

fun fib(n: Int): State<Pair<Int, Int>, Int> {
    val monad = StateMonad<Pair<Int, Int>>()

    monad.run {
        return when (n) {
            0 -> get.flatMap { pure(it.first) }.narrow()
            1 -> get.flatMap { pure(it.second) }.narrow()
            else -> modify { Pair(it.second, it.first + it.second) }.flatMap { fib(n - 1) }.narrow()
        }
    }
}

object FibTest {
    @JvmStatic
    fun main(args: Array<String>) {
        println(fib(7).runState(Pair(0, 1)).value)  // 13
    }
}