package com.boswelja.xmldtd.codegen

import kotlin.test.Test
import kotlin.test.assertEquals

class NameProcessorTest {
    @Test
    fun `toPascalCase()`() {
        val testCases = mapOf(
            "PascalCase" to "PascalCase",
            "camelCase" to "CamelCase",
            "kebab-case" to "KebabCase",
            "snake_case" to "SnakeCase",
            "SCREAMING-KEBAB" to "ScreamingKebab",
            "SCREAMING_SNAKE" to "ScreamingSnake",
            "" to ""
        )

        testCases.forEach { (input, expected) ->
            assertEquals(
                expected,
                input.toPascalCase()
            )
        }
    }

    @Test
    fun `toCamelCase()`() {
        val testCases = mapOf(
            "camelCase" to "camelCase",
            "PascalCase" to "pascalCase",
            "kebab-case" to "kebabCase",
            "snake_case" to "snakeCase",
            "SCREAMING-KEBAB" to "screamingKebab",
            "SCREAMING_SNAKE" to "screamingSnake",
            "" to ""
        )

        testCases.forEach { (input, expected) ->
            assertEquals(
                expected,
                input.toCamelCase()
            )
        }
    }

    @Test
    fun `toPlural()`() {
        val testCases = mapOf(
            "entry" to "entries",
            "ENTRY" to "ENTRIES",
            "gloss" to "glosses",
            "GLOSS" to "GLOSSES",
            "cactus" to "cacti",
            "focus" to "foci",
            "tax" to "taxes",
            "CasePreserve" to "CasePreserves",
            "" to ""
        )

        testCases.forEach { (input, expected) ->
            assertEquals(
                expected,
                input.toPlural()
            )
        }
    }
}
