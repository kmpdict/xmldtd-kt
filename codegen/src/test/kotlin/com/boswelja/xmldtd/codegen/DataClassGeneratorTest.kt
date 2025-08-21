package com.boswelja.xmldtd.codegen

import com.boswelja.xmldtd.deserialize.AttributeDefinition
import com.boswelja.xmldtd.deserialize.ChildElementDefinition
import com.boswelja.xmldtd.deserialize.DocumentTypeDefinition
import com.boswelja.xmldtd.deserialize.ElementDefinition
import com.boswelja.xmldtd.deserialize.Entity
import com.diffplug.selfie.Selfie.expectSelfie
import kotlin.io.path.Path
import kotlin.test.BeforeTest
import kotlin.test.Test

class DataClassGeneratorTest {

    private val testDir = Path("testdata/")
    private val packageName = "com.example.test"

    private lateinit var generator: DataClassGenerator

    @BeforeTest
    fun setUp() {
        generator = DataClassGenerator(packageName, testDir)
    }

    @Test
    fun `generates prefixes`() {
        val input = DocumentTypeDefinition(
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
        )

        val target = StringBuilder()
        generator.buildFileSpec(input).writeTo(target)

        expectSelfie(target.toString()).toMatchDisk()
    }

    @Test
    fun `generates comments`() {
        val input = DocumentTypeDefinition(
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
        )

        val target = StringBuilder()
        generator.buildFileSpec(input).writeTo(target)

        expectSelfie(target.toString()).toMatchDisk()
    }

    @Test
    fun `generates with no nested elements`() {
        val input = DocumentTypeDefinition(
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
        )

        val target = StringBuilder()
        generator.buildFileSpec(input).writeTo(target)

        expectSelfie(target.toString()).toMatchDisk()
    }

    @Test
    fun `generates nested elements`() {
        val input = DocumentTypeDefinition(
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
            entities = emptyList()
        )

        val target = StringBuilder()
        generator.buildFileSpec(input).writeTo(target)

        expectSelfie(target.toString()).toMatchDisk()
    }

    @Test
    fun `generates pcdata parser`() {
        val input = DocumentTypeDefinition(
            rootElement = ElementDefinition.ParsedCharacterData(
                elementName = "parseable_data",
                attributes = emptyList(),
                comment = null
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
        )

        val target = StringBuilder()
        generator.buildFileSpec(input).writeTo(target)

        expectSelfie(target.toString()).toMatchDisk()
    }

    @Test
    fun `generates either elements`() {
        val input = DocumentTypeDefinition(
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
        )

        val target = StringBuilder()
        generator.buildFileSpec(input).writeTo(target)

        expectSelfie(target.toString()).toMatchDisk()
    }
}
