# 十分钟魔法练习：广义代数数据类型

### By 「玩火」，改写「qbosen」

> 前置技能：Kotlin基础，ADT

在 ADT 中可以构造出如下类型：

```kotlin
interface Expr
class IVal(val value: Int) : Expr
class BVal(val value: Boolean) : Expr
class Add(val e1: Expr, val e2: Expr) : Expr
class Eq(val e1: Expr, val e2: Expr) : Expr
```

但是这样构造有个问题，很显然 `BVal` 是不能相加的。而这样的构造并不能防止构造出这样的东西。实际上在这种情况下ADT的表达能力是不足的。

一个比较显然的解决办法是给 `Expr` 添加一个类型参数用于标记表达式的类型：

```kotlin
interface Expr<T>
class IVal(val value: Int) : Expr<Int>
class BVal(val value: Boolean) : Expr<Boolean>
class Add(val e1: Expr<Int>, val e2: Expr<Int>) : Expr<Int>
class Eq<T>(val e1: Expr<T>, val e2: Expr<T>) : Expr<Boolean>
```

这样就可以避免构造出两个类型为 `Boolean` 的表达式相加，能构造出的表达式都是类型安全的。

注意到四个 `class` 的父类都不是 `Expr<T>` 而是包含参数的 `Expr` ，这和 ADT 并不一样。而这就是广义代数数据类型（Generalized Algebraic Data Type, GADT）。

