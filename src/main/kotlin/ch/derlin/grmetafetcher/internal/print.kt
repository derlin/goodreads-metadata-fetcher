package ch.derlin.grmetafetcher.internal

import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties

@Suppress("UNCHECKED_CAST")
internal fun <T : Any> ppDataClass(data: T): String {
    val klass = data::class as KClass<T>
    val propsInObject = klass.declaredMemberProperties.associate { it.name to it.get(data) }
    val orderedPropsInToString = "([A-Za-z0-9_]+)=".toRegex().findAll(data.toString()).map { it.groupValues[1] }
    val builder = StringBuilder()

    builder.appendLine(klass.simpleName + "(")
    orderedPropsInToString.forEach { propName ->
        val value = ppValue(propsInObject[propName])
        builder.appendLine("""  $propName=$value,""")
    }
    builder.appendLine(")")
    return builder.toString()
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