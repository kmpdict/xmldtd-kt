package com.boswelja.xmldtd.deserialize

import kotlinx.io.Source
import kotlinx.io.readLine

public fun DocumentTypeDefinition.Companion.fromSource(source: Source): DocumentTypeDefinition {
    // Find the start of the doctype (there might be xml tags and comments before the actual start)
    var line: String? = source.readLinesUntil { it.matches(DoctypeRegex) }
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
        ElementDto.fromLine(line)?.let {
            elements.add(it.copy(comment = extractCommentFromNextLines(source)))
            line = source.readLine()?.trim()
            continue
        }
        InternalEntityDto.fromLine(line)?.let {
            internalEntities.add(it.copy(comment = extractCommentFromNextLines(source)))
            line = source.readLine()?.trim()
            continue
        }
        ExternalEntityDto.fromLine(line)?.let {
            externalEntities.add(it.copy(comment = extractCommentFromNextLines(source)))
            line = source.readLine()?.trim()
            continue
        }
        AttributeDto.fromLine(line)?.let {
            attributes.add(it.copy(comment = extractCommentFromNextLines(source)))
            line = source.readLine()?.trim()
            continue
        }
        AttributeDto.fromLines(line, source)?.let {
            attributes.addAll(it)
            line = source.readLine()?.trim()
            continue
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
                    },
                comment = element.comment,
            )
        }
        element.children.isNotEmpty() -> {
            when (element.children.size) {
                1 if element.children.first() == "#PCDATA" -> {
                    ElementDefinition.ParsedCharacterData(
                        elementName = elementName,
                        attributes = mappedAttrs,
                        comment = element.comment,
                    )
                }
                1 if element.children.first() == "ANY" -> {
                    ElementDefinition.Any(
                        elementName = elementName,
                        attributes = mappedAttrs,
                        comment = element.comment,
                    )
                }
                else -> {
                    ElementDefinition.WithChildren(
                        elementName = elementName,
                        attributes = mappedAttrs,
                        children = element.children
                            .map { childName ->
                                val childName = childName.trim()
                                buildChildElementDefinition(childName, elements, attributes)
                            },
                        comment = element.comment,
                    )
                }
            }
        }
        else -> {
            ElementDefinition.Empty(
                elementName = elementName,
                attributes = mappedAttrs,
                comment = element.comment,
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
        value = value,
        comment = attribute.comment
    )
}

internal val DoctypeRegex = Regex("<!DOCTYPE\\s+([a-zA-Z0-9_]+)\\s*\\[")
