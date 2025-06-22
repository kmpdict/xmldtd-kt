package com.boswelja.xmldtd.codegen

internal fun String.toPascalCase(): String {
    return this
        .split("-", "_")
        .joinToString(separator = "") { segment ->
            val loweredSegment = if (segment.all { it.isUpperCase() }) {
                // If it looks like the segment is SCREAMING, then lower it
                segment.lowercase()
            } else {
                segment
            }
            loweredSegment.replaceFirstChar { it.uppercaseChar() }
        }
}

internal fun String.toCamelCase(): String {
    return this.toPascalCase().replaceFirstChar { it.lowercaseChar() }
}

internal fun String.stripPrefix(separator: String = ":"): String {
    return if (this.contains(separator)) {
        this.split(separator)[1]
    } else {
        this
    }
}

internal fun String.toPlural(): String {
    if (isEmpty()) return this

    val isUpper = last().isUpperCase()
    return if (endsWith("y", ignoreCase = true)) {
        this.dropLast(1) + if (isUpper) "IES" else "ies"
    } else if (endsWith("us", ignoreCase = true)) {
        this.dropLast(2) + if (isUpper) "I" else "i"
    } else if (endsWith("s", ignoreCase = true) ||
        endsWith("h", ignoreCase = true) ||
        endsWith("x", ignoreCase = true)) {
        this + if (isUpper) "ES" else "es"
    } else {
        this + if (isUpper) "S" else "s"
    }
}
