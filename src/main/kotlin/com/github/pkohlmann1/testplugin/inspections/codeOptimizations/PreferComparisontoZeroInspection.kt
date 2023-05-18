package com.github.pkohlmann1.testplugin.inspections.codeOptimizations

import com.intellij.codeInspection.*
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiBinaryExpression
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiExpression

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

class PreferComparisontoZeroInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitForStatement(statement: PsiForStatement) {
                super.visitForStatement(statement)

                val initialization = statement.initialization
                val condition = statement.condition
                val update = statement.update

                // Check if the loop has an integer iteration variable
                if (initialization is PsiDeclarationStatement && condition is PsiBinaryExpression &&
                    update is PsiExpressionStatement
                ) {
                    val iterationVariable = getIterationVariable(initialization)
                    if (iterationVariable != null && PsiType.INT == iterationVariable.type) {

                        // Check if the loop condition depends on the sign of the iteration variable
                        val comparison = getComparisonWithVariable(condition, iterationVariable)
                        if (comparison != null && isSignDependent(comparison)) {
                            val quickFix = ConvertToZeroComparisonQuickFix(statement)
                            holder.registerProblem(condition, "Loop depends on the sign of the iteration variable", quickFix)
                        }
                    }
                }
            }
        }
    }

    private fun getIterationVariable(declarationStatement: PsiDeclarationStatement): PsiVariable? {
        val declaredElements = declarationStatement.declaredElements
        if (declaredElements.size == 1 && declaredElements[0] is PsiVariable) {
            return declaredElements[0] as PsiVariable
        }
        return null
    }

    private fun getComparisonWithVariable(binaryExpression: PsiBinaryExpression, variable: PsiVariable): PsiBinaryExpression? {
        if (binaryExpression.lOperand is PsiReferenceExpression) {
            val leftOperand = binaryExpression.lOperand as PsiReferenceExpression
            if (leftOperand.isReferenceTo(variable)) {
                return binaryExpression
            }
        }
        if (binaryExpression.rOperand is PsiReferenceExpression) {
            val rightOperand = binaryExpression.rOperand as PsiReferenceExpression
            if (rightOperand.isReferenceTo(variable)) {
                return binaryExpression
            }
        }
        return null
    }

    private fun isSignDependent(binaryExpression: PsiBinaryExpression): Boolean {
        val operation = binaryExpression.operationTokenType
        return operation == JavaTokenType.GT || operation == JavaTokenType.GE ||
                operation == JavaTokenType.LT || operation == JavaTokenType.LE
    }

    private class ConvertToZeroComparisonQuickFix(private val statement: PsiForStatement) : LocalQuickFix {
        override fun getFamilyName(): String {
            return "Convert loop condition to comparison with 0"
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val loop = statement

            val initialization = loop.initialization
            val condition = loop.condition
            val update = loop.update

            if (initialization is PsiDeclarationStatement && condition is PsiBinaryExpression && update is PsiExpressionStatement) {
                val iterationVariableDeclaration = initialization.declaredElements.firstOrNull() as? PsiLocalVariable ?: return
                val iterationVariable = iterationVariableDeclaration.name ?: return

                // Get the termination value of the loop
                val terminationValue = getTerminationValue(condition, iterationVariable) ?: return

                // Create the new initialization statement
                val newInitialization = JavaPsiFacade.getElementFactory(project)
                    .createStatementFromText("int $iterationVariable = $terminationValue;", loop) as? PsiStatement ?: return

                // Create the new condition
                val newCondition = JavaPsiFacade.getElementFactory(project)
                    .createExpressionFromText("$iterationVariable != 0", loop) as? PsiExpression ?: return

                // Create the new update statement
                val newUpdate = JavaPsiFacade.getElementFactory(project)
                    .createStatementFromText("$iterationVariable--", loop) as? PsiStatement ?: return

                // Replace the old loop components with the new ones
                loop.initialization!!.replace(newInitialization)
                loop.condition!!.replace(newCondition)
                loop.update!!.replace(newUpdate)
            }
        }

        private fun getTerminationValue(condition: PsiBinaryExpression, iterationVariable: String): String? {
            val leftOperand = condition.lOperand
            val rightOperand = condition.rOperand

            if (leftOperand is PsiReferenceExpression && leftOperand.text == iterationVariable) {
                return rightOperand?.text
            } else if (rightOperand is PsiReferenceExpression && rightOperand.text == iterationVariable) {
                return leftOperand?.text
            }

            return null
        }

    }
}

