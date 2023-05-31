package com.github.pkohlmann1.testplugin.detectors

import com.intellij.codeInspection.ProblemHighlightType
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
    ): PsiMethod? {

        var method: PsiMethod? = null;
        val methodCallVisitor = object : JavaRecursiveElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)

                val calledMethod = expression.resolveMethod()
                val calledMethodName = calledMethod?.name

                if (calledMethodName != null && invokingMethods.any { calledMethodName.startsWith(it) }) {

                    holder.registerProblem(
                        expression, "Class $className calls $calledMethodName",
                        ProblemHighlightType.WARNING
                    )
                    method= calledMethod;
                }
            }
        }
        cont_method.accept(methodCallVisitor)
        return method;
    }
}