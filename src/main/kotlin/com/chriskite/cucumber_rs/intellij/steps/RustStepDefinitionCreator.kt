package com.chriskite.cucumber_rs.intellij.steps

import com.chriskite.cucumber_rs.intellij.CucumberRustBundle
import com.chriskite.cucumber_rs.intellij.util.RustStepDefinitionUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import org.jetbrains.plugins.cucumber.StepDefinitionCreator
import org.jetbrains.plugins.cucumber.psi.GherkinStep

/**
 * Creates Rust step definition files and step definition functions.
 *
 * This creator generates properly formatted cucumber-rs step definition code
 * with the appropriate #[given/when/then] attributes and async function signatures.
 */
class RustStepDefinitionCreator : StepDefinitionCreator {

    override fun createStepDefinitionContainer(dir: PsiDirectory, name: String): PsiFile {
        val fileName = if (name.endsWith(".rs")) name else "$name.rs"

        val fileContent = buildString {
            appendLine("use cucumber::{given, when, then};")
            appendLine()
            appendLine("use crate::World;")
            appendLine()
            appendLine("// Step definitions for cucumber-rs")
            appendLine("// See: https://github.com/cucumber-rs/cucumber")
            appendLine()
        }

        val factory = PsiFileFactory.getInstance(dir.project)
        val file = factory.createFileFromText(fileName, com.intellij.openapi.fileTypes.PlainTextFileType.INSTANCE, fileContent)

        return dir.add(file) as PsiFile
    }

    override fun createStepDefinition(step: GherkinStep, file: PsiFile, withTemplate: Boolean): Boolean {
        try {
            val keyword = step.keyword?.text?.trim()?.lowercase() ?: "given"
            // Map "and"/"but" to the appropriate keyword based on context
            val effectiveKeyword = when (keyword) {
                "given", "when", "then" -> keyword
                else -> "given" // Default for "And", "But", "*"
            }

            val stepText = step.name
            val functionName = RustStepDefinitionUtil.stepTextToFunctionName(stepText)
            val stepCode = RustStepDefinitionUtil.generateStepDefinition(effectiveKeyword, stepText, functionName)

            val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance()
                .getDocument(file.virtualFile) ?: return false

            com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(file.project) {
                val text = document.text
                val insertOffset = text.length
                document.insertString(insertOffset, "\n$stepCode\n")
            }

            return true
        } catch (e: Exception) {
            return false
        }
    }

    override fun validateNewStepDefinitionFileName(project: Project, fileName: String): Boolean {
        // Valid Rust file names: alphanumeric, underscores, must end in .rs
        val name = if (fileName.endsWith(".rs")) fileName.substringBeforeLast(".rs") else fileName
        return name.matches(Regex("""[a-zA-Z_][a-zA-Z0-9_]*"""))
    }

    override fun getDefaultStepDefinitionFolderPath(step: GherkinStep): String {
        val featureFile = step.containingFile
        val featureDir = featureFile?.virtualFile?.parent

        // Try to find a "steps" directory near the feature file
        val stepsDir = featureDir?.findChild("steps")
        if (stepsDir != null && stepsDir.isDirectory) {
            return stepsDir.path
        }

        // Try the tests directory
        val project = step.project
        val basePath = project.basePath ?: return CucumberRustBundle.DEFAULT_STEP_DEF_DIR
        val testsDir = com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath("$basePath/tests")
        if (testsDir != null && testsDir.isDirectory) {
            return testsDir.path
        }

        return "$basePath/${CucumberRustBundle.DEFAULT_STEP_DEF_DIR}"
    }

    override fun getStepDefinitionFilePath(file: PsiFile): String {
        val virtualFile = file.virtualFile ?: return file.name
        val projectPath = file.project.basePath ?: return virtualFile.path
        return virtualFile.path.removePrefix("$projectPath/")
    }

    override fun getDefaultStepFileName(step: GherkinStep): String {
        val featureName = step.containingFile?.virtualFile?.nameWithoutExtension ?: "steps"
        return "${featureName}_steps"
    }
}
