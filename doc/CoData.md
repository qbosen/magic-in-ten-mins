# 十分钟魔法练习：余代数数据类型

### By 「玩火」，改写「qbosen」

> 前置技能：Kotlin基础，ADT

## ADT 的局限性

很显然， ADT 可以构造任何树形的数据结构：树的节点内分支用和类型连接，层级间节点用积类型连接。

但是同样很显然 ADT 并不能搞出环形的数据结构或者说是无穷大小的数据结构。比如下面的代码：

```kotlin
val list : IntCons = IntCons(1, list);
```

编译器会表示 `list` 在使用时未初始化。

为什么会这样呢？ ADT 是归纳构造的，也就是说它必须从非递归的基本元素开始组合构造成更大的元素。

如果我们去掉这些基本元素那就没法凭空构造大的元素，也就是说如果去掉归纳的第一步那整个归纳过程毫无意义。

## 余代数数据类型

余代数数据类型（Coalgebraic Data Type）也就是余归纳数据类型（Coinductive Data Type），代表了自顶向下的数据类型构造思路，思考一个类型可以如何被分解从而构造数据类型。

这样在分解过程中再次使用自己这个数据类型本身就是一件非常自然的事情了。

不过在编程实现过程中使用自己需要加个惰性数据结构包裹，防止积极求值的语言无限递归生成数据。

比如一个列表可以被分解为第一项和剩余的列表：

```kotlin
class InfIntList(val head: Int, val next: () -> InfIntList)
```

这里的 `Supplier` 可以做到仅在需要 `next` 的时候才求值。使用的例子如下：

```kotlin
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
```

运行会输出 `2` 。注意，这里的 `infAlt` 从某种角度来看实际上就是个长度为 2 的环形结构。

用这样的思路可以构造出无限大的树、带环的图等数据结构。

不过以上都是对余代数数据类型的一种模拟，实际上在对其支持良好的语言都会自动加上 `Supplier` 来辅助构造，同时还能处理好对无限大（其实是环）的数据结构的无限递归变换（`map`, `fold` ...）的操作。