package com.chriskite.cucumber_rs.intellij.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.icons.AllIcons
import javax.swing.Icon

/**
 * Run configuration type for cucumber-rs tests.
 *
 * Registers "Cucumber Rust" as a run configuration type in the IDE,
 * allowing users to create and manage run configurations for cucumber-rs tests.
 */
class CucumberRustConfigurationType : ConfigurationType {

    override fun getDisplayName(): String = "Cucumber Rust"

    override fun getConfigurationTypeDescription(): String =
        "Run cucumber-rs BDD tests via cargo test"

    override fun getIcon(): Icon = AllIcons.RunConfigurations.TestState.Run

    override fun getId(): String = "CucumberRustConfiguration"

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(CucumberRustConfigurationFactory(this))
    }
}
