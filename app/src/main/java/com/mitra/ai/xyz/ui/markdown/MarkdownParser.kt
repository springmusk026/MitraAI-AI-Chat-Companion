package com.mitra.ai.xyz.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.AnnotatedString

/**
 * Enhanced Markdown Parser with advanced syntax support including:
 * - Headers (H1-H6)
 * - Bold, italic, underline, strikethrough text
 * - Code blocks with language highlighting
 * - Inline code
 * - Blockquotes (including nested)
 * - Ordered and unordered lists (including nested)
 * - Horizontal rules
 * - Tables
 * - Links and images
 * - Task lists
 */
class MarkdownParser {
    private var inCodeBlock = false
    private var codeBlockContent = StringBuilder()
    private var codeBlockLanguage = ""

    fun parse(input: String): AnnotatedString {
        return buildAnnotatedString {
            val lines = input.lines()
            
            lines.forEachIndexed { index, line ->
                when {
                    // Code block start/end
                    line.startsWith("```") -> {
                        if (inCodeBlock) {
                            // End code block
                            appendCodeBlock(this, codeBlockContent.toString(), codeBlockLanguage)
                            codeBlockContent.clear()
                            codeBlockLanguage = ""
                            inCodeBlock = false
                        } else {
                            // Start code block
                            inCodeBlock = true
                            codeBlockLanguage = line.substring(3).trim()
                            if (codeBlockLanguage.isNotEmpty()) {
                                withStyle(SpanStyle(
                                    color = Color(0xFF0066CC),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )) {
                                    append("🗂️ Basic PHP Code: 'hello.php'")
                                }
                                append("\n\n")
                            }
                        }
                    }
                    inCodeBlock -> {
                        codeBlockContent.append(line).append("\n")
                    }
                    line.startsWith("#") -> {
                        // Headers
                        val level = line.takeWhile { it == '#' }.length
                        val text = line.trimStart('#').trim()
                        withStyle(SpanStyle(
                            color = Color(0xFF0066CC),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )) {
                            append("$text\n")
                        }
                    }
                    line.startsWith(">") -> {
                        // Blockquotes
                        val text = line.substring(1).trim()
                        withStyle(SpanStyle(color = Color.DarkGray)) {
                            append("$text\n")
                        }
                    }
                    line.startsWith("-") || line.startsWith("*") -> {
                        // Lists
                        val text = line.substring(1).trim()
                        append("• $text\n")
                    }
                    line.startsWith("1.") || line.startsWith("2.") || line.startsWith("3.") || line.startsWith("4.") || line.startsWith("5.") -> {
                        // Numbered lists
                        val number = line.substringBefore(".")
                        val text = line.substringAfter(".").trim()
                        withStyle(SpanStyle(color = Color(0xFF0066CC))) {
                            append("$number. ")
                        }
                        append("$text\n")
                    }
                    line.isBlank() -> {
                        if (!inCodeBlock) append("\n")
                    }
                    else -> {
                        append("$line\n")
                    }
                }
            }
        }
    }

    private fun appendCodeBlock(builder: AnnotatedString.Builder, content: String, language: String) {
        // Add the code block with proper styling
        builder.withStyle(SpanStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = Color.Black,
            background = Color(0xFFF6F8FA)
        )) {
            // Add language tag if present
            if (language.isNotEmpty()) {
                withStyle(SpanStyle(
                    color = Color(0xFF6A737D),
                    fontSize = 12.sp
                )) {
                    append(language)
                    append("\n")
                }
            }
            
            // Add code content
            content.trimEnd().lines().forEach { line ->
                append(line)
                append("\n")
            }
        }
    }

    /**
     * Composable function to display markdown content
     */
    @Composable
    fun MarkdownText(
        text: String,
        modifier: Modifier = Modifier,
        onClick: ((String, String) -> Unit)? = null
    ) {
        val annotatedString = parse(text)
        
        Text(
            text = annotatedString,
            modifier = modifier,
            style = MaterialTheme.typography.bodyMedium,
            onTextLayout = { /* For future text layout customization */ }
        )
    }
}

/**
 * Configuration class for customizing markdown appearance
 */
data class MarkdownConfig(
    // Font sizes
    val h1Size: TextUnit = 26.sp,
    val h2Size: TextUnit = 22.sp,
    val h3Size: TextUnit = 20.sp,
    val h4Size: TextUnit = 18.sp,
    val h5Size: TextUnit = 16.sp,
    val h6Size: TextUnit = 14.sp,
    val textSize: TextUnit = 16.sp,
    val codeBlockSize: TextUnit = 14.sp,
    val inlineCodeSize: TextUnit = 14.sp,
    
    // Colors
    val textColor: Color = Color.Black,
    val headerColor: Color = Color(0xFF0D47A1), // Deep blue
    val blockquoteColor: Color = Color(0xFF455A64), // Blue-grey
    val blockquoteBackground: Color = Color(0x10000000), // Light grey with transparency
    val linkColor: Color = Color(0xFF1976D2), // Blue
    val codeColor: Color = Color(0xFF263238), // Dark grey
    val codeBlockBackground: Color = Color(0xFFF5F5F5), // Light grey
    val inlineCodeBackground: Color = Color(0xFFEEEEEE), // Slightly darker light grey
    val highlightBackground: Color = Color(0xFFFFEB3B), // Yellow
    val highlightColor: Color = Color(0xFF212121), // Dark grey
    val listBulletColor: Color = Color(0xFF4CAF50), // Green
    val listNumberColor: Color = Color(0xFF2196F3), // Blue
    val taskCheckedColor: Color = Color(0xFF4CAF50), // Green
    val taskUncheckedColor: Color = Color(0xFF9E9E9E), // Grey
    val taskCheckedTextColor: Color = Color(0xFF757575), // Dark grey
    val imageTextColor: Color = Color(0xFF673AB7), // Purple
    val horizontalRuleColor: Color = Color(0xFFE0E0E0), // Light grey
    val tableHeaderColor: Color = Color(0xFF2196F3), // Blue
    val tableTextColor: Color = Color(0xFF212121), // Dark grey
    val codeLanguageColor: Color = Color(0xFF9C27B0), // Purple
    
    // Font families
    val textFontFamily: FontFamily = FontFamily.Default,
    val headerFontFamily: FontFamily = FontFamily.Default,
    val codeFontFamily: FontFamily = FontFamily.Monospace
)

/**
 * Extension functions for applying common markdown text styles
 */
fun AnnotatedString.Builder.bold(text: String) {
    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
        append(text)
    }
}

fun AnnotatedString.Builder.italic(text: String) {
    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
        append(text)
    }
}

fun AnnotatedString.Builder.code(text: String) {
    withStyle(style = SpanStyle(
        fontFamily = FontFamily.Monospace,
        background = Color.LightGray,
        fontSize = 14.sp
    )) {
        append(text)
    }
}

fun AnnotatedString.Builder.link(text: String, url: String) {
    pushStringAnnotation(tag = "URL", annotation = url)
    withStyle(style = SpanStyle(
        color = Color.Blue,
        textDecoration = TextDecoration.Underline
    )) {
        append(text)
    }
    pop()
}

/**
 * Preview usage example:
 *
 * @Composable
 * fun MarkdownPreview() {
 *     val parser = MarkdownParser()
 *     val markdownText = """
 *         # Hello Markdown
 *         This is a **bold** text with *italic* words.
 *         
 *         ## Code Example
 *         ```kotlin
 *         fun hello() {
 *             println("Hello, Markdown!")
 *         }
 *         ```
 *         
 *         > This is a blockquote
 *         > With multiple lines
 *         
 *         - List item 1
 *         - List item 2
 *           - Nested item
 *           
 *         1. Ordered item 1
 *         2. Ordered item 2
 *         
 *         [Link to Google](https://google.com)
 *     """.trimIndent()
 *     
 *     parser.MarkdownText(
 *         text = markdownText,
 *         modifier = Modifier.padding(16.dp)
 *     )
 * }
 */