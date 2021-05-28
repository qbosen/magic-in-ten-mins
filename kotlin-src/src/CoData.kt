package codata

/**
 * 余代数数据类型（Coalgebraic Data Type）
 * 余归纳数据类型（Coinductive Data Type）
 * @author  qiubaisen
 * @date  2021/5/28
 */

class InfIntList(val head: Int, val next: () -> InfIntList)

object Codata {
    fun infAlt(): InfIntList = InfIntList(1) {
        InfIntList(2, Codata::infAlt)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        println(infAlt().next().head)   // 2

        cyclePrint()    // 1->2->1->2->1->2->1->2->1->2
    }

    private fun cyclePrint() {
        val bound = 10
        var infList = infAlt()
        for (i in 1..bound) {
            print(infList.head)
            infList = infList.next()
            if (i != bound) {
                print("->")
            }
        }
    }
}