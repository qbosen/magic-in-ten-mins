package adt

/**
 * @author  qiubaisen
 * @date  2021/5/28
 */

// Product type
class Student_(val name: String, val id: Int)

// Sum type
interface SchoolPerson
class Student(val name: String, val id: Int) : SchoolPerson
class Teacher(val name: String, val office: String) : SchoolPerson

// ADT 基础类型例子
interface Bool
class True : Bool
class False : Bool

// ADT 自然数的皮亚诺构造
interface Nat
object Z : Nat
data class S(val v: Nat) : Nat

val n_3 = S(S(S(Z)))

// ADT List链表
sealed class List<out T>
object Nil : List<Nothing>()
data class Cons<T>(val head: T, val tail: List<T>) : List<T>()

val list__1_3_4 = Cons(1, Cons(3, Cons(4, Nil)))

// ADT 解析JSON
sealed class JsonValue
class JsonBool(val value: Boolean) : JsonValue()
class JsonInt(val value: Int) : JsonValue()
class JsonString(val value: String) : JsonValue()
class JsonArray(val value: kotlin.collections.List<JsonValue>) : JsonValue()
class JsonMap(val value: Map<String, JsonValue>) : JsonValue()

fun main() {
    """ output:
        S(v=S(v=S(v=adt.Z@548c4f57)))
        Cons(head=1, tail=Cons(head=3, tail=Cons(head=4, tail=adt.Nil@1218025c)))
    """.trimIndent()
    println(n_3)
    println(list__1_3_4)
}