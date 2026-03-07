package com.chriskite.cucumber_rs.intellij.util

import com.chriskite.cucumber_rs.intellij.CucumberRustBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

/**
 * Data class representing a parsed step definition from a Rust source file.
 */
data class ParsedStepDefinition(
    /** The step keyword: "given", "when", or "then". */
    val keyword: String,
    /** The step pattern (either a cucumber expression or regex). */
    val pattern: String,
    /** Whether this step uses a regex pattern (vs cucumber expression). */
    val isRegex: Boolean,
    /** The name of the function implementing the step. */
    val functionName: String,
    /** The offset in the file where the attribute starts. */
    val attributeOffset: Int,
    /** The offset in the file where the function starts. */
    val functionOffset: Int
)

/**
 * Utility functions for working with Rust step definitions.
 */
object RustStepDefinitionUtil {

    /**
     * Parse all step definitions from a Rust source file's text content.
     */
    fun parseStepDefinitions(text: String): List<ParsedStepDefinition> {
        val results = mutableListOf<ParsedStepDefinition>()

        // Match the three forms of cucumber-rs step attributes:
        // 1. Simple string:  #[given("pattern")]
        // 2. Expr form:      #[given(expr = "pattern")]
        // 3. Regex form:     #[when(regex = r"pattern")]  or  #[when(regex = r#"pattern"#)]
        //
        // We use separate patterns for clarity and correctness.

        // Pattern for raw string with hash: r#"..."#
        val rawHashPattern = Regex(
            """#\[(given|when|then)\s*\(\s*(?:(expr|regex)\s*=\s*)?r#"(.*?)"#\s*\)\s*\]""",
            RegexOption.DOT_MATCHES_ALL
        )

        // Pattern for raw string without hash: r"..."
        val rawPattern = Regex(
            """#\[(given|when|then)\s*\(\s*(?:(expr|regex)\s*=\s*)?r"(.*?)"\s*\)\s*\]""",
            RegexOption.DOT_MATCHES_ALL
        )

        // Pattern for normal string: "..."
        val simplePattern = Regex(
            """#\[(given|when|then)\s*\(\s*(?:(expr|regex)\s*=\s*)?"(.*?)"\s*\)\s*\]""",
            RegexOption.DOT_MATCHES_ALL
        )

        // Collect all matches with their offsets to avoid duplicates
        data class AttrMatch(val keyword: String, val qualifier: String, val pattern: String, val offset: Int, val endOffset: Int)

        val allMatches = mutableListOf<AttrMatch>()

        for (match in rawHashPattern.findAll(text)) {
            allMatches.add(AttrMatch(match.groupValues[1], match.groupValues[2], match.groupValues[3], match.range.first, match.range.last))
        }
        for (match in rawPattern.findAll(text)) {
            // Skip if this offset is already covered by a rawHashPattern match
            if (allMatches.none { it.offset == match.range.first }) {
                allMatches.add(AttrMatch(match.groupValues[1], match.groupValues[2], match.groupValues[3], match.range.first, match.range.last))
            }
        }
        for (match in simplePattern.findAll(text)) {
            // Skip if this offset is already covered
            if (allMatches.none { it.offset == match.range.first }) {
                allMatches.add(AttrMatch(match.groupValues[1], match.groupValues[2], match.groupValues[3], match.range.first, match.range.last))
            }
        }

        // Sort by offset to process in order
        allMatches.sortBy { it.offset }

        for (attrMatch in allMatches) {
            val isRegex = attrMatch.qualifier == "regex"
            val attrOffset = attrMatch.offset

            // Find the function declaration following this attribute
            val remainingText = text.substring(attrMatch.endOffset + 1)
            val fnMatch = CucumberRustBundle.FUNCTION_PATTERN.find(remainingText)
            if (fnMatch != null) {
                val functionName = fnMatch.groupValues[1]
                val functionOffset = attrMatch.endOffset + 1 + fnMatch.range.first

                results.add(
                    ParsedStepDefinition(
                        keyword = attrMatch.keyword,
                        pattern = attrMatch.pattern,
                        isRegex = isRegex,
                        functionName = functionName,
                        attributeOffset = attrOffset,
                        functionOffset = functionOffset
                    )
                )
            }
        }

        return results
    }

    /**
     * Convert a Cucumber Expression to a regex pattern for matching.
     * Cucumber expressions use {type} placeholders that need to be converted to regex groups.
     */
    fun cucumberExpressionToRegex(expression: String): String {
        // Split the expression around {type} placeholders, escaping literal parts
        // and replacing placeholders with appropriate regex groups.
        val placeholderRegex = Regex("""\{(\w*)\}""")
        val sb = StringBuilder()
        sb.append("^")

        var lastEnd = 0
        for (match in placeholderRegex.findAll(expression)) {
            // Escape the literal text before this placeholder
            val literalBefore = expression.substring(lastEnd, match.range.first)
            if (literalBefore.isNotEmpty()) {
                sb.append(Regex.escape(literalBefore))
            }

            // Replace placeholder with regex group
            val type = match.groupValues[1]
            val replacement = when (type) {
                "int" -> """(-?\d+)"""
                "float" -> """(-?\d+\.\d+)"""
                "word" -> """(\S+)"""
                "string" -> """(?:"([^"]*)"|'([^']*)')"""
                "" -> """(.*)"""
                else -> """(.+)"""
            }
            sb.append(replacement)

            lastEnd = match.range.last + 1
        }

        // Escape any remaining literal text after the last placeholder
        val literalAfter = expression.substring(lastEnd)
        if (literalAfter.isNotEmpty()) {
            sb.append(Regex.escape(literalAfter))
        }

        sb.append("$")
        return sb.toString()
    }

    /**
     * Find all Rust files in a project that might contain step definitions.
     */
    fun findRustFiles(project: Project, scope: GlobalSearchScope): List<PsiFile> {
        val psiManager = PsiManager.getInstance(project)
        val rustFiles = mutableListOf<PsiFile>()

        FilenameIndex.getAllFilesByExt(project, CucumberRustBundle.RUST_FILE_EXTENSION, scope)
            .forEach { vFile ->
                psiManager.findFile(vFile)?.let { psiFile ->
                    // Only include files that might have cucumber step definitions
                    val text = psiFile.text
                    if (containsStepDefinitions(text)) {
                        rustFiles.add(psiFile)
                    }
                }
            }

        return rustFiles
    }

    /**
     * Quick check if a file's text might contain step definitions.
     */
    fun containsStepDefinitions(text: String): Boolean {
        return CucumberRustBundle.STEP_DEFINITION_MACROS.any { macro ->
            text.contains("#[$macro(") || text.contains("#[$macro (")
        }
    }

    /**
     * Check if a virtual file is a Rust file.
     */
    fun isRustFile(file: VirtualFile): Boolean {
        return file.extension == CucumberRustBundle.RUST_FILE_EXTENSION
    }

    /**
     * Generate a step definition function signature for a given step.
     */
    fun generateStepDefinition(keyword: String, pattern: String, functionName: String): String {
        // Determine parameters from the pattern
        val params = extractParametersFromPattern(pattern)

        val paramList = if (params.isEmpty()) {
            "w: &mut World"
        } else {
            "w: &mut World, ${params.joinToString(", ") { "${it.first}: ${it.second}" }}"
        }

        return buildString {
            appendLine("#[${keyword.lowercase()}(\"$pattern\")]")
            appendLine("async fn $functionName($paramList) {")
            appendLine("    todo!(\"Implement step\")")
            appendLine("}")
        }
    }

    /**
     * Extract parameter names and types from a Cucumber expression.
     */
    private fun extractParametersFromPattern(pattern: String): List<Pair<String, String>> {
        val params = mutableListOf<Pair<String, String>>()
        val paramRegex = Regex("""\{(\w*)\}""")
        var paramIndex = 0

        for (match in paramRegex.findAll(pattern)) {
            val type = match.groupValues[1]
            val paramName = "param${paramIndex++}"
            val rustType = when (type) {
                "int" -> "i32"
                "float" -> "f64"
                "word" -> "String"
                "string" -> "String"
                "" -> "String"
                else -> "String"
            }
            params.add(Pair(paramName, rustType))
        }

        return params
    }

    /**
     * Convert a step text to a suitable function name.
     */
    fun stepTextToFunctionName(stepText: String): String {
        return stepText
            .lowercase()
            .replace(Regex("""[^a-z0-9\s]"""), "")
            .trim()
            .replace(Regex("""\s+"""), "_")
            .take(50)
    }
}
