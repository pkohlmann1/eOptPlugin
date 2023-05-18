package com.github.pkohlmann1.testplugin.inspections.generalRules

import com.github.pkohlmann1.testplugin.InspectionBundle
import com.github.pkohlmann1.testplugin.detectors.FileDetector
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLiteralExpression


class FileUsageInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun getDisplayName(): String {
        return "File Usage Inspection"
    }


    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitLiteralExpression(expression: PsiLiteralExpression?) {
                if (expression != null && expression.value is String) {
                    val fileReferences = FileDetector().detectFiles(expression.value as String)
                    if (fileReferences.isNotEmpty()) {
                        for (reference in fileReferences) {

                            holder.registerProblem(
                                expression, InspectionBundle.message("inspection.detecting.file.usage.problem.descriptor") + " File reference found: ${reference.first}"
                            )
                        }
                    }
                }
            }
        }
    }
}
