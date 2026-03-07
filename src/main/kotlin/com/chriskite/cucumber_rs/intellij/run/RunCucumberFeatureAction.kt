package com.chriskite.cucumber_rs.intellij.run

import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

/**
 * Action to run cucumber-rs tests for the currently open .feature file.
 *
 * This action creates a temporary run configuration and executes it,
 * running `cargo test` with the appropriate test target.
 */
class RunCucumberFeatureAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        if (file.extension != "feature") return

        val runManager = RunManager.getInstance(project)
        val configurationType = CucumberRustConfigurationType()
        val factory = configurationType.configurationFactories.first()

        val settings = runManager.createConfiguration(
            "Cucumber: ${file.nameWithoutExtension}",
            factory
        )

        val configuration = settings.configuration as CucumberRustRunConfiguration
        configuration.featureFilePath = file.path
        configuration.workingDirectory = project.basePath

        // Make the configuration temporary
        settings.isTemporary = true
        runManager.addConfiguration(settings)
        runManager.selectedConfiguration = settings

        val executor = DefaultRunExecutor.getRunExecutorInstance()
        val environment = ExecutionEnvironmentBuilder.createOrNull(executor, settings)?.build()
        if (environment != null) {
            ExecutionManager.getInstance(project).restartRunProfile(environment)
        }
    }

    override fun update(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        event.presentation.isEnabledAndVisible = file?.extension == "feature"
    }
}
