package com.github.pkohlmann1.testplugin.inspections.codeOptimizations

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiField
import com.intellij.psi.PsiModifier

class StaticVariablesInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {

            override fun visitField(field: PsiField) {
                if (field.hasModifierProperty(PsiModifier.STATIC)) {
                    val description = "Static variable declaration: ${field.name}"
                    val problemDescriptor = holder.manager.createProblemDescriptor(
                        field,
                        description,
                        true,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        false,
                        QuickFix()
                    )
                    holder.registerProblem(problemDescriptor)
                }
            }
        }
    }

    private class QuickFix : LocalQuickFix {

        override fun getName(): String = "Convert to instance variable"

        override fun getFamilyName(): String = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val field = descriptor.psiElement as? PsiField ?: return
            field.modifierList?.setModifierProperty(PsiModifier.STATIC, false)
        }
    }
}
