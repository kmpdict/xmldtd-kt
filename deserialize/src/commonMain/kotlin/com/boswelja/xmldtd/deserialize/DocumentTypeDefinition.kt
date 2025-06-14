package com.boswelja.xmldtd.deserialize

/**
 * Defines the structure and the legal elements and attributes of an XML document.
 */
public data class DocumentTypeDefinition(
    val rootElement: ElementDefinition,
    val entities: List<Entity>
) {
    public companion object
}

/**
 * Describes a child element that can occur within its parent [ElementDefinition].
 */
public sealed interface ChildElementDefinition {
    /**
     * The number of times any given [ChildElementDefinition] can occur within its parent.
     */
    public val occurs: Occurs

    /**
     * Describes a child element that can occur exactly once.
     */
    public data class Single(
        override val occurs: Occurs,
        /**
         * The definition of the child element.
         */
        val elementDefinition: ElementDefinition,
    ) : ChildElementDefinition

    /**
     * Describes a child element that will match exactly one of the specified options.
     */
    public data class Either(
        override val occurs: Occurs,
        /**
         * A list of child element options that can occur.
         */
        val options: List<ChildElementDefinition>,
    ) : ChildElementDefinition

    /**
     * The number of times any given [ChildElementDefinition] can occur within its parent.
     */
    public enum class Occurs {
        /**
         * The element occurs exactly once.
         */
        Once,

        /**
         * The element occurs at least once.
         */
        AtLeastOnce,

        /**
         * The element occurs zero or more times.
         */
        ZeroOrMore,

        /**
         * The element occurs at most once.
         */
        AtMostOnce,
    }
}

public sealed interface ElementDefinition {
    /**
     * The name of the element. For example, in `!ELEMENT note` the element is named "note".
     */
    public val elementName: String

    /**
     * A list of attributes that can occur on this element. An empty list means no attributes are
     * allowed.
     */
    public val attributes: List<AttributeDefinition>

    public val comment: String?

    /**
     * The element has no children. For example, in `!ELEMENT note EMPTY` there are no elements contained within `note`.
     */
    public data class Empty(
        override val elementName: String,
        override val attributes: List<AttributeDefinition>,
        override val comment: String?,
    ) : ElementDefinition

    /**
     * The element contains parsed character data (PCDATA). For example, in `!ELEMENT note (#PCDATA)`
     * there is no child element within `note`, but it can contain text data.
     */
    public data class ParsedCharacterData(
        override val elementName: String,
        override val attributes: List<AttributeDefinition>,
        override val comment: String?,
    ) : ElementDefinition

    /**
     * The element contains arbitrary data.
     */
    public data class Any(
        override val elementName: String,
        override val attributes: List<AttributeDefinition>,
        override val comment: String?,
    ) : ElementDefinition

    /**
     * The element has a sequence of children. For example, in `!ELEMENT note (title, description)`
     * there is a title and a description within `note`. Children under this element are ordered,
     * such that in the above example `title` will appear before `description`.
     */
    public data class WithChildren(
        override val elementName: String,
        override val attributes: List<AttributeDefinition>,
        override val comment: String?,
        val children: List<ChildElementDefinition>,
    ) : ElementDefinition

    /**
     * The element has zero or more occurrences of its children, in any order.
     */
    public data class Mixed(
        override val elementName: String,
        override val attributes: List<AttributeDefinition>,
        override val comment: String?,
        val containsPcData: Boolean,
        val children: List<ElementDefinition>,
    ) : ElementDefinition

    /**
     * The element may contain an occurrence of [options], but never multiple options at the same
     * time.
     */
    public data class Either(
        override val elementName: String,
        override val attributes: List<AttributeDefinition>,
        override val comment: String?,
        val options: List<ChildElementDefinition>
    ): ElementDefinition
}

/**
 * An attribute definition specifies how an attribute can be declared within an element.
 *
 * @property attributeName The name of the attribute.
 * @property attributeType The data type of the attribute. See [Type] for all possible types.
 * @property value Specifies whether the attribute is required, implied, has a default value, or
 * must have a fixed value. See [Value] for details.
 */
public data class AttributeDefinition(
    val attributeName: String,
    val attributeType: Type,
    val value: Value,
    val comment: String?,
) {
    public sealed interface Type {
        /**
         * A character data type represents textual content within an element.
         */
        public data object CharacterData: Type

        /**
         * The value must be one of the values in the given list of options.
         */
        public data class Enum(val options: List<String>): Type

        /**
         * The value is a unique ID.
         */
        public data object Id: Type

        /**
         * The value is the ID of another element.
         */
        public data object IdRef: Type

        /**
         * The value is a list of IDs for other elements.
         */
        public data object IdRefs: Type

        /**
         * The value is a valid XML name.
         */
        public data object NmToken: Type

        /**
         * The value is a list of valid XML names.
         */
        public data object NmTokens: Type

        /**
         * The value is an entity.
         */
        public data object Entity: Type

        /**
         * The value is a list of entities.
         */
        public data object Entities: Type

        /**
         * The value is a name of a notation.
         */
        public data object Notation: Type

        /**
         * The value is a predefined XML value.
         */
        public data object Xml: Type
    }

    /**
     * Attribute values can be fixed, default, required or implied.
     */
    public sealed interface Value {
        /**
         * A fixed attribute must always have this specific value.
         */
        public data class Fixed(val value: String): Value

        /**
         * A default attribute has this specific value if it is not specified.
         */
        public data class Default(val value: String): Value

        /**
         * A required attribute must always be specified. There is no default value.
         */
        public data object Required: Value

        /**
         * An implied attribute has no default value and is not required. If it is not specified,
         * the attribute is considered absent.
         */
        public data object Implied: Value
    }
}

/**
 * Entities are used to define shortcuts to special characters. Entities can be [Internal] or [External].
 */
public sealed interface Entity {
    /**
     * Internal entities represent direct name-value replacements inside the document.
     */
    public data class Internal(
        val name: String,
        val value: String,
    ): Entity

    /**
     * External entities represent external references to a file containing the entity definition.
     */
    public data class External(
        val name: String,
        val url: String,
    ): Entity
}
