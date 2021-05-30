# 十分钟魔法练习：简单类型 λ 演算

### By 「玩火」，改写「qbosen」

> 前置技能：Kotlin 基础，ADT，λ 演算

## 简单类型 λ 演算

简单类型 λ 演算（Simply-Typed Lambda Calculus）是在无类型 λ 演算（Untyped Lambda Calculus）的基础上加了个非常简单的类型系统。

这个类型系统包含两种类型结构，一种是内建的基础类型 `T` ，一种是函数类型 `A → B` ，其中函数类型由源类型 `A` 和目标类型 `B` 组成：

```
Type = BaseType + FunctionType
FunctionType = Type * Type
```

注意函数类型的符号是右结合的，也就是说 `A → A → A` 等价于 `A → (A → A)` 。

用 Kotlin 代码可以表示为：

```kotlin
interface Type

// BaseType
data class TVal(val name: String) : Type {
    override fun toString(): String = name
}

// FunctionType
data class TArr(val src: Type, val tar: Type) : Type {
    override fun toString(): String = "($src -> $tar)"
}
```

## 年轻人的第一个 TypeChecker

然后需把类型嵌入到 λ 演算的语法树中：

```kotlin
interface Expr
data class Val(val x: String, val type: Type? = null) : Expr
data class Fun(val x: Val, val e: Expr) : Expr
data class App(val f: Expr, val x: Expr) : Expr
```

注意只有函数定义的变量需要标记类型，表达式的类型是可以被简单推导出的。同时还需要一个环境来保存定义变量的类型（其实是一个不可变链表）：

```kotlin
class BadTypeException : RuntimeException()
interface Env {
    @Throws(BadTypeException::class)
    fun lookup(s: String): Type
}

class NilEnv : Env {
    override fun lookup(s: String): Type {
        throw BadTypeException()
    }
}

class ConsEnv(val v: Val, val next: Env) : Env {
    override fun lookup(s: String): Type {
        return if (s == v.x && v.type != null) v.type else next.lookup(s)
    }
}
```

而对于这样简单的模型，类型检查只需要判断 `F X` 中的 `F` 需要是函数类型，并且 `(λ x. F) E` 中 `x` 的类型和 `E` 的类型一致。

而类型推导也很简单：变量的类型就是它被标记的类型；函数定义的类型就是以它变量的标记类型为源，它函数体的类型为目标的函数类型；而函数应用的类型就是函数的目标类型，在能通过类型检查的情况下。

以上用 Kotlin 代码描述就是：

```kotlin
interface Expr {
    fun checkType(env: Env): Type
}

data class Val(val x: String, val type: Type? = null) : Expr {
    override fun checkType(env: Env): Type {
        return type ?: env.lookup(x)
    }
}

data class Fun(val x: Val, val e: Expr) : Expr {
    override fun checkType(env: Env): Type {
        return TArr(x.checkType(env), e.checkType(ConsEnv(x, env)))
    }
}

data class App(val f: Expr, val x: Expr) : Expr {
    override fun checkType(env: Env): Type {
        val tf: Type = f.checkType(env)
        if (tf is TArr && tf.src == x.checkType(env)) {
            return tf.tar
        }
        throw BadTypeException()
    }
}
```

下面的测试代码对

 ````
(λ (x: int). (λ (y: int → bool). (y x)))
 ````

进行了类型检查，会打印输出 `(int → ((int → bool) → bool))` ：

```kotlin
object STLambda {
    @JvmStatic
    fun main(args: Array<String>) {
        // (λ (x: int). (λ (y: int → bool). (y x)))
        val x = Val("x", TVal("int"))   // (x:int)
        val y = Val("y", TArr(TVal("int"), TVal("bool")))   // (y:int->bool)
        val app = App(Val("y"), Val("x"))   // (y x)
        val f2 = Fun(y, app)    // (λ (y: int → bool). (y x))
        val f = Fun(x, f2)  // (λ (x: int). (λ (y: int → bool). (y x)))
        println(f.checkType(NilEnv))    // output: (int -> ((int -> bool) -> bool))

        // (λ (x: bool). (λ (y: int → bool). (y x)))
        val x2 = Val("x", TVal("bool"))
        assertFailsWith(BadTypeException::class) { println(Fun(x2, f2).checkType(NilEnv)) }
    }
}
```

而如果对

```
(λ (x: bool). (λ (y: int → bool). (y x)))
```

进行类型检查就会抛出 `BadTypeException` 。