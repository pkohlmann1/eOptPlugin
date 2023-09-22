package com.github.pkohlmann1.eOptPlugin.inspections.refactoring

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*


class SimplifyNestedLoopInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitForStatement(statement: PsiForStatement) {
                super.visitForStatement(statement)
                // Check if it is a nested loop
                if (isNestedLoop(statement)) {
                    // Perform inspection
                    if (canSimplifyNestedLoop(statement)) {
                        registerProblem(holder, statement)
                    }
                } else {
                    if (isConstantLoop(statement)){
                        registerProblemConstant(holder, statement)
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
        val initialization = statement.initialization
        val condition = statement.condition
        val update = statement.update
        val body = statement.body

        if (initialization is PsiDeclarationStatement &&
            condition is PsiBinaryExpression &&
            update is PsiExpressionStatement &&
            body is PsiBlockStatement
        ) {
            val initializationStatement = initialization as PsiDeclarationStatement
            val conditionExpression = condition as PsiBinaryExpression
            val updateExpression = (update as PsiExpressionStatement).expression
            val bodyStatement = body as PsiBlockStatement

            var iterationVariable: PsiVariable? = null
            var iterationRange: PsiExpression? = null

            // Get the iteration variable and range of the outer loop
            val initializationElements = initializationStatement.declaredElements
            if (initializationElements.size == 1 && initializationElements[0] is PsiLocalVariable) {
                val iterationVariableDeclaration = initializationElements[0] as PsiLocalVariable
                iterationVariable = iterationVariableDeclaration

                val initializer = iterationVariableDeclaration.initializer
                if (initializer != null) {
                    iterationRange = initializer
                }
            }

            if (iterationVariable == null || iterationRange == null) {
                // Unable to determine the outer loop iteration variable or range
                return false
            }

            // Check the inner loop for dependencies on the outer loop iteration variable
            val innerLoopStatements = bodyStatement.codeBlock.statements
            for (innerLoopStatement in innerLoopStatements) {
                if (innerLoopStatement is PsiForStatement) {
                    val innerLoop = innerLoopStatement

                    val innerLoopInitialization = innerLoop.initialization
                    val innerLoopCondition = innerLoop.condition
                    val innerLoopUpdate = innerLoop.update

                    if (innerLoopInitialization is PsiDeclarationStatement &&
                        innerLoopCondition is PsiBinaryExpression &&
                        innerLoopUpdate is PsiExpressionStatement
                    ) {
                        val innerLoopInitializationStatement = innerLoopInitialization as PsiDeclarationStatement
                        val innerLoopConditionExpression = innerLoopCondition as PsiBinaryExpression
                        val innerLoopUpdateExpression = (innerLoopUpdate as PsiExpressionStatement).expression

                        val innerLoopInitializationElements = innerLoopInitializationStatement.declaredElements
                        if (innerLoopInitializationElements.size == 1 && innerLoopInitializationElements[0] is PsiLocalVariable) {
                            val innerLoopIterationVariableDeclaration = innerLoopInitializationElements[0] as PsiLocalVariable
                            val initializer = innerLoopIterationVariableDeclaration.initializer

                            if (initializer is PsiReferenceExpression) {
                                val referenceExpression = initializer as PsiReferenceExpression
                                val referencedElement = referenceExpression.resolve()

                                if (referencedElement != null && referencedElement == iterationVariable) {
                                    // Inner loop depends on the outer loop iteration variable
                                    return false
                                }
                            }
                        }

                        val operation = innerLoopConditionExpression.operationTokenType
                        val leftOperand = innerLoopConditionExpression.lOperand
                        val rightOperand = innerLoopConditionExpression.rOperand

                        if ((leftOperand is PsiReferenceExpression) && (leftOperand.resolve() == iterationVariable)){
                            return false
                        }
                        if ((rightOperand is PsiReferenceExpression) && (rightOperand.resolve() == iterationVariable)){
                            return false
                        }

                        // Check if the iteration variable is used in the inner loop body
                        val iterationVariableUsed = innerLoop.body?.let { isIterationVariableUsed(it, iterationVariable) }

                        if (iterationVariableUsed == true) {
                            // Inner loop depends on the outer loop iteration variable
                            return false
                        }

                        if (!isConstantRange(innerLoopConditionExpression)) {
                            // Inner loop range is not a constant value or known size
                            return false
                        }
                    } else {
                        // Inner loop has unsupported structure
                        return false
                    }
                } else {
                    // Statement in the body of the outer loop is not a nested for-loop
                    return false
                }
            }

            // All conditions for simplification are satisfied
            return true
        }

        // Statement is not a for-loop or has unsupported structure
        return false
    }

    private fun isIterationVariableUsed(body: PsiStatement, iterationVariable: PsiVariable): Boolean {
        if (body is PsiBlockStatement) {
            val statements = body.codeBlock.statements
            for (statement in statements) {
                if (isIterationVariableUsed(statement, iterationVariable)) {
                    return true
                }
            }
        } else if (body is PsiExpressionStatement) {
            val expression = body.expression
            return isIterationVariableUsed(expression, iterationVariable)
        }

        return false
    }

    private fun isIterationVariableUsed(expression: PsiExpression, iterationVariable: PsiVariable): Boolean {
        if (expression is PsiReferenceExpression) {
            val referenceExpression = expression
            val referencedElement = referenceExpression.resolve()

            if (referencedElement != null && referencedElement == iterationVariable) {
                return true
            }
        }
        else if (expression is PsiMethodCallExpression) {
            val methodExpression = expression
            val methodArguments = methodExpression.argumentList.expressions
            for (argument in methodArguments) {
                if (isIterationVariableUsed(argument, iterationVariable)) {
                    return true
                }
            }
        }

        return false
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
