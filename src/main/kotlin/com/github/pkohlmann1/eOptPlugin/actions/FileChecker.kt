package com.github.pkohlmann1.eOptPlugin.actions

import com.github.pkohlmann1.eOptPlugin.detectors.FileDetector
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageConstants
import com.intellij.openapi.ui.Messages

class FileChecker : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val message = "Diese Aktion durchsucht Ihren Code auf Dateien wie Videos, Bilder oder Dokumente"
        val result = Messages.showYesNoDialog(message, "FileChecking", Messages.getQuestionIcon())
        if (result == MessageConstants.YES) {
            // Get the current project
            val project: Project? = event.project
            if (project == null) {
                return
            }
            // Get the current editor and document
            val editor: Editor? = event.getData(CommonDataKeys.EDITOR)
            val document: Document? = editor?.document
            // Search for file references in the document
            val input: String = document?.text ?: ""
            val fD = FileDetector()
            val fileReferences: List<Triple<String, Int, Int>> = fD.detectFiles(input)
            // Show the search results in a dialog
            if (fileReferences.isNotEmpty()) {
                val sb = StringBuilder()
                sb.append("File references found in the code:\n")
                for ((fileType, line, column) in fileReferences) {
                    sb.append("$fileType at line position $line at column $column\n")
                }
                Messages.showInfoMessage(project, sb.toString(), "Search Results")
            } else {
                Messages.showInfoMessage(project, "No file references found in the code.", "Search Results")
            }

        } else if (result == MessageConstants.NO) {
            Messages.showMessageDialog(
                event.project,
                "No optimization has been done",
                "No Changes",
                Messages.getInformationIcon()
            )
        }
    }
}
