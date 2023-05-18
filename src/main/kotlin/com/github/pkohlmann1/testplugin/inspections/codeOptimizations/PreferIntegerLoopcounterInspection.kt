package com.github.pkohlmann1.testplugin.inspections.codeOptimizations

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil

class PreferIntegerLoopCounterInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitForStatement(statement: PsiForStatement) {
                super.visitForStatement(statement)
                val initialization = statement.initialization
                val condition = statement.condition
                val update = statement.update

                // Überprüfen, ob die Initialisierung, Bedingung und das Update vorhanden sind
                if (initialization != null && condition != null && update != null) {
                    // Überprüfen, ob der Iterationsbereich Gleitkommazahlen verwendet
                    if (usesFloatingPointNumbers(condition, initialization, update)) {
                        // Überprüfen, ob die Schleife keine Gleitkommazahlen erfordert
                        if (!requiresFloatingPointNumbers(statement)) {
                            registerProblem(holder, statement)
                        }
                    }
                }
            }
        }
    }

    private fun usesFloatingPointNumbers(condition: PsiExpression, initialization: PsiStatement,update: PsiStatement): Boolean {
        if (condition is PsiBinaryExpression) {
            val operation = condition.operationTokenType
            val leftOperand = condition.lOperand
            val rightOperand = condition.rOperand

            // Überprüfen, ob der Operator Gleitkommazahlen verwendet
            if (isFloatingPointOperation(operation) && (isFloatingPointNumber(leftOperand) || isFloatingPointNumber(rightOperand))) {
                return true
            }
        }
        // Überprüfen, ob die Initialisierungsvariable ein Gleitkommazahl-Typ ist
        if (initialization is PsiDeclarationStatement) {
            val initializationElements = initialization.declaredElements
            if (initializationElements.size == 1 && initializationElements[0] is PsiLocalVariable) {
                val iterationVariableDeclaration = initializationElements[0] as PsiLocalVariable
                val iterationVariableType = iterationVariableDeclaration.type

                if (isFloatingPointType(iterationVariableType)) {
                    return true
                }
            }
        }
        if (update is PsiExpressionStatement) {
            // Überprüfen, ob der Updateregel eine Gleitkommazahl enthält
            val updateExpression = update.expression
            if (isFloatingPointNumber(updateExpression)) {
                return true
            }
        }


        return false
    }

    private fun isFloatingPointType(type: PsiType?): Boolean {
        return type is PsiPrimitiveType && (type == PsiType.FLOAT || type == PsiType.DOUBLE)
    }


    private fun isFloatingPointOperation(operation: IElementType?): Boolean {
        return operation == JavaTokenType.GT || operation == JavaTokenType.GE || operation == JavaTokenType.LT || operation == JavaTokenType.LE
    }

    private fun isFloatingPointNumber(expression: PsiExpression?): Boolean {
        return expression is PsiLiteralExpression && expression.type == PsiType.FLOAT || expression is PsiLiteralExpression && expression.type == PsiType.DOUBLE
    }

    private fun requiresFloatingPointNumbers(statement: PsiForStatement): Boolean {
        val body = statement.body
        if (body != null) {
            val references = PsiTreeUtil.findChildrenOfType(body, PsiReferenceExpression::class.java)
            for (reference in references) {
                val resolved = reference.resolve()
                if (resolved is PsiVariable) {
                    val variableType = resolved.type
                    // Überprüfen, ob die Schleife Gleitkommazahlen erfordert
                    if (variableType == PsiType.FLOAT || variableType == PsiType.DOUBLE) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun registerProblem(holder: ProblemsHolder, statement: PsiForStatement) {
        holder.registerProblem(statement, "Prefer integer loop counter", ProblemHighlightType.WARNING)
    }
}
