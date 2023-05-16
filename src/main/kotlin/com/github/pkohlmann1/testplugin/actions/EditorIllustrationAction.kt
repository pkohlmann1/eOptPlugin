package com.github.pkohlmann1.testplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project


class EditorIllustrationAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project
        val editor: Editor? = e.getData(CommonDataKeys.EDITOR)

        val primaryCaret: Caret? = editor?.caretModel?.primaryCaret
        val start = primaryCaret?.selectionStart
        val end = primaryCaret?.selectionEnd

        WriteCommandAction.runWriteCommandAction(project) {
            if (start != null) {
                if (end != null) {
                    editor.document.replaceString(start, end, "This is green code since it does nothing!")
                }
            }
        }

        primaryCaret?.removeSelection();
    }
}
