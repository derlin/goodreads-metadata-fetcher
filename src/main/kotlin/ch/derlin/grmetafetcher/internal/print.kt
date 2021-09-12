package ch.derlin.grmetafetcher.internal

import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties

@Suppress("UNCHECKED_CAST")
internal fun <T : Any> ppDataClass(data: T, indent: Int = 2): String {
    val klass = data::class as KClass<T>
    // store all declared properties in a map as key=value
    val propsInObject = klass.declaredMemberProperties
        .associate { it.name to it.get(data) }
    // extract the properties present from the toString() method, preserving order
    val orderedPropsInToString = "([A-Za-z0-9_]+)=".toRegex()
        .findAll(data.toString()).map { it.groupValues[1] }

    return with(StringBuilder()) {
        val spaces = " ".repeat(indent) // indent
        appendLine(klass.simpleName + "(")
        orderedPropsInToString.forEach { propName ->
            val value = ppValue(propsInObject[propName])
            appendLine("""$spaces$propName=$value,""")
        }
        appendLine(")") // close the class name
        toString()
    }
}

private fun ppValue(value: Any?): String {
    if (value == null) return "null"
    return when (value) {
        is String -> "\"$value\""
        is LocalDate -> "LocalDate.parse(\"$value\")"
        is List<*> -> "listOf(" + value.joinToString(", ") { ppValue(it) } + ")"
        else -> "$value"
    }
}