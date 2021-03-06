# 十分钟魔法练习：单子

### By 「玩火」，改写「qbosen」

> 前置技能：Kotlin基础，HKT

## 单子

单子(Monad)是指一种有一个类型参数的数据结构，拥有 `pure` （也叫 `unit` 或者 `return` ）和 `flatMap` （也叫 `bind` 或者 `>>=` ）两种操作：

```kotlin
interface Monad<F> {
    fun <A> pure(a: A): HKT<F, A>
    fun <A, B> HKT<F, A>.flatMap(f: (A) -> HKT<F, B>): HKT<F, B>
}
```

其中 `pure` 要求返回一个包含参数类型内容的数据结构， `flatMap` 要求把 `ma` 中的值经过 `f` 以后再串起来。

举个最经典的例子：

## List Monad

```kotlin
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
```

简单来说 `pure(v)` 将得到 `{v}` ，而 `flatMap({1, 2, 3}, v -> {v + 1, v + 2})` 将得到 `{2, 3, 3, 4, 4, 5}` 。这都是 Java 里面非常常见的操作了，并没有什么新意。

测试：

```kotlin
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
```



## Maybe Monad

Java 不是一个空安全的语言，也就是说任何对象类型的变量都有可能为 `null` 。对于一串可能出现空值的逻辑来说，判空常常是件麻烦事：

```kotlin
object AddI {
    operator fun Maybe<Int>.plus(other: Maybe<Int>): Maybe<Int> {
        return when {
            this.value == null || other.value == null -> Maybe()
            else -> Maybe(this.value + other.value)
        }
    }
}
```

其中 `Maybe` 是个 `HKT` 的包装类型：

```kotlin
class Maybe<A>(val value: A? = null) : HKT<Maybe.K, A> {
    object K {
        fun <A> HKT<K, A>.narrow(): Maybe<A> = this as Maybe<A>
    }
}
```

像这样定义 `Maybe Monad` ：

```kotlin
object MaybeMonad : Monad<Maybe.K> {
    override fun <A> pure(a: A): HKT<Maybe.K, A> = Maybe(a)

    override fun <A, B> HKT<Maybe.K, A>.flatMap(f: (A) -> HKT<Maybe.K, B>): HKT<Maybe.K, B> {
        val a: A? = this.narrow().value
        return a?.let(f) ?: Maybe()
    }
}
```

上面 `addI` 的代码就可以改成：

```kotlin
object AddM {
    operator fun Maybe<Int>.plus(other: Maybe<Int>): Maybe<Int> {
        return this.flatMap { a ->
            other.flatMap { b ->
                MaybeMonad.pure(a + b)
            }
        }.narrow()
    }
}

```

这样看上去就比上面的连续 `if-return` 优雅很多。在一些有语法糖的语言 (`Haskell`) 里面 Monad 的逻辑可以更加简单明了。

> > kotlin强大的表达能力导致这个例子并没有那么优雅。。

> 我知道会有人说，啊，我有更简单的写法：
>
> > 错误示范就不用改写成kotlin了吧
> 
> ```java
>    static Maybe<Integer> addE(Maybe<Integer> ma, Maybe<Integer> mb) {
>     try { 
>       return new Maybe<>(ma.value + mb.value);
>     } catch (Exception e) {
>         return new Maybe<>();
>     }
> }
> ```
>
> 确实，这样写也挺简洁直观的， `Maybe Monad` 在有异常的 Java 里面确实不是一个很好的例子，不过 `Maybe Monad` 确实是在其他没有异常的函数式语言里面最为常见的 Monad 用法之一。而之后我也会介绍一些异常也无能为力的 Monad 用法。



测试：

```kotlin

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
```

