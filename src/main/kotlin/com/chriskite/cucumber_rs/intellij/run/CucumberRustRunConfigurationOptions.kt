package com.chriskite.cucumber_rs.intellij.run

import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.openapi.components.StoredProperty

/**
 * Stores the options/settings for a Cucumber Rust run configuration.
 */
class CucumberRustRunConfigurationOptions : RunConfigurationOptions() {

    private val myFeatureFilePath: StoredProperty<String?> =
        string("").provideDelegate(this, "featureFilePath")

    private val myTestTarget: StoredProperty<String?> =
        string("").provideDelegate(this, "testTarget")

    private val myWorkingDirectory: StoredProperty<String?> =
        string("").provideDelegate(this, "workingDirectory")

    private val myAdditionalArgs: StoredProperty<String?> =
        string("").provideDelegate(this, "additionalArgs")

    private val myEnvironmentVariables: StoredProperty<String?> =
        string("").provideDelegate(this, "environmentVariables")

    var featureFilePath: String?
        get() = myFeatureFilePath.getValue(this)
        set(value) { myFeatureFilePath.setValue(this, value) }

    var testTarget: String?
        get() = myTestTarget.getValue(this)
        set(value) { myTestTarget.setValue(this, value) }

    var workingDirectory: String?
        get() = myWorkingDirectory.getValue(this)
        set(value) { myWorkingDirectory.setValue(this, value) }

    var additionalArgs: String?
        get() = myAdditionalArgs.getValue(this)
        set(value) { myAdditionalArgs.setValue(this, value) }

    var environmentVariables: String?
        get() = myEnvironmentVariables.getValue(this)
        set(value) { myEnvironmentVariables.setValue(this, value) }
}
