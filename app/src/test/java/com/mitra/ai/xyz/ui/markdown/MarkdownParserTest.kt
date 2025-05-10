package com.mitra.ai.xyz.ui.markdown

import androidx.compose.ui.text.AnnotatedString
import org.junit.Test
import kotlin.test.assertEquals

class MarkdownParserTest {

    private val parser = MarkdownParser()

    @Test
    fun `parses headers correctly`() {
        val input = "# Header 1\n## Header 2"
        val result = parser.parse(input).toAnnotatedString()

        assertEquals(2, result.toString().count { it == '\n' })
        // Add more assertions for styles
    }

    @Test
    fun `parses bold text`() {
        val input = "**bold text**"
        val result = parser.parse(input).toAnnotatedString()

        assertEquals("This is bold text", result.toString())
        // Add style assertions
    }

    @Test
    fun testItalicText() {
        val input = "This is __italic__ text"
        val result = parser.parse(input)

        assertEquals("This is italic text", result.toString())
        // Add style assertions
    }

    @Test
    fun testLinks() {
        val input = "[Example](https://example.com)"
        val result = parser.parse(input)

        assertEquals("Example", result.toString())
        // Add URL handling assertions
    }

    @Test
    fun testCodeBlocks() {
        val input = "```kotlin\nval x = 5\n```"
        val result = parser.parse(input)

        assertEquals("```\nval x = 5\n```", result.toString())
    }

    @Test
    fun testLists() {
        val input = "- Item 1\n- Item 2"
        val result = parser.parse(input)

        assertEquals("• Item 1\n• Item 2", result.toString())
    }

    @Test
    fun testBlockquotes() {
        val input = "> This is a quote"
        val result = parser.parse(input)

        assertEquals("This is a quote", result.toString())
        // Add style assertions
    }

    @Test
    fun testNestedElements() {
        val input = "**Bold and __italic__ together**"
        val result = parser.parse(input)

        assertEquals("Bold and italic together", result.toString())
        // Add nested style assertions
    }
}
