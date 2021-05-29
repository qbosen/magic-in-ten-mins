# 十分钟魔法练习：高阶类型

### By 「玩火」，改写「qbosen」

> 前置技能：Kotlin基础

## 常常碰到的困难

写代码的时候常常会碰到语言表达能力不足的问题，比如下面这段用来给 `F` 容器中的值进行映射的代码：

> 这两个例子用 Java 描述，未用kotlin改写，一个意思

```java
interface Functor<F> {
    <A, B> F<B> map(F<A> a, Function<A, B> f);
}
```

并不能通过 `javac` 的编译，编译器会告诉你F不能有泛型参数。

最简单粗暴的解决方案就是放弃类型检查，全上 `Object` ，如：

```java
interface Functor<F> {
    Object map(Object a,  Function<Object, Object> f);
}
```

实际上Java经常这么干，标准库中到处是 `Object` 的身影，重载的各种接口也常常要手工转换类型， `equals` 要和 `Object` 比较， `compareTo` 要和 `Object` 比较……似乎习惯了以后也挺好，又不是不能用！

## 高阶类型

假设类型的类型是 `Type` ，比如 `int` 和 `String` 类型都是 `Type` 。

而对于 `List` 这样带有一个泛型参数的类型来说，它相当于一个把类型 `T` 映射到 `List<T>` 的函数，其类型可以表示为 `Type -> Type` 。

同样的对于 `Map` 来说它有两个泛型参数，类型可以表示为 `(Type, Type) -> Type` 。

像这样把类型映射到类型的非平凡类型就叫高阶类型（HKT, Higher Kinded Type）。

虽然Java中存在这样的高阶类型但是我们并不能用一个泛型参数表示出来，也就不能写出如上 `F<A>` 这样的代码了，因为 `F` 是个高阶类型。

> 如果加一层解决不了问题，那就加两层。

虽然在Java中不能直接表示出高阶类型，但是我们可以通过加一个中间层来在保留完整信息的情况下强类型地模拟出高阶类型。

首先，我们需要一个中间层：

```kotlin
interface HKT<F, A>
```

然后我们就可以用 `HKT<F, A>` 来表示 `F<A>` ，这样操作完 `HKT<F, A>` 后我们仍然有完整的类型信息来还原 `F<A>` 的类型。

这样，上面 `Functor` 就可以写成：

```kotlin
interface Functor<F> {
    fun <A, B> HKT<F, A>.map(f: (A) -> B): HKT<F, B>
}
```

这样就可以编译通过了。而对于想实现 `Functor` 的类，需要先实现 `HKT` 这个中间层，这里拿 `List` 举例：

```kotlin
class HKTList<A>(val value: List<A> = ArrayList()) : HKT<HKTList.K, A> {
    object K {
        fun <A> HKT<K, A>.narrow() = this as HKTList<A>
        fun <A> collector(): Collector<A, *, HKTList<A>> {
            return Collectors.collectingAndThen(Collectors.toList(), ::HKTList)
        }
    }
}
```

注意 `HKTList` 把 `HKTList.K` 作为了 `HKT` 的第一个参数来保存自己的类型信息，这样对于 `HKT<HKTList.K, T>` 这个接口来说就只有自己这一个子类，而在 `narrow` 函数中可以安全地把这个唯一子类转换回来。

这样，实现 `Functor` 类就是一件简单的事情了：

```java
object ListF : Functor<HKTList.K> {
    override fun <A, B> HKT<HKTList.K, A>.map(f: (A) -> B): HKT<HKTList.K, B> {
        return this.narrow().value.stream().map(f).collect(HKTList.K.collector())
    }
}
```



测试：

```kotlin
object ListFTest {
    @JvmStatic
    fun main(args: Array<String>) {
        """output:
            [1, 2, 3]
            [s1, s2, s3]
        """.trimIndent()

        val listA: HKT<HKTList.K, Int> = HKTList(listOf(1, 2, 3)).also { println(it.value) }
        val listB: HKT<HKTList.K, String> = ListF.run { 
          	// 使用 receiver 机制导入 object对象 中定义的扩展方法
            listA.map { "s$it" }.narrow()
        }.also { println(it.value) }
    }
}
```



