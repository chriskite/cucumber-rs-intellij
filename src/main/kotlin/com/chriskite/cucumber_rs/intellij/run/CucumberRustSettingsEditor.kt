package com.chriskite.cucumber_rs.intellij.run

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

/**
 * Settings editor UI for the Cucumber Rust run configuration.
 *
 * Allows users to configure:
 * - Feature file path
 * - Test target name (the [[test]] name in Cargo.toml)
 * - Working directory
 * - Additional cargo test arguments
 * - Environment variables
 */
class CucumberRustSettingsEditor(private val project: Project) :
    SettingsEditor<CucumberRustRunConfiguration>() {

    private val featureFileField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
            "Select Feature File",
            "Select the .feature file to run",
            project,
            FileChooserDescriptorFactory.createSingleFileDescriptor("feature")
        )
    }

    private val testTargetField = JTextField()
    private val workingDirectoryField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
            "Select Working Directory",
            "Select the working directory for cargo test",
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )
    }
    private val additionalArgsField = JTextField()
    private val environmentVariablesField = JTextField()

    private var myPanel: JPanel? = null

    override fun resetEditorFrom(configuration: CucumberRustRunConfiguration) {
        featureFileField.text = configuration.featureFilePath ?: ""
        testTargetField.text = configuration.testTarget ?: ""
        workingDirectoryField.text = configuration.workingDirectory ?: (project.basePath ?: "")
        additionalArgsField.text = configuration.additionalArgs ?: ""
        environmentVariablesField.text = configuration.environmentVariables ?: ""
    }

    override fun applyEditorTo(configuration: CucumberRustRunConfiguration) {
        configuration.featureFilePath = featureFileField.text
        configuration.testTarget = testTargetField.text
        configuration.workingDirectory = workingDirectoryField.text
        configuration.additionalArgs = additionalArgsField.text
        configuration.environmentVariables = environmentVariablesField.text
    }

    override fun createEditor(): JComponent {
        myPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Feature file:", featureFileField)
            .addLabeledComponent("Test target:", testTargetField)
            .addLabeledComponent("Working directory:", workingDirectoryField)
            .addLabeledComponent("Additional arguments:", additionalArgsField)
            .addLabeledComponent("Environment variables:", environmentVariablesField)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        return myPanel!!
    }
}
