package com.github.pkohlmann1.eOptPlugin.inspections.codeOptimizations

import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiDeclarationStatement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable

class TypeOptimizationInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitDeclarationStatement(declaration: PsiDeclarationStatement) {
                val variables = declaration.declaredElements.filterIsInstance<PsiVariable>()
                for (variable in variables) {
                    if (isOptimizableType(variable.type)) {
                        val message = "Consider using a more energy-efficient type (Int or Long) for variable '${variable.name}'"
                        holder.registerProblem(variable, message, ProblemHighlightType.WEAK_WARNING)
                    }
                }
            }
        }
    }

    private fun isOptimizableType(type: PsiType): Boolean {
        return when (type) {
            PsiType.SHORT, PsiType.FLOAT, PsiType.DOUBLE, PsiType.CHAR, PsiType.BYTE -> true
            else -> false
        }
    }

    override fun getDisplayName(): String {
        return "Optimize Variable Types"
    }

    override fun getDefaultLevel(): HighlightDisplayLevel {
        return HighlightDisplayLevel.WEAK_WARNING
    }
}
