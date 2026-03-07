package com.chriskite.cucumber_rs.intellij.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.cucumber.psi.GherkinFeature
import org.jetbrains.plugins.cucumber.psi.GherkinFile
import org.jetbrains.plugins.cucumber.psi.GherkinScenario
import org.jetbrains.plugins.cucumber.psi.GherkinStepsHolder

/**
 * Automatically creates run configurations from the context when a user
 * right-clicks or uses the run gutter icon on a .feature file.
 *
 * This producer detects Gherkin files and scenarios to create appropriate
 * Cucumber Rust run configurations.
 */
class CucumberRustRunConfigurationProducer : LazyRunConfigurationProducer<CucumberRustRunConfiguration>() {

    override fun getConfigurationFactory(): ConfigurationFactory {
        return CucumberRustConfigurationFactory(CucumberRustConfigurationType())
    }

    override fun setupConfigurationFromContext(
        configuration: CucumberRustRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val element = context.psiLocation ?: return false
        val file = element.containingFile

        // Only handle Gherkin files
        if (file !is GherkinFile) return false

        val virtualFile = file.virtualFile ?: return false
        val project = context.project

        // Set the feature file path
        configuration.featureFilePath = virtualFile.path

        // Set working directory to project root
        configuration.workingDirectory = project.basePath

        // Try to determine the test target from Cargo.toml
        val testTarget = findTestTargetForFeature(virtualFile.path, project.basePath)
        if (testTarget != null) {
            configuration.testTarget = testTarget
        }

        // Set configuration name
        val scenarioName = findScenarioName(element)
        configuration.name = if (scenarioName != null) {
            "Cucumber: $scenarioName"
        } else {
            "Cucumber: ${virtualFile.nameWithoutExtension}"
        }

        sourceElement.set(element)
        return true
    }

    override fun isConfigurationFromContext(
        configuration: CucumberRustRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val element = context.psiLocation ?: return false
        val file = element.containingFile as? GherkinFile ?: return false
        val virtualFile = file.virtualFile ?: return false

        return configuration.featureFilePath == virtualFile.path
    }

    /**
     * Try to find the test target name that runs cucumber tests for a given feature file.
     */
    private fun findTestTargetForFeature(featurePath: String, projectBasePath: String?): String? {
        if (projectBasePath == null) return null

        // Common convention: tests/<name>.rs corresponds to [[test]] name = "<name>"
        // Feature files are usually in tests/features/ directory
        val relativePath = featurePath.removePrefix("$projectBasePath/")

        // Try to infer from the path structure
        // e.g., tests/features/eating.feature -> test target might be "eating" or a general test runner
        val featureName = java.io.File(featurePath).nameWithoutExtension

        // Check if there's a test file matching the feature name
        val testFile = java.io.File("$projectBasePath/tests/$featureName.rs")
        if (testFile.exists()) {
            return featureName
        }

        // Check for common cucumber test runner names
        val commonNames = listOf("cucumber", "bdd", "features", "integration")
        for (name in commonNames) {
            val file = java.io.File("$projectBasePath/tests/$name.rs")
            if (file.exists()) {
                return name
            }
        }

        return null
    }

    /**
     * Find the scenario name at the current cursor position.
     */
    private fun findScenarioName(element: PsiElement): String? {
        var current: PsiElement? = element
        while (current != null) {
            if (current is GherkinStepsHolder) {
                return current.scenarioName
            }
            current = current.parent
        }
        return null
    }
}
