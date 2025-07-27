package com.boswelja.xmldtd.deserialize

import kotlinx.io.Buffer
import kotlinx.io.writeString
import kotlin.test.Test
import kotlin.test.assertEquals

class DeserializerE2ETest {

    @Test
    fun `when dtd has multiline attlist, then deserialize succeeds`() {
        val dtd = """
            <!DOCTYPE NAME [
            <!ELEMENT NAME (#PCDATA)>
            <!ATTLIST NAME
            gender CDATA #IMPLIED
            lang CDATA "eng">
            ]>
        """.trimIndent()
        val expected = DocumentTypeDefinition(
            rootElement = ElementDefinition.ParsedCharacterData(
                elementName = "NAME",
                attributes = listOf(
                    AttributeDefinition(
                        attributeName = "gender",
                        attributeType = AttributeDefinition.Type.CharacterData,
                        value = AttributeDefinition.Value.Implied,
                        comment = null,
                    ),
                    AttributeDefinition(
                        attributeName = "lang",
                        attributeType = AttributeDefinition.Type.CharacterData,
                        value = AttributeDefinition.Value.Default("eng"),
                        comment = null,
                    ),
                ),
                comment = null,
            ),
            entities = emptyList(),
        )

        val source = Buffer()
        source.writeString(dtd)
        val result = DocumentTypeDefinition.fromSource(source)
        source.close()

        assertEquals(
            expected,
            result
        )
    }

    @Test
    fun `when dtd attlist children have colons, then deserialize succeeds`() {
        val dtd = """
            <!DOCTYPE NAME [
            <!ELEMENT NAME (#PCDATA)>
            <!ATTLIST NAME xml:lang CDATA "eng">
            ]>
        """.trimIndent()
        val expected = DocumentTypeDefinition(
            rootElement = ElementDefinition.ParsedCharacterData(
                elementName = "NAME",
                attributes = listOf(
                    AttributeDefinition(
                        attributeName = "xml:lang",
                        attributeType = AttributeDefinition.Type.CharacterData,
                        value = AttributeDefinition.Value.Default("eng"),
                        comment = null,
                    )
                ),
                comment = null,
            ),
            entities = emptyList(),
        )

        val source = Buffer()
        source.writeString(dtd)
        val result = DocumentTypeDefinition.fromSource(source)
        source.close()

        assertEquals(
            expected,
            result
        )
    }

    @Test
    fun `when dtd element children contains spaces, then deserialize succeeds`() {
        // Note the space between DATE and NAME in HOLIDAY
        val dtd = """
            <!DOCTYPE HOLIDAY [
            <!ELEMENT HOLIDAY (DATE, NAME)>
            <!ELEMENT DATE (#PCDATA)>
            <!ELEMENT NAME (#PCDATA)>
            ]>
        """.trimIndent()
        val expected = DocumentTypeDefinition(
            rootElement = ElementDefinition.WithChildren(
                elementName = "HOLIDAY",
                children = listOf(
                    ChildElementDefinition.Single(
                        elementDefinition = ElementDefinition.ParsedCharacterData(
                            elementName = "DATE",
                            attributes = emptyList(),
                            comment = null,
                        ),
                        occurs = ChildElementDefinition.Occurs.Once,
                    ),
                    ChildElementDefinition.Single(
                        elementDefinition = ElementDefinition.ParsedCharacterData(
                            elementName = "NAME",
                            attributes = emptyList(),
                            comment = null,
                        ),
                        occurs = ChildElementDefinition.Occurs.Once,
                    ),
                ),
                attributes = emptyList(),
                comment = null,
            ),
            entities = emptyList(),
        )

        val source = Buffer()
        source.writeString(dtd)
        val result = DocumentTypeDefinition.fromSource(source)
        source.close()

        assertEquals(
            expected,
            result
        )
    }

    @Test
    fun `when dtd contains documentations, then deserialize succeeds`() {
        // Note the space between DATE and NAME in HOLIDAY
        val dtd = """
            <!DOCTYPE HOLIDAY [
            <!ELEMENT HOLIDAY (DATE, NAME)>
            <!-- A holiday on some date -->
            <!ELEMENT DATE (#PCDATA)>
            <!--
              The date that a holiday occurs on, in ISO8601 datetime format. -->
            <!ELEMENT NAME (#PCDATA)>
            <!-- The name of a holiday.
              -->
            ]>
        """.trimIndent()
        val expected = DocumentTypeDefinition(
            rootElement = ElementDefinition.WithChildren(
                elementName = "HOLIDAY",
                children = listOf(
                    ChildElementDefinition.Single(
                        elementDefinition = ElementDefinition.ParsedCharacterData(
                            elementName = "DATE",
                            attributes = emptyList(),
                            comment = "The date that a holiday occurs on, in ISO8601 datetime format.",
                        ),
                        occurs = ChildElementDefinition.Occurs.Once,
                    ),
                    ChildElementDefinition.Single(
                        elementDefinition = ElementDefinition.ParsedCharacterData(
                            elementName = "NAME",
                            attributes = emptyList(),
                            comment = "The name of a holiday.",
                        ),
                        occurs = ChildElementDefinition.Occurs.Once,
                    ),
                ),
                attributes = emptyList(),
                comment = "A holiday on some date",
            ),
            entities = emptyList(),
        )

        val source = Buffer()
        source.writeString(dtd)
        val result = DocumentTypeDefinition.fromSource(source)
        source.close()

        assertEquals(
            expected,
            result
        )
    }
    @Test
    fun `fromSource returns the correct type for valid DTD`() {
        ValidDtdSamples.forEach { (dtd, expected) ->
            val source = Buffer()
            source.writeString(dtd)

            val result = DocumentTypeDefinition.fromSource(source)

            source.close()

            assertEquals(
                expected,
                result
            )
        }
    }

    companion object {
        val ValidDtdSamples = mapOf(
            """
                <!DOCTYPE TVSCHEDULE [

                <!ELEMENT TVSCHEDULE (CHANNEL+)>
                <!ELEMENT CHANNEL (BANNER,DAY+)>
                <!ELEMENT BANNER (#PCDATA)>
                <!ELEMENT DAY (DATE,(HOLIDAY|PROGRAMSLOT+)+)>
                <!ELEMENT HOLIDAY (#PCDATA)>
                <!ELEMENT DATE (#PCDATA)>
                <!ELEMENT PROGRAMSLOT (TIME,TITLE,DESCRIPTION?)>
                <!ELEMENT TIME (#PCDATA)>
                <!ELEMENT TITLE (#PCDATA)> 
                <!ELEMENT DESCRIPTION (#PCDATA)>

                <!ATTLIST TVSCHEDULE NAME CDATA #REQUIRED>
                <!ATTLIST CHANNEL CHAN CDATA #REQUIRED>
                <!ATTLIST PROGRAMSLOT VTR CDATA #IMPLIED>
                <!ATTLIST TITLE RATING CDATA #IMPLIED>
                <!ATTLIST TITLE LANGUAGE CDATA #IMPLIED>
                ]>
            """.trimIndent() to
                    DocumentTypeDefinition(
                        rootElement = ElementDefinition.WithChildren(
                            elementName = "TVSCHEDULE",
                            attributes = listOf(
                                AttributeDefinition(
                                    attributeName = "NAME",
                                    attributeType = AttributeDefinition.Type.CharacterData,
                                    value = AttributeDefinition.Value.Required,
                                    comment = null,
                                )
                            ),
                            children = listOf(
                                ChildElementDefinition.Single(
                                    elementDefinition = ElementDefinition.WithChildren(
                                        elementName = "CHANNEL",
                                        attributes = listOf(
                                            AttributeDefinition(
                                                attributeName = "CHAN",
                                                attributeType = AttributeDefinition.Type.CharacterData,
                                                value = AttributeDefinition.Value.Required,
                                                comment = null,
                                            )
                                        ),
                                        children = listOf(
                                            ChildElementDefinition.Single(
                                                elementDefinition = ElementDefinition.ParsedCharacterData(
                                                    elementName = "BANNER",
                                                    attributes = emptyList(),
                                                    comment = null,
                                                ),
                                                occurs = ChildElementDefinition.Occurs.Once
                                            ),
                                            ChildElementDefinition.Single(
                                                elementDefinition = ElementDefinition.WithChildren(
                                                    elementName = "DAY",
                                                    attributes = emptyList(),
                                                    children = listOf(
                                                        ChildElementDefinition.Single(
                                                            elementDefinition = ElementDefinition.ParsedCharacterData(
                                                                elementName = "DATE",
                                                                attributes = emptyList(),
                                                                comment = null,
                                                            ),
                                                            occurs = ChildElementDefinition.Occurs.Once
                                                        ),
                                                        ChildElementDefinition.Either(
                                                            options = listOf(
                                                                ChildElementDefinition.Single(
                                                                    elementDefinition = ElementDefinition.ParsedCharacterData(
                                                                        elementName = "HOLIDAY",
                                                                        attributes = emptyList(),
                                                                        comment = null,
                                                                    ),
                                                                    occurs = ChildElementDefinition.Occurs.Once
                                                                ),
                                                                ChildElementDefinition.Single(
                                                                    elementDefinition = ElementDefinition.WithChildren(
                                                                        elementName = "PROGRAMSLOT",
                                                                        attributes = listOf(
                                                                            AttributeDefinition(
                                                                                attributeType = AttributeDefinition.Type.CharacterData,
                                                                                attributeName = "VTR",
                                                                                value = AttributeDefinition.Value.Implied,
                                                                                comment = null,
                                                                            )
                                                                        ),
                                                                        children = listOf(
                                                                            ChildElementDefinition.Single(
                                                                                elementDefinition = ElementDefinition.ParsedCharacterData(
                                                                                    elementName = "TIME",
                                                                                    attributes = emptyList(),
                                                                                    comment = null,
                                                                                ),
                                                                                occurs = ChildElementDefinition.Occurs.Once
                                                                            ),
                                                                            ChildElementDefinition.Single(
                                                                                elementDefinition = ElementDefinition.ParsedCharacterData(
                                                                                    elementName = "TITLE",
                                                                                    attributes = listOf(
                                                                                        AttributeDefinition(
                                                                                            attributeType = AttributeDefinition.Type.CharacterData,
                                                                                            attributeName = "RATING",
                                                                                            value = AttributeDefinition.Value.Implied,
                                                                                            comment = null,
                                                                                        ),
                                                                                        AttributeDefinition(
                                                                                            attributeType = AttributeDefinition.Type.CharacterData,
                                                                                            attributeName = "LANGUAGE",
                                                                                            value = AttributeDefinition.Value.Implied,
                                                                                            comment = null,
                                                                                        ),
                                                                                    ),
                                                                                    comment = null,
                                                                                ),
                                                                                occurs = ChildElementDefinition.Occurs.Once
                                                                            ),
                                                                            ChildElementDefinition.Single(
                                                                                elementDefinition = ElementDefinition.ParsedCharacterData(
                                                                                    elementName = "DESCRIPTION",
                                                                                    attributes = emptyList(),
                                                                                    comment = null,
                                                                                ),
                                                                                occurs = ChildElementDefinition.Occurs.AtMostOnce
                                                                            ),
                                                                        ),
                                                                        comment = null,
                                                                    ),
                                                                    occurs = ChildElementDefinition.Occurs.AtLeastOnce
                                                                )
                                                            ),
                                                            occurs = ChildElementDefinition.Occurs.AtLeastOnce
                                                        )
                                                    ),
                                                    comment = null,
                                                ),
                                                occurs = ChildElementDefinition.Occurs.AtLeastOnce
                                            )
                                        ),
                                        comment = null,
                                    ),
                                    occurs = ChildElementDefinition.Occurs.AtLeastOnce
                                )
                            ),
                            comment = null,
                        ),
                        entities = emptyList()
                    ),
            """
                <!DOCTYPE NEWSPAPER [

                <!ELEMENT NEWSPAPER (ARTICLE+)>
                <!ELEMENT ARTICLE (HEADLINE,BYLINE,LEAD,BODY,NOTES)>
                <!ELEMENT HEADLINE (#PCDATA)>
                <!ELEMENT BYLINE (#PCDATA)>
                <!ELEMENT LEAD (#PCDATA)>
                <!ELEMENT BODY (#PCDATA)>
                <!ELEMENT NOTES (#PCDATA)>

                <!ATTLIST ARTICLE AUTHOR CDATA #REQUIRED>
                <!ATTLIST ARTICLE EDITOR CDATA #IMPLIED>
                <!ATTLIST ARTICLE DATE CDATA #IMPLIED>
                <!ATTLIST ARTICLE EDITION CDATA #IMPLIED>

                <!ENTITY NEWSPAPER "Vervet Logic Times">
                <!ENTITY PUBLISHER "Vervet Logic Press">
                <!ENTITY COPYRIGHT "Copyright 1998 Vervet Logic Press">

                ]> 
            """.trimIndent() to
                    DocumentTypeDefinition(
                        rootElement = ElementDefinition.WithChildren(
                            elementName = "NEWSPAPER",
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
                    ),
            """
                <!DOCTYPE HOLIDAY [
                <!ELEMENT HOLIDAY (#PCDATA)*>
                ]>
            """.trimIndent() to DocumentTypeDefinition(
                rootElement = ElementDefinition.Mixed(
                    elementName = "HOLIDAY",
                    attributes = emptyList(),
                    containsPcData = true,
                    children = emptyList(),
                    comment = null,
                ),
                entities = emptyList()
            )
        )
    }
}
