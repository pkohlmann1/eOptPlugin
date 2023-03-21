package com.github.pkohlmann1.testplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.Messages

class About: AnAction() {

    override fun actionPerformed(e: AnActionEvent) {

        Messages.showMessageDialog(e.project, "This is the Energy-Efficiency-Assistant", "About", Messages.getInformationIcon())
//        val fileChooserDescriptor = FileChooserDescriptor(false, true, false, false, false, false)
//        fileChooserDescriptor.title = "MyTestPlugin Pick Directory"
//        fileChooserDescriptor.description = "MyTestPlugin demo"
//
//        FileChooser.chooseFile(fileChooserDescriptor, e.project, null, {
//            Messages.showMessageDialog(e.project, it.path, "Path", Messages.getInformationIcon())
//        })
    }
}