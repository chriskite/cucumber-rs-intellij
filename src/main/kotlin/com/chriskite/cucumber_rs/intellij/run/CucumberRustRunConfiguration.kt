package com.chriskite.cucumber_rs.intellij.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jdom.Element

/**
 * Run configuration for executing cucumber-rs BDD tests.
 *
 * This runs `cargo test --test <target>` which executes the cucumber-rs test binary,
 * which in turn reads .feature files and matches them against step definitions.
 */
class CucumberRustRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : RunConfigurationBase<CucumberRustRunConfigurationOptions>(project, factory, name) {

    override fun getOptions(): CucumberRustRunConfigurationOptions {
        return super.getOptions() as CucumberRustRunConfigurationOptions
    }

    var featureFilePath: String?
        get() = options.featureFilePath
        set(value) { options.featureFilePath = value }

    var testTarget: String?
        get() = options.testTarget
        set(value) { options.testTarget = value }

    var workingDirectory: String?
        get() = options.workingDirectory
        set(value) { options.workingDirectory = value }

    var additionalArgs: String?
        get() = options.additionalArgs
        set(value) { options.additionalArgs = value }

    var environmentVariables: String?
        get() = options.environmentVariables
        set(value) { options.environmentVariables = value }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return CucumberRustSettingsEditor(project)
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return object : CommandLineState(environment) {
            override fun startProcess(): ProcessHandler {
                val commandLine = GeneralCommandLine()
                commandLine.exePath = "cargo"

                val args = mutableListOf("test")

                // Add test target if specified
                testTarget?.takeIf { it.isNotBlank() }?.let {
                    args.add("--test")
                    args.add(it)
                }

                // Add -- separator for test binary args
                args.add("--")

                // If a specific feature file is specified, pass it as an argument
                featureFilePath?.takeIf { it.isNotBlank() }?.let {
                    args.add(it)
                }

                // Add any additional arguments
                additionalArgs?.takeIf { it.isNotBlank() }?.let {
                    args.addAll(it.split(" "))
                }

                commandLine.addParameters(args)

                // Set working directory
                val workDir = workingDirectory?.takeIf { it.isNotBlank() } ?: project.basePath
                workDir?.let { commandLine.workDirectory = java.io.File(it) }

                // Set environment variables
                environmentVariables?.takeIf { it.isNotBlank() }?.let { envStr ->
                    envStr.split(",").forEach { pair ->
                        val parts = pair.trim().split("=", limit = 2)
                        if (parts.size == 2) {
                            commandLine.environment[parts[0].trim()] = parts[1].trim()
                        }
                    }
                }

                val handler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
                ProcessTerminatedListener.attach(handler)
                return handler
            }
        }
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
    }
}
