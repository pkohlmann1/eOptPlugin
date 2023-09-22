package com.github.pkohlmann1.eOptPlugin.inspections.refactoring

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*


class IntroduceExplainingVariableInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitExpressionStatement(statement: PsiExpressionStatement) {
                super.visitExpressionStatement(statement)
                val expression = statement.expression
                if (expression is PsiAssignmentExpression) {
                    val assignmentExpression = expression
                    val lhs = assignmentExpression.lExpression
                    val rhs = assignmentExpression.rExpression

                    // Check if the assignment expression is a complex expression
                    if (rhs?.let { isComplexExpression(it, false) } == true) {
                        // Perform inspection
                        registerProblem(holder, assignmentExpression)
                    }
                }
            }

            override fun visitNewExpression(expression: PsiNewExpression?) {
                super.visitNewExpression(expression)

                if (isExpressionComplex(expression)) {
                    // Mark the expression as complex (e.g., print a message, add a warning, etc.)
                    if (expression != null) {
                        registerProblem(holder, expression)
                    }
                }
            }

            override fun visitMethodCallExpression(expression: PsiMethodCallExpression?) {
                super.visitMethodCallExpression(expression)
                val methodName = expression?.methodExpression?.referenceName
                if (methodName == "forEach") {
                    val forEachArgument = expression.argumentList?.expressions?.firstOrNull()
                    if (forEachArgument is PsiLambdaExpression) {
                        val lambdaBody = forEachArgument.body
                        if (lambdaBody is PsiBlockStatement) {
                            val block = lambdaBody.codeBlock
                            for (innerStatement in block.statements) {
                                checkStatementForComplexity(innerStatement, holder)
                            }
                        }
                        else if (lambdaBody is PsiCodeBlock) {
                            for (innerStatement in lambdaBody.statements) {
                                checkStatementForComplexity(innerStatement, holder)
                            }
                        }
                        else if (lambdaBody is PsiMethodCallExpression) {
                            if (isExpressionComplex(lambdaBody)) {
                                registerProblem(holder, expression)
                            }
                        }
                        else {
                            checkStatementForComplexity(lambdaBody as PsiStatement, holder)
                        }
                    }
                }
                else if (isExpressionComplex(expression)) {
                    // Mark the expression as complex (e.g., print a message, add a warning, etc.)
                    if (expression != null) {
                        registerProblem(holder, expression)
                    }
                }
            }

            override fun visitReturnStatement(statement: PsiReturnStatement?) {
                super.visitReturnStatement(statement)

                val returnValue = statement?.returnValue
                if (isExpressionComplex(returnValue)) {
                    // Mark the return statement as complex (e.g., print a message, add a warning, etc.)
                    if (returnValue != null) {
                        registerProblem(holder, returnValue)
                    }
                }
            }

            override fun visitSwitchStatement(statement: PsiSwitchStatement?) {
                super.visitSwitchStatement(statement)
                val condition: PsiExpression? = statement?.expression;
                if (condition != null) {
                    checkCondition(holder, condition)
                }
            }

            override fun visitIfStatement(statement: PsiIfStatement) {
                super.visitIfStatement(statement)
                val condition: PsiExpression? = statement.condition;
                if (condition != null) {
                    checkCondition(holder, condition)
                }
            }

            override fun visitWhileStatement(statement: PsiWhileStatement?) {
                super.visitWhileStatement(statement)
                val condition: PsiExpression? = statement?.condition
                if (condition != null) {
                    checkCondition(holder, condition)
                }
            }

            override fun visitDoWhileStatement(statement: PsiDoWhileStatement?) {
                super.visitDoWhileStatement(statement)
                val condition: PsiExpression? = statement?.condition
                if (condition != null) {
                    checkCondition(holder, condition)
                }
            }
        }
    }

    private fun checkCondition(holder: ProblemsHolder, expression: PsiExpression){
        if (expression.parent is PsiIfStatement || expression.parent is PsiWhileStatement || expression.parent is PsiDoWhileStatement || expression.parent is PsiSwitchStatement) {
            // Check if the condition expression is a complex expression
            if (isComplexExpression(expression)) {
                // Perform inspection
                registerProblem(holder, expression)
            }
        }
    }

    private fun isComplexExpression(expression: PsiExpression, isCondition: Boolean = true): Boolean {
        // Count the number of operators in the expression
        val operatorCount = countOperators(expression.text)

        // Count the number of nesting levels in the expression
        val nestingLevel = countNestingLevels(expression)

        // Check if the expression contains method invocations
        val containsMethodInvocation = containsMethodInvocationWithParameters(expression)

        if (isCondition) {
            return operatorCount > 3 || nestingLevel > 2 || containsMethodInvocation
        } else {
            return operatorCount > 3 || nestingLevel > 2
        }
    }

    private fun countOperators(expressionText: String): Int {
        // Define the operators you want to count
        val operators = arrayOf(
            "\\+",
            "-",
            "\\*",
            "/",
            "%",
            "&&",
            "\\|\\|",
            "\\^",
            "~",
            "<<",
            ">>",
            ">>>",
            "<",
            ">",
            "<=",
            ">=",
            "==",
            "!=",
            "instanceof"
        )

        // Count the number of occurrences of operators in the expression text
        var count = 0
        for (operator in operators) {
            count += expressionText.split(operator.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size - 1
        }
        return count
    }


    private fun countNestingLevels(expression: PsiExpression?): Int {
        // Recursively count the number of nesting levels in the expression
        if (expression is PsiBinaryExpression) {
            val binaryExpression = expression
            val leftOperand = binaryExpression.lOperand
            val rightOperand = binaryExpression.rOperand
            val leftNestingLevel = countNestingLevels(leftOperand)
            val rightNestingLevel = countNestingLevels(rightOperand)
            return Math.max(leftNestingLevel, rightNestingLevel) + 1
        } else if (expression is PsiMethodCallExpression) {
            val methodCallExpression = expression
            val argumentList = methodCallExpression.argumentList
            var maxNestingLevel = 0
            for (argument in argumentList.expressions) {
                val nestingLevel = countNestingLevels(argument) + 1
                maxNestingLevel = Math.max(maxNestingLevel, nestingLevel)
            }
            return maxNestingLevel
        }
        else if (expression is PsiPrefixExpression) {
            return countNestingLevels(expression.lastChild as PsiExpression?)
        }
        return 0
    }


    private fun containsMethodInvocationWithParameters(expression: PsiExpression?): Boolean {
        // Check if the expression contains a method invocation
        if (expression is PsiMethodCallExpression) {
            val argumentList = expression.argumentList
            if (argumentList.expressions.isNotEmpty()) {
                return true
            }
        }

        // Recursively check if any subexpressions contain a method invocation
        if (expression is PsiBinaryExpression) {
            val binaryExpression = expression
            val leftOperand = binaryExpression.lOperand
            val rightOperand = binaryExpression.rOperand
            return containsMethodInvocationWithParameters(leftOperand) || containsMethodInvocationWithParameters(rightOperand)
        }
        if (expression is PsiPrefixExpression) {
            return containsMethodInvocationWithParameters(expression.lastChild as PsiExpression?);
        }
        return false
    }

    private fun checkLoopBodyForComplexity(statement: PsiStatement, holder: ProblemsHolder) {
        if (statement is PsiBlockStatement) {
            val block = statement.codeBlock
            for (innerStatement in block.statements) {
                checkStatementForComplexity(innerStatement, holder)
            }
        } else {
            checkStatementForComplexity(statement, holder)
        }
    }

    private fun checkStatementForComplexity(statement: PsiStatement, holder: ProblemsHolder) {
        if (statement is PsiExpressionStatement) {
            val expression = statement.expression
            if (isExpressionComplex(expression)) {
                // Mark the expression as complex (e.g., print a message, add a warning, etc.)
                registerProblem(holder, expression)

            }
        } else if (statement is PsiBlockStatement) {
            val block = statement.codeBlock
            for (innerStatement in block.statements) {
                checkStatementForComplexity(innerStatement, holder)
            }
        }
    }

    private fun isExpressionComplex(expression: PsiExpression?): Boolean {
        if (expression == null) {
            return false
        }

        if (expression is PsiMethodCallExpression) {
            return isMethodCallWithNestedArguments(expression)
        }

        if (expression is PsiNewExpression) {
            return isConstructorCallWithNestedArguments(expression)
        }

        if (expression is PsiLambdaExpression) {
            val body = expression.body
            if (body !is PsiCodeBlock) {
                return isExpressionComplex(body as PsiExpression?)
            }
        }
        if (expression is PsiBinaryExpression) {
            return isComplexExpression(expression)
        }

        return false
    }

    private fun isMethodCallWithNestedArguments(expression: PsiMethodCallExpression): Boolean {
        val arguments = expression.argumentList.expressions

        for (arg in arguments) {
            if (isMethodCallWithArguments(arg) || isLambdaWithNestedExpression(arg) || isConstructorInvocationWithArguments(arg)) {
                // The argument itself is a method call expression with arguments, so the expression is complex
                return true
            }
        }

        return false
    }

    private fun isLambdaWithNestedExpression(expression: PsiExpression?): Boolean {
        if (expression !is PsiLambdaExpression) {
            return false
        }

        return isExpressionComplex(expression)
    }

    private fun isConstructorCallWithNestedArguments(expression: PsiNewExpression): Boolean {
        val arguments = expression.argumentList?.expressions

        if (arguments != null) {
            for (arg in arguments) {
                if (isMethodCallWithArguments(arg)) {
                    // The argument itself is a method call expression with arguments, so the expression is complex
                    return true
                }
                if (isConstructorInvocationWithArguments(arg)){
                    return true
                }
            }
        }

        return false
    }

    private fun isMethodCallWithArguments(expression: PsiExpression?): Boolean {
        if (expression !is PsiMethodCallExpression) {
            return false
        }

        val arguments = expression.argumentList.expressions

        return arguments.isNotEmpty()
    }

    private fun isConstructorInvocationWithArguments(expression: PsiExpression?): Boolean {
        if (expression is PsiNewExpression) {
            val argumentList = expression.argumentList
            if (argumentList != null && argumentList.expressions.isNotEmpty()) {
                return true
            }
        }

        if (expression is PsiBinaryExpression) {
            val leftOperand = expression.lOperand
            val rightOperand = expression.rOperand
            return isConstructorInvocationWithArguments(leftOperand) || isConstructorInvocationWithArguments(rightOperand)
        }

        if (expression is PsiPrefixExpression) {
            return isConstructorInvocationWithArguments(expression.lastChild as PsiExpression?)
        }

        return false
    }



    private fun registerProblem(holder: ProblemsHolder, expression: PsiExpression) {
        holder.registerProblem(
            expression,
            "Complex expression can be simplified by introducing explaining variable",
            ProblemHighlightType.WARNING
        )
    }
}
