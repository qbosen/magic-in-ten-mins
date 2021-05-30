package stlc

import kotlin.test.assertFailsWith


/**
 * @author qiubaisen
 * @date 2021/5/30
 */

interface Type

// BaseType
data class TVal(val name: String) : Type {
    override fun toString(): String = name
}

// FunctionType
data class TArr(val src: Type, val tar: Type) : Type {
    override fun toString(): String = "($src -> $tar)"
}

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

class BadTypeException : RuntimeException()
interface Env {
    @Throws(BadTypeException::class)
    fun lookup(s: String): Type
}

object NilEnv : Env {
    override fun lookup(s: String): Type {
        throw BadTypeException()
    }
}

class ConsEnv(val v: Val, val next: Env) : Env {
    override fun lookup(s: String): Type {
        return if (s == v.x && v.type != null) v.type else next.lookup(s)
    }
}

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