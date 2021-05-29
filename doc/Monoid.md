# 十分钟魔法练习：单位半群

### By 「玩火」，改写「qbosen」

> 前置技能：kotlin基础

## 半群（Semigroup）

半群是一种代数结构，在集合 `A` 上包含一个将两个 `A` 的元素映射到 `A` 上的运算即 `<> : (A, A) -> A​` ，同时该运算满足**结合律**即 `(a <> b) <> c == a <> (b <> c)` ，那么代数结构 `{<>, A}` 就是一个半群。

比如在自然数集上的加法或者乘法可以构成一个半群，再比如字符串集上字符串的连接构成一个半群。

## 单位半群（Monoid）

单位半群是一种带单位元的半群，对于集合 `A` 上的半群 `{<>, A}` ， `A` 中的元素 `a` 使 `A` 中的所有元素 `x` 满足 `x <> a` 和 `a <> x` 都等于 `x`，则 `a` 就是 `{<>, A}` 上的单位元。

举个例子， `{+, 自然数集}` 的单位元就是 0 ， `{*, 自然数集}` 的单位元就是 1 ， `{+, 字符串集}` 的单位元就是空串 `""` 。

用 Kotlin 代码可以表示为：

```kotlin
interface Monoid<T> {
    val zero: T
    fun append(a: T, b: T): T
    fun appends(x: Iterable<T>): T = x.fold(zero, this::append)
}
```

## 应用：Optional

在 Java8 中有个新的类 `Optional` 可以用来表示可能有值的类型，而我们可以对它定义个 Monoid ：

```kotlin
class OptionalM<T> : Monoid<Optional<T>> {
    override val zero: Optional<T> = Optional.empty()
    override fun append(a: Optional<T>, b: Optional<T>): Optional<T> = if (a.isPresent) a else b
}

```

这样对于 appends 来说我们将获得一串 Optional 中第一个不为空的值，对于需要进行一连串尝试操作可以这样写：

```kotlin
OptionalM<Int>().appends(listof(try1(), try2(), try3(), try4()))
```

## 应用：Ordering

对于 `Comparable` 接口可以构造出：

```kotlin
class OrderingM : Monoid<Int> {
    override val zero: Int = 0
    override fun append(a: Int, b: Int): Int = if (a != 0) a else b
}
```

同样如果有一串带有优先级的比较操作就可以用 appends 串起来，比如：

```kotlin
data class Student(
        val name: String,
        val sex: String,
        val birthday: Date,
        val from: String
) : Comparable<Student> {
    override fun compareTo(other: Student): Int {
        return OrderingM().appends(listOf(
                name.compareTo(other.name),
                sex.compareTo(other.sex),
                birthday.compareTo(other.birthday),
                from.compareTo(other.from),
        ))
    }
}
```

这样的写法比一连串 `if-else` 优雅太多。

## 扩展

在 Monoid 接口里面加 default 方法可以支持更多方便的操作：

```kotlin
interface MonoidEx<T> {
    val zero: T
    fun T.append(other: T): T
    fun sumOf(x: Iterable<T>): T = x.fold(zero) { a, b -> a.append(b) }
    fun only(condition: Boolean, then: T): T = if (condition) then else zero
    fun cond(condition: Boolean, then: T, els: T): T = if (condition) then else els
}

typealias Runnable = () -> Unit

class Todo : MonoidEx<Runnable> {
    override val zero: Runnable = {}

    override fun Runnable.append(other: Runnable): Runnable = { this(); other() }
}
```

然后就可以像下面这样使用上面的定义:

```kotlin
object TodoTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val logic1: Runnable = { println("1") }
        val logic2: Runnable = { println("2") }
        val logic3: Runnable = { println("3") }
        val logic4: Runnable = { println("4") }
        val todos = Todo().sumOf(listOf(
                logic1,
                { logic2() },
                Todo().cond(false, logic3, logic4)
        ))
        todos.invoke()  // 1 2 4
    }
}
```

> 注：上面的 Optional 并不是 lazy 的，实际运用中加上非空短路能提高效率。