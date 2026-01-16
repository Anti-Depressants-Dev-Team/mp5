package org.antidepressants.mp5.util

/**
 * Save text content to a file using a save dialog.
 * Returns the file path if saved successfully, null otherwise.
 */
expect suspend fun saveTextToFile(suggestedName: String, content: String): String?

/**
 * Load text content from a file using an open dialog.
 * Returns the file content if loaded successfully, null otherwise.
 */
expect suspend fun loadTextFromFile(): String?
