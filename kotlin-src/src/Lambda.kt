package lambda

import java.util.*


/**
 * @author qiubaisen
 * @date 2021/5/30
 */

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