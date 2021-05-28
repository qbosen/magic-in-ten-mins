package gadt

/**
 * @author  qiubaisen
 * @date  2021/5/28
 */
interface Expr<T>
class IVal(val value: Int) : Expr<Int>
class BVal(val value: Boolean) : Expr<Boolean>
class Add(val e1: Expr<Int>, val e2: Expr<Int>) : Expr<Int>
class Eq<T>(val e1: Expr<T>, val e2: Expr<T>) : Expr<Boolean>