# 十分钟魔法练习：状态单子

### By 「玩火」，改写「qbosen」

> 前置技能：Kotlin基础，HKT，Monad

## 函数容器

很显然Java标准库中的各类容器都是可以看成是单子的， `Stream` 类也给出了这些类的 `flatMap` 实现。不过在函数式的理论中单子不仅仅可以是实例意义上的容器，也可以是其他抽象意义上的容器，比如函数。

对于一个形如` Function<S, Complex<A>>` 形式的函数来说，我们可以把它看成包含了一个 `A` 的惰性容器，只有在给出 `S` 的时候才能知道 `A` 的值。对于这样形式的函数我们同样能写出对应的 `flatMap` ，这里就拿状态单子举例子。

## 状态单子

状态单子（State Monad）是一种可以包含一个“可变”状态的单子，我这里用了引号是因为尽管状态随着逻辑流在变化但是在内存里面实际上都是不变量。

其本质就是在每次状态变化的时候将新状态作为代表接下来逻辑的函数的输入。比如对于：

```java
i = i + 1;
System.out.println(i);
```

可以用状态单子的思路改写成：

```java
(v -> System.out.println(v)).apply(i + 1);
```

最简单的理解就是这样的一个包含函数的对象：

```kotlin
typealias HKT2<F, A, B> = HKT<HKT<F, A>, B>

data class StateData<A, S>(val value: A, val state: S)

class State<S, A>(val runState: (S) -> StateData<A, S>) : HKT2<State.K, S, A> {
    object K {
        fun <S, A> HKT2<K, S, A>.narrow() = this as State<S, A>
    }
}
```

这里为了好看定义了一个 `StateData` 类，包含变化后的状态和计算结果。而最核心的就是 `runState` 函数对象，通过组合这个函数对象来使变化的状态在逻辑间传递。

`State` 是一个 Monad （注释中是简化的伪代码）：

```kotlin
class StateMonad<S> : Monad<HKT<State.K, S>> {
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
```

`pure` 操作直接返回当前状态和给定的值， `flatMap` 操作只需要把 `ma` 中的 `A` 取出来然后传给 `f` ，并处理好 `state` 。

仅仅这样的话 `State` 使用起来并不方便，还需要定义一些常用的操作来读取写入状态：

```kotlin
// class StateM<S>
// 读取
val get: HKT2<State.K, S, S> = State { s -> StateData(s, s) }
// 写入
fun put(s: S): HKT2<State.K, S, S> = State { StateData(it, s) }
// 修改
fun modify(f: (S) -> S): HKT2<State.K, S, S> = State { StateData(it, f(it)) }
```

使用的话这里举个求斐波那契数列的例子：

```kotlin
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
```

`fib` 函数对应的 Haskell 代码是：

```haskell
fib :: Int -> State (Int, Int) Int
fib 0 = do
  (_, x) <- get
  pure x
fib n = do
  modify (\(a, b) -> (b, a + b))
  fib (n - 1)
```

~~看上去比 Java 版简单很多~~

## 有啥用

看到这里肯定有人会拍桌而起：求斐波那契数列我有更简单的写法！

```java
static int fib(int n) {
    int[] a = {0, 1, 1};
    for (int i = 0; i < n - 2; i++)
        a[(i + 3) % 3] = a[(i + 2) % 3] + 
                         a[(i + 1) % 3];
    return a[n % 3];
}
```

但问题是你用变量了啊， `State Monad` 最妙的一点就是全程都是常量而模拟出了变量的感觉。

更何况你这里用了数组而不是在递归，如果你递归就会需要在 `fib` 上加一个状态参数， `State Monad` 可以做到在不添加任何函数参数的情况下在函数之间传递参数。

同时它还是纯的，也就是说是**可组合**的，把任意两个状态类型相同的 `State Monad` 组合起来并不会有任何问题，比全局变量的解决方案不知道高到哪里去。



