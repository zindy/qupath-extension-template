import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/**
 * Gradle task to create a new QuPath extension from the template.
 * 
 * Usage:
 *   ./gradlew createExtension -PextensionName=MyExtension
 * 
 * Or run without parameters to be prompted:
 *   ./gradlew createExtension
 */

tasks.register("createExtension") {
    group = "QuPath"
    description = "Create a new QuPath extension from this template"
    
    doLast {
        // Get extension name from property or prompt
        val extensionName = if (project.hasProperty("extensionName")) {
            project.property("extensionName").toString()
        } else {
            print("Enter extension name (e.g., MyExtension): ")
            System.`in`.bufferedReader().readLine()?.trim() ?: "MyExtension"
        }
        
        // Validate extension name
        if (!extensionName.matches(Regex("^[A-Z][a-zA-Z0-9]*$"))) {
            throw GradleException("Extension name must start with uppercase letter and contain only letters/numbers")
        }
        
        // Calculate derived names
        val extensionNameLower = extensionName.lowercase()
        val packageName = "qupath.ext.$extensionNameLower"
        val artifactId = "qupath-extension-$extensionNameLower"
        val moduleId = "io.github.qupath.extension.$extensionNameLower"
        
        println("\nCreating extension with:")
        println("  Extension name: $extensionName")
        println("  Lowercase name: $extensionNameLower")
        println("  Package name:   $packageName")
        println("  Artifact ID:    $artifactId")
        println("  Module ID:      $moduleId")
        println()
        
        // Confirm
        if (!project.hasProperty("extensionName")) {
            print("Continue? (y/n): ")
            val confirm = System.`in`.bufferedReader().readLine()?.trim()?.lowercase()
            if (confirm != "y" && confirm != "yes") {
                println("Cancelled.")
                return@doLast
            }
        }
        
        // Define target directory
        val targetDir = project.rootDir.parentFile.resolve(artifactId)
        
        if (targetDir.exists()) {
            throw GradleException("Target directory already exists: $targetDir")
        }
        
        println("Creating extension at: $targetDir")
        
        // Copy entire project structure
        project.copy {
            from(project.rootDir)
            into(targetDir)
            exclude("build", ".gradle", ".git", ".idea", "*.iml")
        }
        
        // Process all files in the target directory
        Files.walkFileTree(targetDir.toPath(), object : SimpleFileVisitor<java.nio.file.Path>() {
            override fun visitFile(file: java.nio.file.Path, attrs: BasicFileAttributes): FileVisitResult {
                val fileName = file.fileName.toString()
                
                // Skip binary and special files
                if (fileName.endsWith(".jar") || 
                    fileName.endsWith(".class") ||
                    fileName.endsWith(".png") ||
                    fileName.endsWith(".jpg") ||
                    fileName.endsWith(".gif") ||
                    fileName.endsWith(".md") ||
                    fileName == "gradlew" ||
                    fileName == "gradlew.bat" ||
                    file.toString().contains("gradle/wrapper/gradle-wrapper.jar")) {
                    return FileVisitResult.CONTINUE
                }

                // Skip the creation code and its README
                if (fileName.lowercase().contains("create-extension")) {
                    return FileVisitResult.CONTINUE
                }
                
                // Read and replace content
                try {
                    var content = String(Files.readAllBytes(file), Charsets.UTF_8)
                    var modified = false
                    
                    // Replace template patterns
                    val replacements = mapOf(
                        "qupath-extension-template" to artifactId,
                        "io.github.qupath.extension.template" to moduleId,
                        "qupath.ext.template" to packageName,
                        "DemoExtension" to "${extensionName}Extension",
                        "DemoGroovyExtension" to "${extensionName}GroovyExtension",
                        "Demo" to extensionName,
                        "Template" to extensionName,
                        "template" to extensionNameLower
                    )
                    
                    replacements.forEach { (oldStr, newStr) ->
                        if (content.contains(oldStr)) {
                            content = content.replace(oldStr, newStr)
                            modified = true
                        }
                    }

                    // Remove template-specific lines from build.gradle.kts
                    if (fileName == "build.gradle.kts") {
                        val lines = content.lines().toMutableList()
                        val filteredLines = mutableListOf<String>()

                        for (i in lines.indices) {
                            val line = lines[i]
                            // Skip lines containing "create-extension"
                            if (!line.contains("create-extension")) {
                                filteredLines.add(line)
                            }
                        }
                        content = filteredLines.joinToString("\n")
                    }                    

                    if (modified) {
                        Files.write(file, content.toByteArray(Charsets.UTF_8))
                        println("  Updated: ${targetDir.toPath().relativize(file)}")
                    }
                } catch (e: Exception) {
                    // Skip files that can't be read as text
                }
                
                // Rename files - check most specific patterns first
                if (fileName.contains("DemoExtension") && !fileName.contains("Groovy")) {
                    val newFileName = fileName.replace("DemoExtension", "${extensionName}Extension")
                    val newFile = file.parent.resolve(newFileName)
                    Files.move(file, newFile)
                    println("  Renamed: ${targetDir.toPath().relativize(file)} -> $newFileName")
                    return FileVisitResult.CONTINUE
                } else if (fileName.contains("DemoGroovyExtension")) {
                    val newFileName = fileName.replace("DemoGroovyExtension", "${extensionName}GroovyExtension")
                    val newFile = file.parent.resolve(newFileName)
                    Files.move(file, newFile)
                    println("  Renamed: ${targetDir.toPath().relativize(file)} -> $newFileName")
                    return FileVisitResult.CONTINUE
                } else if (fileName.contains("Template")) {
                    val newFileName = fileName.replace("Template", extensionName)
                    val newFile = file.parent.resolve(newFileName)
                    Files.move(file, newFile)
                    println("  Renamed: ${targetDir.toPath().relativize(file)} -> $newFileName")
                    return FileVisitResult.CONTINUE
                } else if (fileName.contains("template") && !fileName.contains("Template")) {
                    val newFileName = fileName.replace("template", extensionNameLower)
                    val newFile = file.parent.resolve(newFileName)
                    Files.move(file, newFile)
                    println("  Renamed: ${targetDir.toPath().relativize(file)} -> $newFileName")
                    return FileVisitResult.CONTINUE
                }
                
                return FileVisitResult.CONTINUE
            }
            
            override fun postVisitDirectory(dir: java.nio.file.Path, exc: java.io.IOException?): FileVisitResult {
                val dirName = dir.fileName.toString()
                
                // Rename directories containing "template"
                if (dirName == "template") {
                    val newDir = dir.parent.resolve(extensionNameLower)
                    Files.move(dir, newDir)
                    println("  Renamed dir: ${targetDir.toPath().relativize(dir)} -> $extensionNameLower")
                }
                
                return FileVisitResult.CONTINUE
            }
        })
        
        println("\n✓ Extension created successfully at: $targetDir")
        println("\nNext steps:")
        println("  1. cd $artifactId")
        println("  2. ./gradlew build")
        println("  3. Drag build/libs/$artifactId-*.jar onto QuPath")
        println()
    }
}