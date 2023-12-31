package n3p

import java.util.AbstractList

fun <T> AbstractList<T>.endsWith(other: List<T>): Boolean {
    if (other.size > size) return false
    val iterator = listIterator(size)
    for (i in other.size - 1 downTo 0) {
        if (iterator.previous() != other[i]) return false
    }
    return true
}
