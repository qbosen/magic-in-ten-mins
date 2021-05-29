package monoid

import java.util.*

/**
 * @author  qiubaisen
 * @date  2021/5/28
 */

interface Monoid<T> {
    val zero: T
    fun append(a: T, b: T): T
    fun appends(x: Iterable<T>): T = x.fold(zero, this::append)
}

// Optional 的加法 表示 获取第一个不为空的值
class OptionalM<T> : Monoid<Optional<T>> {
    override val zero: Optional<T> = Optional.empty()
    override fun append(a: Optional<T>, b: Optional<T>): Optional<T> = if (a.isPresent) a else b
}

object OptionalMTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val opt = OptionalM<Int>()
        val res = opt.appends(listOf(Optional.empty(), Optional.of(1), Optional.of(2)))
        println(res)    // 1
    }
}

// Ordering 的加法 表示 获取第一个可以区分正负的值(大小)
class OrderingM : Monoid<Int> {
    override val zero: Int = 0
    override fun append(a: Int, b: Int): Int = if (a != 0) a else b
}

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

// 等效的比较器
val studentComparator = Comparator.comparing(Student::name)
        .thenComparing(Student::sex)
        .thenComparing(Student::birthday)
        .thenComparing(Student::from)

// 扩展
interface MonoidEx<T> {
    val zero: T
    fun T.append(other: T): T
    fun sumOf(x: Iterable<T>): T = x.fold(zero) { a, b -> a.append(b) }
    fun only(condition: Boolean, then: T): T = if (condition) then else zero
    fun cond(condition: Boolean, then: T, els: T): T = if (condition) then else els
}

typealias Runnable = () -> Unit     // 避免 SAM

class Todo : MonoidEx<Runnable> {
    override val zero: Runnable = {}

    override fun Runnable.append(other: Runnable): Runnable = { this(); other() }
}

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