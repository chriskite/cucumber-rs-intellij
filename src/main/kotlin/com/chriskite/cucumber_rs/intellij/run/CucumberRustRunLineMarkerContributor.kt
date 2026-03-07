package com.chriskite.cucumber_rs.intellij.run

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.cucumber.psi.GherkinFeature
import org.jetbrains.plugins.cucumber.psi.GherkinScenario
import org.jetbrains.plugins.cucumber.psi.GherkinScenarioOutline
import org.jetbrains.plugins.cucumber.psi.GherkinStepsHolder

/**
 * Adds run/debug gutter icons to Feature and Scenario lines in .feature files.
 *
 * Users can click these icons to quickly run individual features or scenarios
 * using the cucumber-rs test runner.
 */
class CucumberRustRunLineMarkerContributor : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        // We need to check the element type - we want to show markers on
        // Feature, Scenario, and Scenario Outline keywords
        val parent = element.parent ?: return null

        val isFeature = parent is GherkinFeature
        val isScenario = parent is GherkinStepsHolder

        if (!isFeature && !isScenario) return null

        // Only place the marker on the first significant token of the line
        // (the keyword element itself)
        if (element != getFirstKeywordElement(parent)) return null

        val actions = ExecutorAction.getActions(0)
        val tooltipProvider: (PsiElement) -> String = { e ->
            when {
                parent is GherkinFeature -> "Run Feature '${parent.featureName}'"
                parent is GherkinStepsHolder -> "Run Scenario '${parent.scenarioName}'"
                else -> "Run Cucumber Test"
            }
        }

        return Info(
            AllIcons.RunConfigurations.TestState.Run,
            actions,
            tooltipProvider
        )
    }

    /**
     * Get the first keyword element of a Gherkin PSI element.
     */
    private fun getFirstKeywordElement(element: PsiElement): PsiElement? {
        return element.firstChild
    }
}
