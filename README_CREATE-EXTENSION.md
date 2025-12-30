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

You'll be prompted to enter your extension name (e.g., `MyAwesome`), and the task will:
- Show you what will be created
- Ask for confirmation
- Generate the complete extension structure

### Option 2: Command-line parameter (Faster for repeat use)

```bash
./gradlew createExtension -PextensionName=MyAwesome
```

This skips the prompts and creates the extension immediately.

## What it does

The task automatically:

1. **Creates a new directory** named `qupath-extension-myawesome` (lowercase) next to your template
2. **Replaces all occurrences** of:
   - `Template` → `MyAwesome`
   - `template` → `myawesome`
   - `qupath-extension-template` → `qupath-extension-myawesome`
   - `qupath.ext.template` → `qupath.ext.myawesome`
   - `io.github.qupath.extension.template` → `io.github.qupath.extension.myawesome`

3. **Renames files and folders**:
   - `DemoExtension.java` → `DemoMyAwesomeExtension.java` (if using Java example)
   - `src/main/java/qupath/ext/template/` → `src/main/java/qupath/ext/myawesome/`
   - Updates `build.gradle.kts`, `settings.gradle.kts`, and META-INF services file

4. **Keeps all build files intact** - gradle wrapper, licenses, workflows, etc.

## Example

```bash
$ ./gradlew createExtension -PextensionName=CellClassifier

Creating extension with:
  Extension name: CellClassifier
  Lowercase name: cellclassifier
  Package name:   qupath.ext.cellclassifier
  Artifact ID:    qupath-extension-cellclassifier
  Module ID:      io.github.qupath.extension.cellclassifier

Creating extension at: /path/to/qupath-extension-cellclassifier
  Updated: build.gradle.kts
  Updated: settings.gradle.kts
  Updated: src/main/java/qupath/ext/template/DemoExtension.java
  Renamed: DemoExtension.java -> DemoCellClassifierExtension.java
  Renamed dir: src/main/java/qupath/ext/template -> cellclassifier
  ...

✓ Extension created successfully!

Next steps:
  1. cd qupath-extension-cellclassifier
  2. ./gradlew build
  3. Drag build/libs/qupath-extension-cellclassifier-*.jar onto QuPath
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

**Build fails after generation**
- Make sure you're using Java 21
- Run `./gradlew clean build` in the new extension directory

## Tips

- Keep the template directory clean - this becomes the starting point for all extensions
- Delete the example Groovy extension if you're only using Java (or vice versa)
- Update the `version` in your template's `build.gradle.kts` before generating new extensions
- Don't modify files in the template directory while testing - clone it first!

## No More Manual Renaming! 🎉

You'll never have to spend hours finding and replacing "Template" and "template" again. Just run one command and start coding!