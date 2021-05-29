package hkt

import hkt.HKTList.K.narrow
import java.util.stream.Collector
import java.util.stream.Collectors

/**
 * @author qiubaisen
 * @date 2021/5/29
 */

// 高阶类型
interface HKT<F, A>

interface Functor<F> {
    fun <A, B> HKT<F, A>.map(f: (A) -> B): HKT<F, B>
}

class HKTList<A>(val value: List<A> = ArrayList()) : HKT<HKTList.K, A> {
    object K {
        fun <A> HKT<K, A>.narrow() = this as HKTList<A>
        fun <A> collector(): Collector<A, *, HKTList<A>> {
            return Collectors.collectingAndThen(Collectors.toList(), ::HKTList)
        }
    }
}

object ListF : Functor<HKTList.K> {
    override fun <A, B> HKT<HKTList.K, A>.map(f: (A) -> B): HKT<HKTList.K, B> {
        return this.narrow().value.stream().map(f).collect(HKTList.K.collector())
    }
}

object ListFTest {
    @JvmStatic
    fun main(args: Array<String>) {
        """output:
            [1, 2, 3]
            [s1, s2, s3]
        """.trimIndent()

        val listA: HKT<HKTList.K, Int> = HKTList(listOf(1, 2, 3)).also { println(it.value) }
        val listB: HKT<HKTList.K, String> = ListF.run { // 使用 receiver 机制导入 object对象 中定义的扩展方法
            listA.map { "s$it" }.narrow()
        }.also { println(it.value) }
    }
}
