package com.boswelja.xmldtd.deserialize

import kotlinx.io.Source
import kotlinx.io.readLine

public fun DocumentTypeDefinition.Companion.fromSource(source: Source): DocumentTypeDefinition {
    // Find the start of the doctype (there might be xml tags and comments before the actual start)
    var line: String?
    do {
        line = source.readLine()
    } while (line != null && !line.matches(DoctypeRegex))
    requireNotNull(line) { "No <!DOCTYPE> tag found in input" }

    // Extract the root element name from the doctype line
    val rootElementName = DoctypeRegex.find(line)?.groupValues?.get(1)
        ?: throw IllegalArgumentException("Could not determine root element name from <!DOCTYPE> tag")

    val elements = mutableListOf<ElementDto>()
    val internalEntities = mutableListOf<InternalEntityDto>()
    val externalEntities = mutableListOf<ExternalEntityDto>()
    val attributes = mutableListOf<AttributeDto>()

    line = source.readLine()?.trim()
    while (line != null && line != "]>") {
        if (!line.startsWith("<")) {
            // We're not at the start of a tag, so we can skip this line
            line = source.readLine()?.trim()
            continue
        }
        if (!line.endsWith(">")) {
            // The tag spans multiple lines, let's grab them all
            val remainingLines = source.readLinesUntil { it.endsWith(">") }
            line = "$line $remainingLines"
        }

        // TODO this runs all checks every time - not good
        ElementRegex.find(line)?.let { matchResult ->
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
            elements.add(ElementDto(name, children, isMixed))
        }
        InternalEntityMatcher.find(line)?.let { matchResult ->
            val name = matchResult.groupValues[1]
            val value = matchResult.groupValues[2]
            internalEntities.add(InternalEntityDto(name, value))
        }
        ExternalEntityMatcher.find(line)?.let { matchResult ->
            val name = matchResult.groupValues[1]
            val uri = matchResult.groupValues[2]
            externalEntities.add(ExternalEntityDto(name, uri))
        }
        AttributeListRegex.find(line)?.let { matchResult ->
            val elementName = matchResult.groupValues[1]
            val attrName = matchResult.groupValues[2]
            val type = matchResult.groupValues[3]
            val value = matchResult.groupValues[4]
            attributes.add(AttributeDto(
                elementName = elementName,
                attributeName = attrName,
                type = type,
                value = value,
            ))
        }

        line = source.readLine()?.trim()
    }

    return buildTypeDefinition(
        rootElementName = rootElementName,
        elements = elements,
        internalEntities = internalEntities,
        externalEntities = externalEntities,
        attributes = attributes
    )
}

internal fun buildTypeDefinition(
    rootElementName: String,
    elements: List<ElementDto>,
    internalEntities: List<InternalEntityDto>,
    externalEntities: List<ExternalEntityDto>,
    attributes: List<AttributeDto>,
): DocumentTypeDefinition {
    val rootElementDto = elements.firstOrNull { it.name == rootElementName }
    requireNotNull(rootElementDto) { "No root element found matching `$rootElementName`! Checked $elements"}
    val rootElement = buildElementDefinition(rootElementDto, rootElementName, elements, attributes)
    val entities = internalEntities.map { Entity.Internal(it.name, it.value) } +
            externalEntities.map { Entity.External(it.name, it.uri) }

    return DocumentTypeDefinition(
        rootElement = rootElement,
        entities = entities
    )
}

internal fun buildChildElementDefinition(
    elementNameWithOccurs: String,
    elements: List<ElementDto>,
    attributes: List<AttributeDto>,
): ChildElementDefinition {
    val occurs = when {
        elementNameWithOccurs.endsWith("+") -> ChildElementDefinition.Occurs.AtLeastOnce
        elementNameWithOccurs.endsWith("*") -> ChildElementDefinition.Occurs.ZeroOrMore
        elementNameWithOccurs.endsWith("?") -> ChildElementDefinition.Occurs.AtMostOnce
        else -> ChildElementDefinition.Occurs.Once
    }
    val elementName = elementNameWithOccurs
        .removeSuffix("?")
        .removeSuffix("*")
        .removeSuffix("+")
    return if (elementName.startsWith("(")) {
        val children = elementName.removeSurrounding("(", ")")
            .split("|")
            .map { childName ->
                buildChildElementDefinition(childName.trim(), elements, attributes)
            }
        ChildElementDefinition.Either(
            occurs = occurs,
            options = children
        )
    } else {
        val elementName = elementName
            .removeSuffix("?")
            .removeSuffix("*")
            .removeSuffix("+")
        val element = elements.firstOrNull {
            it.name == elementName
        }
        requireNotNull(element) { "Couldn't find a child with the name $elementName in $elements" }
        val elementDefinition = buildElementDefinition(element, elementName, elements, attributes)
        ChildElementDefinition.Single(
            occurs = occurs,
            elementDefinition = elementDefinition
        )
    }
}

internal fun buildElementDefinition(
    element: ElementDto,
    elementName: String,
    elements: List<ElementDto>,
    attributes: List<AttributeDto>,
): ElementDefinition {
    val mappedAttrs = attributes
        .filter { it.elementName == elementName }
        .map { buildAttribute(it) }
    return when {
        element.isMixed -> {
            ElementDefinition.Mixed(
                elementName = elementName,
                attributes = mappedAttrs,
                containsPcData = element.children.any { it.trim() == "#PCDATA" },
                children = element.children
                    .filterNot { it.trim() == "#PCDATA" }
                    .map { childName ->
                        val childName = childName.trim()
                        val element = elements.firstOrNull { it.name == childName }
                        requireNotNull(element) { "Couldn't find a child with the name $childName in $elements" }
                        buildElementDefinition(element, childName, elements, attributes)
                    }
            )
        }
        element.children.isNotEmpty() -> {
            if (element.children.size == 1 && element.children.first() == "#PCDATA") {
                ElementDefinition.ParsedCharacterData(
                    elementName = elementName,
                    attributes = mappedAttrs,
                )
            } else if (element.children.size == 1 && element.children.first() == "ANY") {
                ElementDefinition.Any(
                    elementName = elementName,
                    attributes = mappedAttrs,
                )
            } else {
                ElementDefinition.WithChildren(
                    elementName = elementName,
                    attributes = mappedAttrs,
                    children = element.children
                        .map { childName ->
                            val childName = childName.trim()
                            buildChildElementDefinition(childName, elements, attributes)
                        }
                )
            }
        }
        else -> {
            ElementDefinition.Empty(
                elementName = elementName,
                attributes = mappedAttrs,
            )
        }
    }
}

internal fun buildAttribute(attribute: AttributeDto): AttributeDefinition {
    val type = when {
        attribute.type == "CDATA" -> AttributeDefinition.Type.CharacterData
        attribute.type == "ID" -> AttributeDefinition.Type.Id
        attribute.type == "IDREF" -> AttributeDefinition.Type.IdRef
        attribute.type == "IDREFS" -> AttributeDefinition.Type.IdRefs
        attribute.type == "NMTOKEN" -> AttributeDefinition.Type.NmToken
        attribute.type == "NMTOKENS" -> AttributeDefinition.Type.NmTokens
        attribute.type == "ENTITY" -> AttributeDefinition.Type.Entity
        attribute.type == "ENTITIES" -> AttributeDefinition.Type.Entities
        attribute.type == "NOTATION" -> AttributeDefinition.Type.Notation
        attribute.type == "xml:" -> AttributeDefinition.Type.Xml
        attribute.type.startsWith("(") && attribute.type.endsWith(")") -> {
            val values = attribute.type
                .removeSurrounding("(", ")")
                .split("|")
                .map { it.trim() }
            AttributeDefinition.Type.Enum(values)
        }
        else -> throw IllegalArgumentException("Unrecognized attribute type! ${attribute.type}")
    }
    val value = when {
        attribute.value == "#REQUIRED" -> AttributeDefinition.Value.Required
        attribute.value == "#IMPLIED" -> AttributeDefinition.Value.Implied
        attribute.value.startsWith("#FIXED") -> {
            val value = attribute.value.removeSurrounding("#FIXED \"", "\"")
            AttributeDefinition.Value.Fixed(value)
        }
        attribute.value.startsWith("\"") && attribute.value.endsWith("\"") ->
            AttributeDefinition.Value.Default(attribute.value.removeSurrounding("\"", "\""))
        else -> throw IllegalArgumentException("Unrecognized attribute value! ${attribute.value}")
    }
    return AttributeDefinition(
        attributeName = attribute.attributeName,
        attributeType = type,
        value = value
    )
}

internal val DoctypeRegex = Regex("<!DOCTYPE\\s+([a-zA-Z0-9_]+)\\s*\\[")

internal val ElementRegex = Regex("<!ELEMENT\\s+([a-zA-Z0-9_]+)\\s+(\\(.+\\)\\*?|EMPTY|ANY)\\s*>")

internal val InternalEntityMatcher = Regex("<!ENTITY\\s+([a-zA-Z0-9_-]+)\\s+\"(.+)\"\\s*>")

internal val ExternalEntityMatcher = Regex("<!ENTITY\\s+([a-zA-Z0-9_-]+)\\s+SYSTEM\\s+\"(.+)\"\\s*>")

internal val AttributeListRegex = Regex("<!ATTLIST\\s+([a-zA-Z0-9_-]+)\\s+([a-zA-Z0-9:_-]+)\\s+([a-zA-Z0-9_-]+)\\s+([\"#a-zA-Z0-9_-]+)\\s*>")

internal data class ElementDto(
    val name: String,
    val children: List<String>,
    val isMixed: Boolean
)

internal class AttributeDto(
    val elementName: String,
    val attributeName: String,
    val type: String,
    val value: String
)

internal data class InternalEntityDto(
    val name: String,
    val value: String
)

internal data class ExternalEntityDto(
    val name: String,
    val uri: String,
)
