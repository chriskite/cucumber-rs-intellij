package com.chriskite.cucumber_rs.intellij

/**
 * Constants and utilities for the Cucumber for Rust plugin.
 */
object CucumberRustBundle {
    const val PLUGIN_ID = "com.chriskite.cucumber-rs.intellij"
    const val PLUGIN_NAME = "Cucumber for Rust"

    /** The cucumber-rs attribute macros that define step definitions. */
    val STEP_DEFINITION_MACROS = setOf("given", "when", "then")

    /** File extensions for Rust source files. */
    const val RUST_FILE_EXTENSION = "rs"

    /** Default step definition directory relative to project root. */
    const val DEFAULT_STEP_DEF_DIR = "tests"

    /** Regex pattern to match cucumber-rs step definition attributes.
     *  Matches patterns like:
     *  - #[given("pattern")]
     *  - #[when(expr = "pattern")]
     *  - #[then(regex = r"pattern")]
     */
    val STEP_ATTR_PATTERN = Regex(
        """#\[(given|when|then)\s*\(\s*(?:(?:expr|regex)\s*=\s*)?(?:r#?")?(.*?)(?:"#?)?\s*\)\s*\]"""
    )

    /** Regex pattern to match a function declaration following a step attribute. */
    val FUNCTION_PATTERN = Regex(
        """(?:pub\s+)?(?:async\s+)?fn\s+(\w+)"""
    )
}
