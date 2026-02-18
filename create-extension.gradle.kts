import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.StandardCopyOption

/**
 * Gradle task to create a new QuPath extension from the template.
 *
 * Usage:
 *   ./gradlew createExtension -PextensionName=MyExtension
 *   ./gradlew createExtension -PextensionName=MyExtension -Planguage=groovy
 *   ./gradlew createExtension -PextensionName=MyExtension -Planguage=both
 *
 * -Planguage accepts: java (default), groovy, both
 *
 * Or run without parameters to be prompted interactively:
 *   ./gradlew createExtension
 */

tasks.register("createExtension") {
    group = "QuPath"
    description = "Create a new QuPath extension from this template"

    doLast {
        // ── Extension name ────────────────────────────────────────────────────
        val extensionName = if (project.hasProperty("extensionName")) {
            project.property("extensionName").toString()
        } else {
            print("Enter extension name (e.g., MyExtension): ")
            System.`in`.bufferedReader().readLine()?.trim() ?: "MyExtension"
        }

        if (!extensionName.matches(Regex("^[A-Z][a-zA-Z0-9]*$"))) {
            throw GradleException("Extension name must start with uppercase letter and contain only letters/numbers")
        }

        // ── Language selection ────────────────────────────────────────────────
        val validLanguages = setOf("java", "groovy", "both")

        val language = if (project.hasProperty("language")) {
            val lang = project.property("language").toString().lowercase()
            if (lang !in validLanguages)
                throw GradleException("Invalid language '$lang'. Must be one of: ${validLanguages.joinToString()}")
            lang
        } else if (project.hasProperty("extensionName")) {
            // When extensionName is provided via CLI but language isn't, default to java
            "java"
        } else {
            // Interactive mode - prompt for language
            print("Language [java/groovy/both] (default: java): ")
            val input = System.`in`.bufferedReader().readLine()?.trim()?.lowercase()
            if (input.isNullOrBlank()) "java"
            else if (input in validLanguages) input
            else throw GradleException("Invalid language '$input'. Must be one of: ${validLanguages.joinToString()}")
        }

        val includeJava   = language == "java"   || language == "both"
        val includeGroovy = language == "groovy"  || language == "both"

        // ── Derived names ─────────────────────────────────────────────────────
        // Convert CamelCase to kebab-case for filenames (ProjectMetadataEditor -> project-metadata-editor)
        val extensionNameKebab = extensionName
            .replace(Regex("([a-z0-9])([A-Z])"), "$1-$2")
            .lowercase()
        
        // Convert to simple lowercase for package names (ProjectMetadataEditor -> projectmetadataeditor)
        val extensionNameLower = extensionName.lowercase()
        
        val packageName        = "qupath.ext.$extensionNameLower"
        val artifactId         = "qupath-extension-$extensionNameKebab"
        val moduleId           = "io.github.qupath.extension.$extensionNameKebab"

        println("\nCreating extension with:")
        println("  Extension name: $extensionName")
        println("  Language:       $language")
        println("  Kebab-case:     $extensionNameKebab (for filenames)")
        println("  Lowercase:      $extensionNameLower (for packages)")
        println("  Package name:   $packageName")
        println("  Artifact ID:    $artifactId")
        println("  Module ID:      $moduleId")
        println()

        // ── Interactive confirmation (only when no CLI properties were given) ─
        val interactive = !project.hasProperty("extensionName") && !project.hasProperty("language")
        if (interactive) {
            print("Continue? (y/n): ")
            val confirm = System.`in`.bufferedReader().readLine()?.trim()?.lowercase()
            if (confirm != "y" && confirm != "yes") {
                println("Cancelled.")
                return@doLast
            }
        }

        // ── Target directory ──────────────────────────────────────────────────
        val targetDir = project.rootDir.parentFile.resolve(artifactId)
        if (targetDir.exists())
            throw GradleException("Target directory already exists: $targetDir")

        println("Creating extension at: $targetDir")

        // ── Copy entire project structure ─────────────────────────────────────
        project.copy {
            from(project.rootDir)
            into(targetDir)
            exclude("build", ".gradle", ".git", ".idea", "*.iml")
        }

        // ── Helper: is this file a Groovy source file? ────────────────────────
        fun isGroovyFile(file: java.nio.file.Path): Boolean {
            val name = file.fileName.toString()
            return name.endsWith(".groovy") || name.contains("Groovy")
        }

        // ── Helper: is this file a Java source file? ──────────────────────────
        fun isJavaSourceFile(file: java.nio.file.Path): Boolean {
            val name = file.fileName.toString()
            return name.endsWith(".java") || (name.contains("DemoExtension") && !name.contains("Groovy"))
        }

        // Collect all operations to perform AFTER the walk
        val filesToDelete = mutableListOf<java.nio.file.Path>()

        // ── Walk the tree (collect operations, don't execute deletions/renames yet) ──
        Files.walkFileTree(targetDir.toPath(), object : SimpleFileVisitor<java.nio.file.Path>() {

            override fun visitFile(file: java.nio.file.Path, attrs: BasicFileAttributes): FileVisitResult {
                val fileName = file.fileName.toString()

                // Always skip binary / non-text assets
                if (fileName.endsWith(".jar")   ||
                    fileName.endsWith(".class") ||
                    fileName.endsWith(".png")   ||
                    fileName.endsWith(".jpg")   ||
                    fileName.endsWith(".gif")   ||
                    fileName.endsWith(".md")    ||
                    fileName == "gradlew"       ||
                    fileName == "gradlew.bat"   ||
                    file.toString().contains("gradle/wrapper/gradle-wrapper.jar")) {
                    return FileVisitResult.CONTINUE
                }

                // Always skip the scaffolding script itself
                if (fileName.lowercase().contains("create-extension")) {
                    return FileVisitResult.CONTINUE
                }

                // ── Language-based pruning (mark for deletion) ────────────────
                if (isGroovyFile(file) && !includeGroovy) {
                    filesToDelete.add(file)
                    return FileVisitResult.CONTINUE
                }

                if (isJavaSourceFile(file) && !includeJava) {
                    filesToDelete.add(file)
                    return FileVisitResult.CONTINUE
                }

                // ── Text replacement ──────────────────────────────────────────
                try {
                    var content = String(Files.readAllBytes(file), Charsets.UTF_8)
                    var modified = false

                    val replacements = mapOf(
                        "qupath-extension-template"          to artifactId,
                        "io.github.qupath.extension.template" to moduleId,
                        "qupath.ext.template"                to packageName,
                        "DemoExtension"                      to "${extensionName}Extension",
                        "DemoGroovyExtension"                to "${extensionName}GroovyExtension",
                        "Demo"                               to extensionName,
                        "Template"                           to extensionName,
                        "template"                           to extensionNameLower
                    )

                    replacements.forEach { (old, new) ->
                        if (content.contains(old)) {
                            content = content.replace(old, new)
                            modified = true
                        }
                    }

                    // ── build.gradle.kts patching ─────────────────────────────
                    if (fileName == "build.gradle.kts") {
                        val lines = content.lines().toMutableList()
                        val filtered = mutableListOf<String>()
                        var i = 0

                        while (i < lines.size) {
                            val line = lines[i]

                            // Always drop the create-extension apply line
                            if (line.contains("create-extension")) {
                                i++
                                continue
                            }

                            // Drop the `groovy` plugin line when Groovy is not wanted
                            if (!includeGroovy && line.trim() == "groovy") {
                                // Also drop the comment line immediately above it, if present
                                if (filtered.isNotEmpty() && filtered.last().trim().startsWith("//")) {
                                    filtered.removeAt(filtered.lastIndex)
                                }
                                i++
                                continue
                            }

                            // Drop Groovy dependency blocks when Groovy is not wanted
                            if (!includeGroovy &&
                                (line.contains("bundles.groovy") || line.contains("groovy", ignoreCase = true))) {
                                i++
                                continue
                            }

                            filtered.add(line)
                            i++
                        }

                        content = filtered.joinToString("\n")
                        modified = true
                    }

                    if (modified) {
                        Files.write(file, content.toByteArray(Charsets.UTF_8))
                        println("  Updated: ${targetDir.toPath().relativize(file)}")
                    }
                } catch (e: Exception) {
                    // Skip files that cannot be read as UTF-8 text
                }

                // ── File renames (execute immediately - files can be renamed during walk) ──
                val currentName = file.fileName.toString()
                
                // Determine new filename
                val newFileName = when {
                    currentName.contains("DemoExtension") && !currentName.contains("Groovy") -> 
                        currentName.replace("DemoExtension", "${extensionName}Extension")
                    currentName.contains("DemoGroovyExtension") -> 
                        currentName.replace("DemoGroovyExtension", "${extensionName}GroovyExtension")
                    currentName.contains("Template") -> 
                        currentName.replace("Template", extensionName)
                    currentName.contains("template") && !currentName.contains("Template") -> 
                        currentName.replace("template", extensionNameLower)
                    else -> currentName  // Keep the same filename
                }
                
                // Check if the file's path contains "template" directory that needs renaming
                val pathString = file.toString()
                val templateSep = File.separator + "template" + File.separator
                
                if (pathString.contains(templateSep)) {
                    // Replace all occurrences of "/template/" with "/extensionNameLower/"
                    val newPathString = pathString.replace(templateSep, File.separator + extensionNameLower + File.separator)
                    val newParentPath = newPathString.substring(0, newPathString.lastIndexOf(File.separator))
                    val newPath = java.nio.file.Paths.get(newParentPath, newFileName)
                    
                    // Create parent directories if they don't exist
                    Files.createDirectories(newPath.parent)
                    
                    Files.move(file, newPath, StandardCopyOption.REPLACE_EXISTING)
                    println("  Renamed: ${targetDir.toPath().relativize(file)} -> ${targetDir.toPath().relativize(newPath)}")
                } else if (newFileName != currentName) {
                    // File needs renaming but not in a template directory
                    val newPath = file.parent.resolve(newFileName)
                    Files.move(file, newPath, StandardCopyOption.REPLACE_EXISTING)
                    println("  Renamed: ${targetDir.toPath().relativize(file)} -> $newFileName")
                }
                
                return FileVisitResult.CONTINUE
            }
        })

        // Force garbage collection to release file handles (Windows workaround)
        System.gc()
        Thread.sleep(100)

        // ── Execute deletions (files first, then directories) ────────────────
        filesToDelete.forEach { file ->
            try {
                Files.delete(file)
                println("  Deleted (not ${if (includeJava) "Groovy" else "Java"} project): ${targetDir.toPath().relativize(file)}")
            } catch (e: Exception) {
                println("  WARNING: Could not delete ${targetDir.toPath().relativize(file)}: ${e.message}")
            }
        }

        // ── Clean up ALL empty directories ────────────────────────────────────
        // Multiple passes to clean up parent directories that become empty
        var removedAny = true
        var passCount = 0
        while (removedAny && passCount < 10) {
            removedAny = false
            passCount++
            
            Files.walkFileTree(targetDir.toPath(), object : SimpleFileVisitor<java.nio.file.Path>() {
                override fun postVisitDirectory(dir: java.nio.file.Path, exc: java.io.IOException?): FileVisitResult {
                    try {
                        if (dir != targetDir.toPath() && Files.list(dir).use { it.count() } == 0L) {
                            Files.delete(dir)
                            println("  Removed empty dir: ${targetDir.toPath().relativize(dir)}")
                            removedAny = true
                        }
                    } catch (_: Exception) {}
                    return FileVisitResult.CONTINUE
                }
            })
        }

        println("\n✓ Extension created successfully at: $targetDir")
        println("\nNext steps:")
        println("  1. cd $artifactId")
        println("  2. ./gradlew build")
        println("  3. Drag build/libs/$artifactId-*.jar onto QuPath")
        println()
    }
}
