package com.boswelja.xmldtd.codegen

import com.boswelja.xmldtd.deserialize.AttributeDefinition
import com.boswelja.xmldtd.deserialize.ChildElementDefinition
import com.boswelja.xmldtd.deserialize.DocumentTypeDefinition
import com.boswelja.xmldtd.deserialize.ElementDefinition
import com.boswelja.xmldtd.deserialize.Entity
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.readText
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DataClassGeneratorTest {

    private val testDir = Path("testdata/")
    private val packageName = "com.example.test"

    private lateinit var generator: DataClassGenerator

    @BeforeTest
    fun setUp() {
        testDir.createDirectories()
        generator = DataClassGenerator(packageName, testDir)
    }

    @OptIn(ExperimentalPathApi::class)
    @AfterTest
    fun tearDown() {
        testDir.deleteRecursively()
    }

    @Test
    fun `writeDtdToTarget should generate prefixes`() {
        val testCases = mapOf(
            DocumentTypeDefinition(
                rootElement = ElementDefinition.ParsedCharacterData(
                    elementName = "prefixes",
                    attributes = listOf(
                        AttributeDefinition(
                            attributeName = "xml:name",
                            attributeType = AttributeDefinition.Type.CharacterData,
                            value = AttributeDefinition.Value.Required,
                            comment = null,
                        )
                    ),
                    comment = null,
                ),
                entities = emptyList()
            ) to """
                |package $packageName
                |
                |import kotlin.String
                |import kotlinx.serialization.SerialName
                |import kotlinx.serialization.Serializable
                |import nl.adaptivity.xmlutil.serialization.XmlElement
                |import nl.adaptivity.xmlutil.serialization.XmlSerialName
                |import nl.adaptivity.xmlutil.serialization.XmlValue
                |
                |@Serializable
                |@XmlElement(value = true)
                |@SerialName(value = "prefixes")
                |public data class Prefixes(
                |  @XmlElement(value = false)
                |  @SerialName(value = "xml:name")
                |  @XmlSerialName(
                |    prefix = "xml",
                |    value = "name",
                |  )
                |  public val name: String,
                |  @XmlValue
                |  public val content: String,
                |)
                |
            """.trimMargin(),
        )

        testCases.forEach { (input, expected) ->
            generator.writeDtdToTarget(input)

            assertEquals(
                expected,
                testDir.resolve("com/example/test/Prefixes.kt").readText()
            )
        }
    }

    @Test
    fun `writeDtdToTarget should generate comments`() {
        val testCases = mapOf(
            DocumentTypeDefinition(
                rootElement = ElementDefinition.ParsedCharacterData(
                    elementName = "comments",
                    attributes = listOf(
                        AttributeDefinition(
                            attributeName = "attr1",
                            attributeType = AttributeDefinition.Type.CharacterData,
                            value = AttributeDefinition.Value.Required,
                            comment = "This is an attribute with a comment.",
                        )
                    ),
                    comment = "This is an element with a comment.",
                ),
                entities = emptyList()
            ) to """
                |package $packageName
                |
                |import kotlin.String
                |import kotlinx.serialization.SerialName
                |import kotlinx.serialization.Serializable
                |import nl.adaptivity.xmlutil.serialization.XmlElement
                |import nl.adaptivity.xmlutil.serialization.XmlValue
                |
                |/**
                | * This is an element with a comment.
                | */
                |@Serializable
                |@XmlElement(value = true)
                |@SerialName(value = "comments")
                |public data class Comments(
                |  /**
                |   * This is an attribute with a comment.
                |   */
                |  @XmlElement(value = false)
                |  @SerialName(value = "attr1")
                |  public val attr1: String,
                |  @XmlValue
                |  public val content: String,
                |)
                |
            """.trimMargin(),
        )

        testCases.forEach { (input, expected) ->
            generator.writeDtdToTarget(input)

            assertEquals(
                expected,
                testDir.resolve("com/example/test/Comments.kt").readText()
            )
        }
    }

    @Test
    fun `writeDtdToTarget should generate with no nested elements`() {
        val testCases = mapOf(
            DocumentTypeDefinition(
                rootElement = ElementDefinition.ParsedCharacterData(
                    elementName = "no-nested-data",
                    attributes = emptyList(),
                    comment = null,
                ),
                entities = emptyList()
            ) to """
                |package $packageName
                |
                |import kotlin.String
                |import kotlinx.serialization.SerialName
                |import kotlinx.serialization.Serializable
                |import nl.adaptivity.xmlutil.serialization.XmlElement
                |import nl.adaptivity.xmlutil.serialization.XmlValue
                |
                |@Serializable
                |@XmlElement(value = true)
                |@SerialName(value = "no-nested-data")
                |public data class NoNestedData(
                |  @XmlValue
                |  public val content: String,
                |)
                |
            """.trimMargin(),
            DocumentTypeDefinition(
                rootElement = ElementDefinition.Empty(
                    elementName = "no-nested-data",
                    attributes = emptyList(),
                    comment = null,
                ),
                entities = emptyList()
            ) to """
                |package $packageName
                |
                |import kotlinx.serialization.SerialName
                |import kotlinx.serialization.Serializable
                |import nl.adaptivity.xmlutil.serialization.XmlElement
                |
                |@Serializable
                |@XmlElement(value = true)
                |@SerialName(value = "no-nested-data")
                |public data object NoNestedData
                |
            """.trimMargin(),
            DocumentTypeDefinition(
                rootElement = ElementDefinition.Any(
                    elementName = "no-nested-data",
                    attributes = emptyList(),
                    comment = null,
                ),
                entities = emptyList()
            ) to """
                |package $packageName
                |
                |import kotlin.String
                |import kotlinx.serialization.SerialName
                |import kotlinx.serialization.Serializable
                |import nl.adaptivity.xmlutil.serialization.XmlElement
                |import nl.adaptivity.xmlutil.serialization.XmlValue
                |
                |@Serializable
                |@XmlElement(value = true)
                |@SerialName(value = "no-nested-data")
                |public data class NoNestedData(
                |  @XmlValue
                |  public val content: String,
                |)
                |
            """.trimMargin(),
            DocumentTypeDefinition(
                rootElement = ElementDefinition.ParsedCharacterData(
                    elementName = "no-nested-data",
                    attributes = listOf(
                        AttributeDefinition(
                            attributeName = "attr1",
                            attributeType = AttributeDefinition.Type.CharacterData,
                            value = AttributeDefinition.Value.Required,
                            comment = null,
                        )
                    ),
                    comment = null,
                ),
                entities = emptyList()
            ) to """
                |package $packageName
                |
                |import kotlin.String
                |import kotlinx.serialization.SerialName
                |import kotlinx.serialization.Serializable
                |import nl.adaptivity.xmlutil.serialization.XmlElement
                |import nl.adaptivity.xmlutil.serialization.XmlValue
                |
                |@Serializable
                |@XmlElement(value = true)
                |@SerialName(value = "no-nested-data")
                |public data class NoNestedData(
                |  @XmlElement(value = false)
                |  @SerialName(value = "attr1")
                |  public val attr1: String,
                |  @XmlValue
                |  public val content: String,
                |)
                |
            """.trimMargin(),
        )

        testCases.forEach { (input, expected) ->
            generator.writeDtdToTarget(input)

            assertEquals(
                expected,
                testDir.resolve("com/example/test/NoNestedData.kt").readText()
            )
        }
    }

    @Test
    fun `writeDtdToTarget should generate nested elements`() {
        val testCases = mapOf(
            DocumentTypeDefinition(
                rootElement = ElementDefinition.WithChildren(
                    elementName = "NESTED",
                    attributes = emptyList(),
                    children = listOf(
                        ChildElementDefinition.Single(
                            elementDefinition = ElementDefinition.WithChildren(
                                elementName = "ARTICLE",
                                attributes = listOf(
                                    AttributeDefinition(
                                        attributeName = "AUTHOR",
                                        attributeType = AttributeDefinition.Type.CharacterData,
                                        value = AttributeDefinition.Value.Required,
                                        comment = null,
                                    ),
                                    AttributeDefinition(
                                        attributeName = "EDITOR",
                                        attributeType = AttributeDefinition.Type.CharacterData,
                                        value = AttributeDefinition.Value.Implied,
                                        comment = null,
                                    ),
                                    AttributeDefinition(
                                        attributeName = "DATE",
                                        attributeType = AttributeDefinition.Type.CharacterData,
                                        value = AttributeDefinition.Value.Implied,
                                        comment = null,
                                    ),
                                    AttributeDefinition(
                                        attributeName = "EDITION",
                                        attributeType = AttributeDefinition.Type.CharacterData,
                                        value = AttributeDefinition.Value.Implied,
                                        comment = null,
                                    ),
                                ),
                                children = listOf(
                                    ChildElementDefinition.Single(
                                        elementDefinition = ElementDefinition.ParsedCharacterData(
                                            elementName = "HEADLINE",
                                            attributes = emptyList(),
                                            comment = null,
                                        ),
                                        occurs = ChildElementDefinition.Occurs.Once
                                    ),
                                    ChildElementDefinition.Single(
                                        elementDefinition = ElementDefinition.ParsedCharacterData(
                                            elementName = "BYLINE",
                                            attributes = emptyList(),
                                            comment = null,
                                        ),
                                        occurs = ChildElementDefinition.Occurs.Once
                                    ),
                                    ChildElementDefinition.Single(
                                        elementDefinition = ElementDefinition.ParsedCharacterData(
                                            elementName = "LEAD",
                                            attributes = emptyList(),
                                            comment = null,
                                        ),
                                        occurs = ChildElementDefinition.Occurs.Once
                                    ),
                                    ChildElementDefinition.Single(
                                        elementDefinition = ElementDefinition.ParsedCharacterData(
                                            elementName = "BODY",
                                            attributes = emptyList(),
                                            comment = null,
                                        ),
                                        occurs = ChildElementDefinition.Occurs.Once
                                    ),
                                    ChildElementDefinition.Single(
                                        elementDefinition = ElementDefinition.ParsedCharacterData(
                                            elementName = "NOTES",
                                            attributes = emptyList(),
                                            comment = null,
                                        ),
                                        occurs = ChildElementDefinition.Occurs.Once
                                    ),
                                ),
                                comment = null,
                            ),
                            occurs = ChildElementDefinition.Occurs.AtLeastOnce
                        )
                    ),
                    comment = null,
                ),
                entities = listOf(
                    Entity.Internal(
                        name = "NEWSPAPER",
                        value = "Vervet Logic Times"
                    ),
                    Entity.Internal(
                        name = "PUBLISHER",
                        value = "Vervet Logic Press"
                    ),
                    Entity.Internal(
                        name = "COPYRIGHT",
                        value = "Copyright 1998 Vervet Logic Press"
                    )
                )
            ) to """
                |package $packageName
                |
                |import kotlin.String
                |import kotlin.collections.List
                |import kotlin.collections.Map
                |import kotlinx.serialization.SerialName
                |import kotlinx.serialization.Serializable
                |import nl.adaptivity.xmlutil.serialization.XmlElement
                |import nl.adaptivity.xmlutil.serialization.XmlValue
                |
                |@Serializable
                |@XmlElement(value = true)
                |@SerialName(value = "HEADLINE")
                |public data class Headline(
                |  @XmlValue
                |  public val content: String,
                |)
                |
                |@Serializable
                |@XmlElement(value = true)
                |@SerialName(value = "BYLINE")
                |public data class Byline(
                |  @XmlValue
                |  public val content: String,
                |)
                |
                |@Serializable
                |@XmlElement(value = true)
                |@SerialName(value = "LEAD")
                |public data class Lead(
                |  @XmlValue
                |  public val content: String,
                |)
                |
                |@Serializable
                |@XmlElement(value = true)
                |@SerialName(value = "BODY")
                |public data class Body(
                |  @XmlValue
                |  public val content: String,
                |)
                |
                |@Serializable
                |@XmlElement(value = true)
                |@SerialName(value = "NOTES")
                |public data class Notes(
                |  @XmlValue
                |  public val content: String,
                |)
                |
                |@Serializable
                |@XmlElement(value = true)
                |@SerialName(value = "ARTICLE")
                |public data class Article(
                |  @XmlElement(value = false)
                |  @SerialName(value = "AUTHOR")
                |  public val author: String,
                |  @XmlElement(value = false)
                |  @SerialName(value = "EDITOR")
                |  public val editor: String?,
                |  @XmlElement(value = false)
                |  @SerialName(value = "DATE")
                |  public val date: String?,
                |  @XmlElement(value = false)
                |  @SerialName(value = "EDITION")
                |  public val edition: String?,
                |  @XmlElement(value = true)
                |  @SerialName(value = "HEADLINE")
                |  public val headline: Headline,
                |  @XmlElement(value = true)
                |  @SerialName(value = "BYLINE")
                |  public val byline: Byline,
                |  @XmlElement(value = true)
                |  @SerialName(value = "LEAD")
                |  public val lead: Lead,
                |  @XmlElement(value = true)
                |  @SerialName(value = "BODY")
                |  public val body: Body,
                |  @XmlElement(value = true)
                |  @SerialName(value = "NOTES")
                |  public val notes: Notes,
                |)
                |
                |@Serializable
                |@XmlElement(value = true)
                |@SerialName(value = "NESTED")
                |public data class Nested(
                |  @XmlElement(value = true)
                |  @SerialName(value = "ARTICLE")
                |  public val articles: List<Article>,
                |)
                |
                |public val NestedInternalEntities: Map<String, String> = mapOf(
                |  "NEWSPAPER" to "Vervet Logic Times",
                |  "PUBLISHER" to "Vervet Logic Press",
                |  "COPYRIGHT" to "Copyright 1998 Vervet Logic Press",
                |)
                |
            """.trimMargin()
        )

        testCases.forEach { (input, expected) ->
            generator.writeDtdToTarget(input)
            println(input)

            assertEquals(
                expected,
                testDir.resolve("com/example/test/Nested.kt").readText()
            )
        }
    }

    @Test
    fun `writeDtdToTarget should generate either elements`() {
        val testCases = mapOf(
            DocumentTypeDefinition(
                rootElement = ElementDefinition.Either(
                    elementName = "date_or_time",
                    attributes = emptyList(),
                    comment = "Holds either a date or a time, but never both.",
                    options = listOf(
                        ChildElementDefinition.Single(
                            occurs = ChildElementDefinition.Occurs.Once,
                            elementDefinition = ElementDefinition.ParsedCharacterData(
                                elementName = "date",
                                attributes = emptyList(),
                                comment = "An ISO8601 date."
                            )
                        ),
                        ChildElementDefinition.Single(
                            occurs = ChildElementDefinition.Occurs.Once,
                            elementDefinition = ElementDefinition.ParsedCharacterData(
                                elementName = "time",
                                attributes = emptyList(),
                                comment = "An ISO8601 time."
                            )
                        ),
                    )
                ),
                entities = emptyList()
            ) to """
                |package $packageName
                |
                |import kotlin.String
                |import kotlinx.serialization.SerialName
                |import kotlinx.serialization.Serializable
                |import nl.adaptivity.xmlutil.serialization.XmlElement
                |import nl.adaptivity.xmlutil.serialization.XmlValue
                |
                |/**
                | * Holds either a date or a time, but never both.
                | */
                |@Serializable
                |@XmlElement(value = true)
                |@SerialName(value = "date_or_time")
                |public sealed interface DateOrTime {
                |  /**
                |   * An ISO8601 date.
                |   */
                |  @Serializable
                |  @XmlElement(value = true)
                |  @SerialName(value = "date")
                |  public data class Date(
                |    @XmlValue
                |    public val content: String,
                |  ) : DateOrTime
                |
                |  /**
                |   * An ISO8601 time.
                |   */
                |  @Serializable
                |  @XmlElement(value = true)
                |  @SerialName(value = "time")
                |  public data class Time(
                |    @XmlValue
                |    public val content: String,
                |  ) : DateOrTime
                |}
                |
            """.trimMargin()
        )

        testCases.forEach { (input, expected) ->
            generator.writeDtdToTarget(input)
            println(input)

            assertEquals(
                expected,
                testDir.resolve("com/example/test/DateOrTime.kt").readText()
            )
        }
    }
}
