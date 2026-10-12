package com.nextdaymobilestudio.lebenindeutschland.core.content.sync

import java.security.MessageDigest
import kotlinx.serialization.json.JsonPrimitive

internal object CanonicalJson {
    fun encode(value: Any?): String = buildString {
        appendCanonical(value)
    }

    fun sha256(value: Any?): String = MessageDigest
        .getInstance("SHA-256")
        .digest(encode(value).toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }

    private fun StringBuilder.appendCanonical(value: Any?) {
        when (value) {
            null -> append("null")
            is String -> append(JsonPrimitive(value).toString())
            is Boolean -> append(value)
            is Byte, is Short, is Int, is Long -> append(value.toString())
            is Float -> appendFloatingPoint(value.toDouble())
            is Double -> appendFloatingPoint(value)
            is Number -> append(value.toString())
            is Map<*, *> -> {
                val entries = value.entries
                    .map { entry ->
                        val key = entry.key as? String
                            ?: throw ContentSyncException("Canonical JSON object keys must be strings")
                        key to entry.value
                    }
                    .sortedBy { (key, _) -> key }

                append('{')
                entries.forEachIndexed { index, (key, nestedValue) ->
                    if (index > 0) append(',')
                    append(JsonPrimitive(key).toString())
                    append(':')
                    appendCanonical(nestedValue)
                }
                append('}')
            }
            is Iterable<*> -> {
                append('[')
                value.forEachIndexed { index, nestedValue ->
                    if (index > 0) append(',')
                    appendCanonical(nestedValue)
                }
                append(']')
            }
            is Array<*> -> appendCanonical(value.asList())
            else -> throw ContentSyncException(
                "Unsupported canonical JSON value: ${value::class.java.name}",
            )
        }
    }

    private fun StringBuilder.appendFloatingPoint(value: Double) {
        if (!value.isFinite()) {
            throw ContentSyncException("Canonical JSON does not support non-finite numbers")
        }
        append(value.toString().replace('E', 'e'))
    }
}
