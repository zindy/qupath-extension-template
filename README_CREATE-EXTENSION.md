# QuPath Extension Generator

This Gradle task makes it super easy to create new QuPath extensions from the template without manually renaming everything!

## Setup (One-time)

1. Add the `create-extension.gradle.kts` file to your `qupath-extension-template` directory
2. Add this line to your `build.gradle.kts`:

```kotlin
apply(from = "create-extension.gradle.kts")
```

That's it! You're ready to create extensions.

## Usage

### Option 1: Interactive (Recommended for first time)

```bash
./gradlew createExtension
```

You'll be prompted to enter:
- Your extension name (e.g., `MyAwesome`)
- Language preference (`java`, `groovy`, or `both`) - defaults to `java`

The task will then:
- Show you what will be created
- Ask for confirmation
- Generate the complete extension structure

### Option 2: Command-line parameters (Faster for repeat use)

```bash
# Java-only extension (default)
./gradlew createExtension -PextensionName=MyAwesome

# Groovy-only extension
./gradlew createExtension -PextensionName=MyAwesome -Planguage=groovy

# Extension with both Java and Groovy support
./gradlew createExtension -PextensionName=MyAwesome -Planguage=both
```

This skips the prompts and creates the extension immediately.

## Language Options

The `-Planguage` flag controls which language support is included:

- **`java`** (default) - Java-only extension
  - Includes Java source files
  - Removes Groovy plugin and dependencies from `build.gradle.kts`
  - Deletes Groovy source files and empty directories
  
- **`groovy`** - Groovy-only extension
  - Includes Groovy source files and plugin
  - Removes Java source files (keeps Groovy plugin needed for compilation)
  
- **`both`** - Dual-language extension
  - Includes both Java and Groovy source files
  - Keeps all dependencies and plugins

**Why choose one over the other?**
- **Java**: Simpler, smaller extension if you don't need Groovy's scripting features
- **Groovy**: Use if you want Groovy's dynamic features or scripting support
- **Both**: Maximum flexibility, but larger dependency footprint

## What it does

The task automatically:

1. **Creates a new directory** named with kebab-case (e.g., `qupath-extension-my-awesome` for `MyAwesome`) next to your template

2. **Replaces all occurrences** of:
   - `Template` → `MyAwesome`
   - `template` → `myawesome` (in package names)
   - `template` → `my-awesome` (in artifact/module IDs)
   - `qupath-extension-template` → `qupath-extension-my-awesome`
   - `qupath.ext.template` → `qupath.ext.myawesome`
   - `io.github.qupath.extension.template` → `io.github.qupath.extension.my-awesome`
   - `DemoExtension` → `MyAwesomeExtension`
   - `DemoGroovyExtension` → `MyAwesomeGroovyExtension`

3. **Renames files and folders**:
   - `DemoExtension.java` → `MyAwesomeExtension.java`
   - `DemoGroovyExtension.groovy` → `MyAwesomeGroovyExtension.groovy`
   - `src/main/java/qupath/ext/template/` → `src/main/java/qupath/ext/myawesome/`
   - `src/main/groovy/qupath/ext/template/` → `src/main/groovy/qupath/ext/myawesome/`
   - Updates `build.gradle.kts`, `settings.gradle.kts`, and META-INF services file

4. **Cleans up based on language choice**:
   - Removes unwanted source files (`.java` or `.groovy`)
   - Strips Groovy plugin/dependencies from build file if not needed
   - Deletes all empty directories (including nested ones)

5. **Keeps all build files intact** - gradle wrapper, licenses, workflows, etc.

## Naming Conventions

The script follows QuPath's naming conventions:

- **CamelCase input**: `ProjectMetadataEditor`
- **Kebab-case** for artifacts/modules: `qupath-extension-project-metadata-editor`, `io.github.qupath.extension.project-metadata-editor`
- **Lowercase** for packages: `qupath.ext.projectmetadataeditor`
- **CamelCase** for class names: `ProjectMetadataEditorExtension`

## Examples

### Java-only extension

```bash
$ ./gradlew createExtension -PextensionName=CellClassifier

Creating extension with:
  Extension name: CellClassifier
  Language:       java
  Kebab-case:     cell-classifier (for filenames)
  Lowercase:      cellclassifier (for packages)
  Package name:   qupath.ext.cellclassifier
  Artifact ID:    qupath-extension-cell-classifier
  Module ID:      io.github.qupath.extension.cell-classifier

Creating extension at: /path/to/qupath-extension-cell-classifier
  Updated: build.gradle.kts
  Updated: src/main/java/qupath/ext/template/DemoExtension.java
  Renamed: src/main/java/qupath/ext/template/DemoExtension.java -> src/main/java/qupath/ext/cellclassifier/CellClassifierExtension.java
  Deleted (not Groovy project): src/main/groovy/qupath/ext/template/DemoGroovyExtension.groovy
  Removed empty dir: src/main/groovy/qupath/ext/template
  Removed empty dir: src/main/groovy/qupath/ext
  Removed empty dir: src/main/groovy/qupath
  Removed empty dir: src/main/groovy
  ...

✓ Extension created successfully!

Next steps:
  1. cd qupath-extension-cell-classifier
  2. ./gradlew build
  3. Drag build/libs/qupath-extension-cell-classifier-*.jar onto QuPath
```

### Groovy-only extension

```bash
$ ./gradlew createExtension -PextensionName=ScriptRunner -Planguage=groovy

Creating extension with:
  Extension name: ScriptRunner
  Language:       groovy
  Kebab-case:     script-runner (for filenames)
  Lowercase:      scriptrunner (for packages)
  Package name:   qupath.ext.scriptrunner
  Artifact ID:    qupath-extension-script-runner
  Module ID:      io.github.qupath.extension.script-runner

Creating extension at: /path/to/qupath-extension-script-runner
  Updated: build.gradle.kts
  Updated: src/main/groovy/qupath/ext/template/DemoGroovyExtension.groovy
  Renamed: src/main/groovy/qupath/ext/template/DemoGroovyExtension.groovy -> src/main/groovy/qupath/ext/scriptrunner/ScriptRunnerGroovyExtension.groovy
  Deleted (not Java project): src/main/java/qupath/ext/template/DemoExtension.java
  Removed empty dir: src/main/java/qupath/ext/template
  Removed empty dir: src/main/java/qupath/ext
  Removed empty dir: src/main/java/qupath
  Removed empty dir: src/main/java
  ...

✓ Extension created successfully!
```

### Extension with both languages

```bash
$ ./gradlew createExtension -PextensionName=Analyzer -Planguage=both

Creating extension with:
  Extension name: Analyzer
  Language:       both
  Kebab-case:     analyzer (for filenames)
  Lowercase:      analyzer (for packages)
  Package name:   qupath.ext.analyzer
  Artifact ID:    qupath-extension-analyzer
  Module ID:      io.github.qupath.extension.analyzer

Creating extension at: /path/to/qupath-extension-analyzer
  Updated: build.gradle.kts
  Updated: src/main/java/qupath/ext/template/DemoExtension.java
  Renamed: src/main/java/qupath/ext/template/DemoExtension.java -> src/main/java/qupath/ext/analyzer/AnalyzerExtension.java
  Updated: src/main/groovy/qupath/ext/template/DemoGroovyExtension.groovy
  Renamed: src/main/groovy/qupath/ext/template/DemoGroovyExtension.groovy -> src/main/groovy/qupath/ext/analyzer/AnalyzerGroovyExtension.groovy
  ...

✓ Extension created successfully!
```

## Extension Name Rules

- Must start with an uppercase letter
- Can only contain letters and numbers
- Examples: `MyExtension`, `CellCounter`, `AwesomeTool`
- Invalid: `my-extension`, `123Tool`, `_MyExtension`

## Troubleshooting

**"Target directory already exists"**
- The extension was already created. Delete the target directory or use a different name.

**"Extension name must start with uppercase"**
- Use CamelCase naming, e.g., `MyExtension` not `myextension`

**"Invalid language 'xyz'"**
- Language must be one of: `java`, `groovy`, or `both`
- Example: `-Planguage=java`

**Build fails after generation**
- Make sure you're using Java 21
- Run `./gradlew clean build` in the new extension directory
- For Groovy extensions, ensure the Groovy plugin is properly configured

## Tips

- Keep the template directory clean - this becomes the starting point for all extensions
- Use `java` (default) if you don't need Groovy - it produces a simpler, lighter extension
- Update the `version` in your template's `build.gradle.kts` before generating new extensions
- Don't modify files in the template directory while testing - clone it first!
- If your template has custom source files:
  - Java files should NOT contain "Groovy" in the filename
  - Groovy files should end in `.groovy` or contain "Groovy" in the filename
  - This ensures correct pruning when using language filters

## No More Manual Renaming! 🎉

You'll never have to spend hours finding and replacing "Template" and "template" again. Just run one command and start coding!
