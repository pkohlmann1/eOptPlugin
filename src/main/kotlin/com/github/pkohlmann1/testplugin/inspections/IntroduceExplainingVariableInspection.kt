package com.github.pkohlmann1.testplugin.inspections

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
        if (expression.parent is PsiIfStatement || expression.parent is PsiWhileStatement || expression.parent is PsiDoWhileStatement) {
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
        val containsMethodInvocation = containsMethodInvocation(expression)

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
            "&",
            "\\|",
            "\\^",
            "!",
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
                val nestingLevel = countNestingLevels(argument)
                maxNestingLevel = Math.max(maxNestingLevel, nestingLevel)
            }
            return maxNestingLevel + 1
        }
        return 0
    }


    private fun containsMethodInvocation(expression: PsiExpression?): Boolean {
        // Check if the expression contains a method invocation
        if (expression is PsiMethodCallExpression) {
            return true
        }

        // Recursively check if any subexpressions contain a method invocation
        if (expression is PsiBinaryExpression) {
            val binaryExpression = expression
            val leftOperand = binaryExpression.lOperand
            val rightOperand = binaryExpression.rOperand
            return containsMethodInvocation(leftOperand) || containsMethodInvocation(rightOperand)
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
