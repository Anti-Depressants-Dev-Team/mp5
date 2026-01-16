package org.antidepressants.mp5.util

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Save text content to a file using a save dialog.
 * Returns the file path if saved successfully, null otherwise.
 */
actual suspend fun saveTextToFile(suggestedName: String, content: String): String? {
    return try {
        val chooser = JFileChooser().apply {
            dialogTitle = "Export Playlist"
            selectedFile = File(suggestedName)
            fileFilter = FileNameExtensionFilter("JSON Files", "json")
        }
        
        val result = chooser.showSaveDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            var file = chooser.selectedFile
            // Ensure .json extension
            if (!file.name.endsWith(".json")) {
                file = File(file.absolutePath + ".json")
            }
            file.writeText(content)
            println("[FileIO] Exported to: ${file.absolutePath}")
            file.absolutePath
        } else {
            null
        }
    } catch (e: Exception) {
        println("[FileIO] Export error: ${e.message}")
        null
    }
}

/**
 * Load text content from a file using an open dialog.
 * Returns the file content if loaded successfully, null otherwise.
 */
actual suspend fun loadTextFromFile(): String? {
    return try {
        val chooser = JFileChooser().apply {
            dialogTitle = "Import Playlist"
            fileFilter = FileNameExtensionFilter("JSON Files", "json")
        }
        
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val content = chooser.selectedFile.readText()
            println("[FileIO] Imported from: ${chooser.selectedFile.absolutePath}")
            content
        } else {
            null
        }
    } catch (e: Exception) {
        println("[FileIO] Import error: ${e.message}")
        null
    }
}
