package com.chriskite.cucumber_rs.intellij.steps

import com.chriskite.cucumber_rs.intellij.CucumberRustBundle
import com.chriskite.cucumber_rs.intellij.util.RustStepDefinitionUtil
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.plugins.cucumber.BDDFrameworkType
import org.jetbrains.plugins.cucumber.CucumberJvmExtensionPoint
import org.jetbrains.plugins.cucumber.StepDefinitionCreator
import org.jetbrains.plugins.cucumber.psi.GherkinFile
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition

/**
 * Main extension point implementation that integrates cucumber-rs with the IntelliJ Cucumber plugin.
 *
 * This class tells the Cucumber plugin how to:
 * - Find Rust files containing step definitions
 * - Parse step definitions from #[given/when/then] attributes
 * - Navigate between .feature steps and their Rust implementations
 * - Create new step definitions in Rust
 */
class RustCucumberExtension : CucumberJvmExtensionPoint {

    override fun isStepLikeFile(child: PsiElement, parent: PsiElement): Boolean {
        val file = child.containingFile?.virtualFile ?: return false
        if (!RustStepDefinitionUtil.isRustFile(file)) return false

        // Check if the file contains cucumber step definition macros
        val text = child.containingFile?.text ?: return false
        return RustStepDefinitionUtil.containsStepDefinitions(text)
    }

    override fun isWritableStepLikeFile(child: PsiElement, parent: PsiElement): Boolean {
        val file = child.containingFile?.virtualFile ?: return false
        return RustStepDefinitionUtil.isRustFile(file) && file.isWritable
    }

    override fun getStepFileType(): BDDFrameworkType {
        // Use PlainTextFileType as a fallback since Rust file type may not be available
        // (depends on whether the Rust plugin is installed)
        return BDDFrameworkType(PlainTextFileType.INSTANCE, "Rust (cucumber-rs)")
    }

    override fun getStepDefinitionCreator(): StepDefinitionCreator {
        return RustStepDefinitionCreator()
    }

    /**
     * Find all .rs files, trying the module scope first and falling back to project scope.
     *
     * RustRover's module model often doesn't populate [GlobalSearchScope.moduleWithDependenciesAndLibrariesScope]
     * with Rust sources, so the module-scoped query returns 0 files even though the project index has them.
     */
    private fun findRustVirtualFiles(project: Project, module: Module): Collection<VirtualFile> {
        val moduleScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
        val moduleFiles = FilenameIndex.getAllFilesByExt(project, CucumberRustBundle.RUST_FILE_EXTENSION, moduleScope)
        if (moduleFiles.isNotEmpty()) {
            return moduleFiles
        }

        // Module scope returned nothing — fall back to project scope.
        val projectScope = GlobalSearchScope.projectScope(project)
        return FilenameIndex.getAllFilesByExt(project, CucumberRustBundle.RUST_FILE_EXTENSION, projectScope)
    }

    override fun loadStepsFor(featureFile: PsiFile?, module: Module): List<AbstractStepDefinition> {
        val stepDefinitions = mutableListOf<AbstractStepDefinition>()
        val project = module.project
        val psiManager = PsiManager.getInstance(project)

        val rustFiles = findRustVirtualFiles(project, module)

        for (virtualFile in rustFiles) {
            val psiFile = psiManager.findFile(virtualFile) ?: continue
            val text = psiFile.text

            // Quick check before parsing
            if (!RustStepDefinitionUtil.containsStepDefinitions(text)) continue

            // Parse step definitions from this file
            val parsedSteps = RustStepDefinitionUtil.parseStepDefinitions(text)
            for (parsed in parsedSteps) {
                // Find the PSI element at the attribute offset
                val element = psiFile.findElementAt(parsed.attributeOffset)
                if (element != null) {
                    stepDefinitions.add(
                        RustStepDefinition(
                            element = element,
                            stepPattern = parsed.pattern,
                            isRegexPattern = parsed.isRegex
                        )
                    )
                }
            }
        }

        return stepDefinitions
    }

    override fun getStepDefinitionContainers(file: GherkinFile): Collection<PsiFile> {
        val module = ModuleUtilCore.findModuleForFile(file)
        val project = file.project
        val psiManager = PsiManager.getInstance(project)

        val rustVirtualFiles = if (module != null) {
            findRustVirtualFiles(project, module)
        } else {
            // No module found — search the whole project
            val projectScope = GlobalSearchScope.projectScope(project)
            FilenameIndex.getAllFilesByExt(project, CucumberRustBundle.RUST_FILE_EXTENSION, projectScope)
        }

        return rustVirtualFiles.mapNotNull { vFile ->
            psiManager.findFile(vFile)?.takeIf { psiFile ->
                RustStepDefinitionUtil.containsStepDefinitions(psiFile.text)
            }
        }
    }

    override fun isGherkin6Supported(module: Module): Boolean {
        // cucumber-rs supports Gherkin 6 (Rule keyword)
        return true
    }
}
