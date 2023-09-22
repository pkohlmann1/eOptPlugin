package com.github.pkohlmann1.eOptPlugin.detectors

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*

class ImportStatementMethodCallDetector {

//    fun detectImportStatementsWithMethodCall(holder: ProblemsHolder, statement: PsiImportStatement, javaFile: PsiJavaFile, packages: MutableMap<String, Array<String>>){
//        for (packageName in packages.keys) {
//            if (statement.importReference!!.qualifiedName.startsWith(packageName)) {
//                val className = statement.importReference!!.qualifiedName
//
//                holder.registerProblem(
//                    statement, "Found import of $className at line ???",
//                    ProblemHighlightType.WARNING
//                )
//
//                if (containsTestMethod(
//                        javaFile, className,
//                        packages[packageName]!!, holder
//                    )
//                ) {
//                    holder.registerProblem(
//                        statement, "Class $className has network communication",
//                        ProblemHighlightType.WARNING
//                    )
//                }
//                return
//            }
//        }
//    }

    fun detectImportStatements(statement: PsiImportStatement, packages: MutableMap<String, Array<String>>): Boolean{
        for (packageName in packages.keys) {
            if (statement.importReference!!.qualifiedName.startsWith(packageName)) {
                return true;
            }
        }
        return false;
    }

    fun getImportStatement(name: String, packages: MutableMap<String, Array<String>>): String{
        for (packageName in packages.keys) {
            if (name.startsWith(packageName)) {
                return packageName
            }
        }
        return "";
    }

    fun containsTestMethod(
        cont_method: PsiMethod,
        className: String,
        invokingMethods: Array<String>,
        holder: ProblemsHolder
    ): Pair<PsiMethod?, PsiMethodCallExpression?> {

        var method: PsiMethod? = null
        var methodCallExpression: PsiMethodCallExpression? = null
        val methodCallVisitor = object : JavaRecursiveElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)

                val calledMethod = expression.resolveMethod()
                var calledMethodName = calledMethod?.name

                if(calledMethod == null) {
                    calledMethodName = expression.methodExpression.referenceName
                }

                if (calledMethodName != null && invokingMethods.any { calledMethodName.startsWith(it) }) {

//                    holder.registerProblem(
//                        expression, "Class $className calls $calledMethodName",
//                        ProblemHighlightType.WARNING
//                    )
                    method= calledMethod;
                    methodCallExpression = expression
                }
            }
        }
        cont_method.accept(methodCallVisitor)
        return Pair(method, methodCallExpression);
    }
}