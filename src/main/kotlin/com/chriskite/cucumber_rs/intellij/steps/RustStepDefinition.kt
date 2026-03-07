package com.chriskite.cucumber_rs.intellij.steps

import com.chriskite.cucumber_rs.intellij.util.RustStepDefinitionUtil
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition

/**
 * Represents a single Rust cucumber step definition parsed from a #[given/when/then] attribute.
 *
 * This class bridges the cucumber-rs attribute macros with the IntelliJ Cucumber plugin's
 * step definition framework, enabling navigation, matching, and other IDE features.
 */
class RustStepDefinition(
    element: PsiElement,
    private val stepPattern: String,
    private val isRegexPattern: Boolean
) : AbstractStepDefinition(element) {

    override fun getVariableNames(): List<String> {
        // Extract variable names from the pattern
        val params = mutableListOf<String>()
        if (isRegexPattern) {
            // For regex patterns, count capturing groups
            val groupPattern = Regex("""\((?!\?)""")
            var index = 0
            for (match in groupPattern.findAll(stepPattern)) {
                params.add("param$index")
                index++
            }
        } else {
            // For cucumber expressions, extract {type} placeholders
            val placeholderPattern = Regex("""\{(\w*)\}""")
            var index = 0
            for (match in placeholderPattern.findAll(stepPattern)) {
                params.add(match.groupValues[1].ifEmpty { "param$index" })
                index++
            }
        }
        return params
    }

    /**
     * Returns the step pattern as a regex for matching against step text in .feature files.
     *
     * The base class flow is: [getPattern] → [getCucumberRegex] → [getExpression].
     * Since [getExpression] returns the raw cucumber expression (e.g. "she eats {int} cucumbers"),
     * we must override [getCucumberRegex] to return the regex-converted form so that
     * [getPattern] compiles a regex that actually matches step text like "she eats 3 cucumbers".
     */
    override fun getCucumberRegex(): String? {
        return if (isRegexPattern) {
            stepPattern
        } else {
            RustStepDefinitionUtil.cucumberExpressionToRegex(stepPattern)
        }
    }

    override fun getCucumberRegexFromElement(element: PsiElement?): String? {
        // Not called by the base class when getExpression() is overridden,
        // but implemented for completeness.
        if (element == null) return null
        return getCucumberRegex()
    }

    /**
     * Returns the original step expression as written in the Rust source.
     * Used for display and navigation purposes.
     */
    override fun getExpression(): String {
        return stepPattern
    }
}
