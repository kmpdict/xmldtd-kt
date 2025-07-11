package com.boswelja.xmldtd.deserialize

import kotlinx.io.Source
import kotlinx.io.readLine

internal val CommentRegex = Regex("<!--([\\s\\S\\n]*?)-->")

internal fun extractCommentFromNextLines(source: Source): String? {
    val nextLine = source.peek().readLine()
    if (nextLine != null && nextLine.contains("<!--")) {
        // There's a comment, let's grab it
        val commentLine = source.readLinesUntil { it.contains("-->") }!!
        val commentMatch = CommentRegex.find(commentLine)!!
        return commentMatch.groupValues[1].trim().takeUnless { it.isEmpty() }
    } else {
        return null
    }
}

internal data class ElementDto(
    val name: String,
    val children: List<String>,
    val isMixed: Boolean,
    val comment: String?,
) {
    companion object {
        internal val ElementRegex = Regex("<!ELEMENT\\s+([a-zA-Z0-9_]+)\\s+(\\(.+\\)\\*?|EMPTY|ANY)\\s*>")

        internal fun fromLine(line: String): ElementDto? {
            return ElementRegex.find(line)?.let { matchResult ->
                val name = matchResult.groupValues[1]
                val isMixed = matchResult.groupValues[2].endsWith("*")
                val children = if (isMixed) {
                    matchResult.groupValues[2]
                        .removeSurrounding("(", ")*")
                        .split("|")
                } else {
                    matchResult.groupValues[2]
                        .removeSurrounding("(", ")")
                        .split(",")
                        .map { it.trim() }
                }
                ElementDto(name, children, isMixed, null)
            }
        }
    }
}

internal data class AttributeDto(
    val elementName: String,
    val attributeName: String,
    val type: String,
    val value: String,
    val comment: String?,
) {
    companion object {
        internal val AttributeListRegex = Regex("<!ATTLIST\\s+([a-zA-Z0-9_-]+)\\s+([a-zA-Z0-9:_-]+)\\s+([a-zA-Z0-9_-]+)\\s+([\"#a-zA-Z0-9_-]+)\\s*>")

        internal fun fromLine(line: String): AttributeDto? {
            return AttributeListRegex.find(line)?.let { matchResult ->
                val elementName = matchResult.groupValues[1]
                val attrName = matchResult.groupValues[2]
                val type = matchResult.groupValues[3]
                val value = matchResult.groupValues[4]
                AttributeDto(
                    elementName = elementName,
                    attributeName = attrName,
                    type = type,
                    value = value,
                    comment = null,
                )
            }
        }
    }
}

internal data class InternalEntityDto(
    val name: String,
    val value: String,
    val comment: String?,
) {
    companion object {
        internal val InternalEntityMatcher = Regex("<!ENTITY\\s+([a-zA-Z0-9_-]+)\\s+\"(.+)\"\\s*>")

        internal fun fromLine(line: String): InternalEntityDto? {
            return InternalEntityMatcher.find(line)?.let { matchResult ->
                val name = matchResult.groupValues[1]
                val value = matchResult.groupValues[2]
                InternalEntityDto(name, value, null)
            }
        }
    }
}

internal data class ExternalEntityDto(
    val name: String,
    val uri: String,
    val comment: String?,
) {
    companion object {
        internal val ExternalEntityMatcher = Regex("<!ENTITY\\s+([a-zA-Z0-9_-]+)\\s+SYSTEM\\s+\"(.+)\"\\s*>")

        internal fun fromLine(line: String): ExternalEntityDto? {
            return ExternalEntityMatcher.find(line)?.let { matchResult ->
                val name = matchResult.groupValues[1]
                val uri = matchResult.groupValues[2]
                ExternalEntityDto(name, uri, null)
            }
        }
    }
}
