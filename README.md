# Cucumber for Rust - IntelliJ Plugin

A JetBrains IntelliJ/RustRover plugin that provides Cucumber BDD support for Rust projects using the [cucumber-rs](https://github.com/cucumber-rs/cucumber) crate.

## Features

This plugin provides the same core BDD features as the official Cucumber.js plugin, adapted for Rust:

### Step Definition Navigation
- **Go to Definition**: Click on a step in a `.feature` file to navigate directly to its Rust implementation
- **Find Usages**: From a Rust step definition, find all `.feature` file steps that reference it

### Step Definition Detection
- Automatically detects step definitions from `#[given(...)]`, `#[when(...)]`, `#[then(...)]` attribute macros
- Supports both **Cucumber Expressions** (`expr = "..."`) and **regex patterns** (`regex = r"..."`)
- Supports simple string patterns (`#[given("...")]`)

### Quick-Fix Step Creation
- When a step in a `.feature` file has no matching definition, a quick-fix is offered to create one
- Generates properly formatted async Rust functions with the appropriate cucumber-rs attribute macro
- Automatically infers parameter types from Cucumber Expression placeholders

### Run Configuration
- **Run Configuration Type**: "Cucumber Rust" run configuration for executing cucumber-rs tests via `cargo test`
- **Gutter Run Icons**: Click the green play icon next to Feature/Scenario lines to run tests
- **Context Menu**: Right-click on `.feature` files to run them
- **Auto-detection**: Automatically finds the appropriate test target from your `Cargo.toml`

### Gherkin Support
- Full Gherkin syntax highlighting (via the Gherkin plugin dependency)
- Gherkin v6 `Rule` keyword support
- Error highlighting for unresolved steps

## Requirements

- **IntelliJ IDEA 2024.2+** or **RustRover 2024.2+**
- **Gherkin plugin** (installed automatically as a dependency)
- **Cucumber for Java plugin** (installed automatically as a dependency, provides the extension point framework)

## Installation

1. Open your JetBrains IDE (IntelliJ IDEA or RustRover)
2. Go to **Settings/Preferences** → **Plugins** → **Marketplace**
3. Search for "Cucumber for Rust"
4. Click **Install**

Or install from disk:
1. Build the plugin: `./gradlew buildPlugin`
2. The plugin ZIP will be in `build/distributions/`
3. Go to **Settings/Preferences** → **Plugins** → **⚙️** → **Install Plugin from Disk...**

## Project Structure Expected

The plugin expects a standard cucumber-rs project layout:

```
my-project/
├── Cargo.toml
├── src/
│   └── lib.rs
├── tests/
│   ├── cucumber.rs          # Test binary with World + step definitions
│   ├── features/
│   │   ├── eating.feature    # Gherkin feature files
│   │   └── drinking.feature
│   └── steps/                # Optional: separate step definition files
│       ├── eating_steps.rs
│       └── drinking_steps.rs
```

### Example Step Definitions

```rust
use cucumber::{given, when, then, World as _};

#[derive(Debug, Default, cucumber::World)]
struct World {
    user: Option<String>,
    capacity: usize,
}

#[given(expr = "{word} is hungry")]
async fn someone_is_hungry(w: &mut World, user: String) {
    w.user = Some(user);
}

#[when(regex = r"^(?:he|she|they) eats? (\d+) cucumbers?$")]
async fn eat_cucumbers(w: &mut World, count: usize) {
    w.capacity += count;
    assert!(w.capacity < 4, "{} exploded!", w.user.as_ref().unwrap());
}

#[then("she is full")]
async fn is_full(w: &mut World) {
    assert_eq!(w.capacity, 3, "{} isn't full!", w.user.as_ref().unwrap());
}
```

## Building from Source

```bash
# Build the plugin
./gradlew buildPlugin

# Run a sandboxed IDE with the plugin installed
./gradlew runIde

# Run tests
./gradlew test

# Verify plugin compatibility
./gradlew verifyPlugin
```

## Architecture

The plugin implements the `CucumberJvmExtensionPoint` interface from the JetBrains Cucumber plugin:

- **`RustCucumberExtension`**: Main extension point — tells the framework how to find and parse Rust step definitions
- **`RustStepDefinition`**: Represents a single parsed step definition with its pattern and navigation element
- **`RustStepDefinitionCreator`**: Creates new step definition files and functions
- **`RustStepDefinitionUtil`**: Parses Rust source files to extract `#[given/when/then]` attributes
- **`CucumberRustRunConfiguration`**: Run configuration that executes `cargo test --test <target>`
- **`CucumberRustRunLineMarkerContributor`**: Adds run gutter icons to Feature/Scenario lines

## License

Apache-2.0 OR MIT
