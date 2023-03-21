package com.github.pkohlmann1.testplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.MessageConstants
import com.intellij.openapi.ui.Messages

class Refactor: AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val message = "Diese Aktion optimiert den Energieverbauch Ihrer Anwendung auf Code-Ebene"
        val result = Messages.showYesNoDialog(message, "Refactoring", Messages.getQuestionIcon())
        if (result == MessageConstants.YES) {
            Messages.showMessageDialog(e.project, "Code has been optimized!", "Done", Messages.getInformationIcon())
        } else if (result == MessageConstants.NO) {
            Messages.showMessageDialog(e.project, "No optimization has been done", "No Changes", Messages.getInformationIcon())
        }
    }
}