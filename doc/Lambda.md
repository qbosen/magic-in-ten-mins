# 十分钟魔法练习：λ 演算

### By 「玩火」

> 前置技能：Java 基础，ADT

## Intro

程序员们总是为哪种语言更好而争论不休，而强悍的大佬也为自己造出语言而感到高兴。造语言也被称为程序员的三大浪漫之一。这样一项看上去高难度的活动总是让萌新望而生畏，接下来我要介绍一种世界上最简单的**图灵完备**语言并给出 100 行Java代码的解释器实现。让萌新也能体验造语言的乐趣。

## λ演算

1936年，丘奇(Alonzo Church)提出了一种非常简单的计算模型，叫λ演算(Lambda Calculus)。

> 一些不严谨的通俗理解：
>
> λ表达式中的函数定义 `(λ x. E)` 就是定义了数学上的函数 `f(x)=E` ，只不过没有名字， `λ` 代表一个函数定义的开始，而 `.` 左边的是函数的自变量，可以是任意符号，这里用了 `x` ， `.` 的右边是函数的内容 `E` ，可以是任意 λ 表达式。
>
> 而函数应用 `F X` 就是对于一个数学上的函数 `F` 求值 `F(X)` ， `F` 就是函数， `X` 就是参数。比如 `(λ x. x)` 就是 `f(x)=x` ，比如 `(λ x. (x x))` 可以表示为 `f(x) = x(x)` ，其中 `x` 应当是个函数，不过这在数学里面是不允许的，而 `((λ x. (x x)) y)` 就可以表示为数学上的 `f(x) = x(x), f(y)` 也就是 `y(y)` 。
>
> 和传统数学函数最不一样的是λ演算里面的函数可以在任何位置被定义并且没有名字，并且可以被当作变量传递也可以作为函数的计算结果。

一个λ表达式有三种组成可能：变量 `x` 、函数定义 `(λ x. E)` 、函数应用 `(F X)` 。其中 `x` 是一个抽象的符号， `E, F, X` 是 λ 表达式。注意这是递归的定义，我们可以通过组合三种形式来构造复杂的 λ 表达式。比如 `((λ x. (x x)) y)` 整体是一个函数应用，其 `F` 是函数定义 `(λ x. (x x))` ， `X` 是 `y` ，而 `(λ x. (x x))` 函数定义的 `x` 是变量 `x` ， `E` 是 `(x x)` 。

λ表达式的计算也称为归约 (reduce) ，只需要将函数应用整体变换，变换结果为其作为函数定义的第一项 `F` (也就是 `(λ x. E)` ) 中 `E` 里出现的所有**自由**的 `x` 替换为其第二项 `X` ，也就是说 `((λ x. E) X)` 会被归约为 `E(x → X)` ，。听上去挺复杂，举个最简单的例子 `((λ x. (x x)) y)` 可以归约为 `(y y)` 。我这里提到了自由的 `x` ，意思是说它不是任何λ函数定义的自变量，比如 `(λ x. (x t))` 中的 `x` 就是不自由的， `t` 就是自由的。

函数定义有比函数应用更低的优先级，也就是说是 `(λ x. (x x))` 可以写成 `(λ x. x x)` 。函数应用是左结合的，所以 `((x x) x)` 可以写成 `(x x x)` 。

## 解释器

首先，我们要用 ADT 定义出 λ 表达式的数据结构：

```kotlin

interface Expr

// Value 变量
data class Val(val x: String, val id: UUID? = null) : Expr {
    override fun toString(): String = x
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Val
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}

// Function 函数定义
data class Fun(val x: Val, val e: Expr) : Expr {
    override fun toString(): String = "(λ $x. $e)"

    constructor(x: String, e: Expr) : this(Val(x), e)
}

// Apply 函数应用
data class App(val f: Expr, val x: Expr) : Expr {
    override fun toString(): String = "($f $x)"
}
```

> 注意到上面代码中 `Val` 有一个类型为 `UUID` 的字段，同时 `equals` 函数只比较 `id` 字段，这个字段是用来区分相同名字的不同变量的。如果不做区分那么对于下面的 λ 表达式：
>
> ```
> λ z. (λ x. (λ z. x)) z
> ```
>
> 会被规约成
>
> ```
> λ z. (λ z. z)
> ```
>
> 然而实际上最内层的 `z` 最开始是被最外层的函数定义定义的，而这里它被内层的函数定义错误地捕获（Capture）了，所以正确的规约结果应该是：
>
> ```
> λ z'. (λ z. z')
> ```

然后就可以构造 λ 表达式了，比如 `(λ x. x (λ x. x)) y` 就可以这样构造：

```kotlin
val expr = App(Fun("x", App(Val("x"), Fun("x", Val("x")))), Val("y"))
```

然后就可以定义归约函数 `reduce` 和应用自由变量函数 `apply` 还有用来生成 `UUID` 的 `genUUID` 函数和 `applyUUID` 函数：

```kotlin

interface Expr {
    fun reduce(): Expr
    fun apply(v: Val, ex: Expr): Expr
    fun genUUID(): Expr
    fun applyUUID(v: Val): Expr
}

// Value 变量
data class Val(val x: String, val id: UUID? = null) : Expr {
    override fun toString(): String = x
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Val
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun reduce(): Expr = this

    override fun apply(v: Val, ex: Expr): Expr = if (this == v) ex else this

    override fun genUUID(): Expr = this

    override fun applyUUID(v: Val): Expr = if (x == v.x) Val(x, v.id) else this
}

// Function 函数定义
data class Fun(val x: Val, val e: Expr) : Expr {
    override fun toString(): String = "(λ $x. $e)"

    constructor(x: String, e: Expr) : this(Val(x), e)

    override fun reduce(): Expr = this

    override fun apply(v: Val, ex: Expr): Expr = if (v == x) this else Fun(x, e.apply(v, ex))

    override fun genUUID(): Expr {
        if (x.id == null) {
            val v = Val(x.x, UUID.randomUUID())
            return Fun(v, e.applyUUID(v).genUUID())
        }
        return Fun(x, e.genUUID())
    }

    override fun applyUUID(v: Val): Expr {
        return if (x.x == v.x) this else Fun(x, e.applyUUID(v))


    }
}

// Apply 函数应用
data class App(val f: Expr, val x: Expr) : Expr {
    override fun toString(): String = "($f $x)"

    override fun reduce(): Expr {
        val fr = f.reduce()
        if (fr is Fun) {
            val (x1, e) = fr
            return e.apply(x1, x).reduce()
        }
        return App(fr, x)
    }

    override fun apply(v: Val, ex: Expr): Expr {
        return App(f.apply(v, ex), x.apply(v, ex))
    }

    override fun genUUID(): Expr {
        return App(f.genUUID(), x.genUUID())
    }

    override fun applyUUID(v: Val): Expr {
        return App(f.applyUUID(v), x.applyUUID(v))
    }
}

```

注意在 `reduce` 一个表达式之前应该先调用 `genUUID` 来生成变量标签否则会抛出空指针异常。

以上就是 100 行 Java 写成的解释器啦！



测试：

```kotlin
object LambdaTest {
    @JvmStatic
    fun main(args: Array<String>) {
        // `(λ x. x (λ x. x)) y`
        val fx = Fun("x", Val("x")) // (λ x. x)
        val expr = App(Fun("x", App(Val("x"), fx)), Val("y"))
        println(expr)   // ((λ x. (x (λ x. x))) y)

        // λ z. (λ x. (λ z. x)) z
        val expr2 = App(Fun("z", Fun("x", Fun("z", Val("x")))), Val("z"))
        println(expr2.reduce()) // (λ x. (λ z. x))
    }
}
```



