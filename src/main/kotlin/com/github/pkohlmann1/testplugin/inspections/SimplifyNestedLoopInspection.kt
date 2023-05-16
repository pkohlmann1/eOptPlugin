package com.github.pkohlmann1.testplugin.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil


class SimplifyNestedLoopInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitForStatement(statement: PsiForStatement) {
                super.visitForStatement(statement)
                if (isConstantLoop(statement)){
                    registerProblemConstant(holder, statement)
                }
                // Check if it is a nested loop
                if (isNestedLoop(statement)) {
                    // Perform inspection
                    if (canSimplifyNestedLoop(statement)) {
                        registerProblem(holder, statement)
                    }
                }
            }
        }
    }


    private fun isNestedLoop(statement: PsiForStatement): Boolean {
        val body = statement.body
        if (body is PsiBlockStatement) {
            val codeBlock = body.codeBlock
            val statements = codeBlock.statements
            for (stmt in statements) {
                if (stmt is PsiForStatement) {
                    return true
                }
            }
        }
        return false
    }

    private fun canSimplifyNestedLoop(statement: PsiForStatement): Boolean {
        val body = statement.body
        val initialization = statement.initialization

        // Überprüfen, ob die Iterationsvariable innerhalb des Schleifenkörpers verwendet wird
        if (body != null && initialization is PsiDeclarationStatement) {
            val declaredElements = initialization.declaredElements
            if (declaredElements.size == 1 && declaredElements[0] is PsiLocalVariable) {
                val iterationVariable = declaredElements[0] as PsiLocalVariable
                val variableName = iterationVariable.name

                val references = PsiTreeUtil.findChildrenOfType(body, PsiReferenceExpression::class.java)
                for (reference in references) {
                    if (reference.text == variableName) {
                        // Die Iterationsvariable wird innerhalb des Schleifenkörpers verwendet
                        return false
                    }
                }
            }
        }

        return true
    }

    private fun isConstantLoop(statement: PsiForStatement): Boolean {
        val initialization = statement.initialization
        val condition = statement.condition
        val update = statement.update

        // Überprüfen Sie, ob die Initialisierung, Bedingung und Update-Ausdrücke vorhanden sind
        if (initialization != null && condition != null && update != null) {
            // Überprüfen Sie, ob der Iterationsbereich konstant ist (z. B. i < 10)
            if (isConstantRange(condition)) {
                return true
            }
        }

        return false
    }

    private fun isConstantRange(expression: PsiExpression): Boolean {
        // Überprüfen Sie, ob der Ausdruck eine einfache binäre Vergleichsoperation ist (z. B. i < 10)
        if (expression is PsiBinaryExpression) {
            val operation = expression.operationTokenType
            val leftOperand = expression.lOperand
            val rightOperand = expression.rOperand

            // Überprüfen Sie, ob der Operator '<' ist und die Operanden konstante Werte sind
            if (operation == JavaTokenType.LT && isConstantValue(leftOperand) && isConstantValue(rightOperand)) {
                return true
            }
        }

        return false
    }

    private fun isConstantValue(expression: PsiExpression?): Boolean {
        // Überprüfen Sie, ob der Ausdruck ein konstanter Wert ist (z. B. Literal)
        return expression is PsiLiteralExpression || (expression is PsiReferenceExpression)
    }


    private fun registerProblem(holder: ProblemsHolder, statement: PsiForStatement) {
        holder.registerProblem(statement, "Nested loop can be simplified", ProblemHighlightType.WARNING)
    }

    private fun registerProblemConstant(holder: ProblemsHolder, statement: PsiForStatement) {
        holder.registerProblem(statement, "Loop is constant and can be simplified", ProblemHighlightType.WARNING)
    }
}
