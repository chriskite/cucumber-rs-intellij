package com.chriskite.cucumber_rs.intellij.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project

/**
 * Factory for creating Cucumber Rust run configurations.
 */
class CucumberRustConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {

    override fun getId(): String = "CucumberRustConfigurationFactory"

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return CucumberRustRunConfiguration(project, this, "Cucumber Rust")
    }

    override fun getOptionsClass(): Class<out BaseState> {
        return CucumberRustRunConfigurationOptions::class.java
    }
}
