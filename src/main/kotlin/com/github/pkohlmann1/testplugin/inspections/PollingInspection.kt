package com.github.pkohlmann1.testplugin.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

class PollingInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitWhileStatement(statement: PsiWhileStatement) {
                super.visitWhileStatement(statement)
                findSleepMethodInStatement(statement, holder)
            }

            override fun visitForStatement(statement: PsiForStatement) {
                super.visitForStatement(statement)
                findSleepMethodInStatement(statement, holder)
            }

            override fun visitForeachStatement(statement: PsiForeachStatement) {
                super.visitForeachStatement(statement)
                findSleepMethodInStatement(statement, holder)
            }

            override fun visitDoWhileStatement(statement: PsiDoWhileStatement) {
                super.visitDoWhileStatement(statement)
                findSleepMethodInStatement(statement, holder)
            }

            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)

                val methodName = expression.methodExpression.referenceName
                if (methodName != null) {
                    if (methodName.startsWith("schedule")) {
                            val arguments = expression.argumentList.expressions
                            if (arguments.size >= 2) {
                                val arg1Type = arguments[0].getType()
                                if (arg1Type != null) {
                                    if (arg1Type.canonicalText == "java.util.TimerTask") {
                                        holder.registerProblem(
                                            expression,
                                            "Code uses Timer.schedule method, which executes code in regular intervals",
                                            ProblemHighlightType.WARNING
                                        )
                                    }
                                }
                            }
                        }

                }
            }
        }
    }


    private fun findSleepMethodInStatement(statement: PsiLoopStatement, holder: ProblemsHolder) {
        val body = statement.body
        if (body != null) {
            val methodCalls = PsiTreeUtil.findChildrenOfType(body, PsiMethodCallExpression::class.java)
            for (methodCall in methodCalls) {
                val methodExpression = methodCall.methodExpression
                val methodName = methodExpression.referenceName
                if (methodName == "sleep") {
                    holder.registerProblem(
                        methodCall,
                        "Consider using event-driven or asynchronous approach instead of polling",
                        ProblemHighlightType.WARNING
                    )
                }
            }
        }
    }
}
