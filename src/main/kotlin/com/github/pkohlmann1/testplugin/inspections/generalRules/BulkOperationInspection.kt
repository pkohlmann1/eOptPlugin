package com.github.pkohlmann1.testplugin.inspections.generalRules

import com.github.pkohlmann1.testplugin.detectors.ImportStatementMethodCallDetector
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

class BulkOperationInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            var containsDbImports = ArrayList<String>();
            var containsNetworkImports = ArrayList<String>();
            override fun visitImportStatement(statement: PsiImportStatement) {
                super.visitImportStatement(statement)
                val javaFile = statement.containingFile as PsiJavaFile
                if (ImportStatementMethodCallDetector().detectImportStatements(statement, DATABASE_PACKAGES)) {
                    this.containsDbImports.add(ImportStatementMethodCallDetector().getImportStatement(statement.importReference!!.qualifiedName,
                        DATABASE_PACKAGES
                    ))
                }
                if (ImportStatementMethodCallDetector().detectImportStatements(statement, NETWORK_PACKAGES)) {
                    this.containsNetworkImports.add(ImportStatementMethodCallDetector().getImportStatement(statement.importReference!!.qualifiedName,
                        NETWORK_PACKAGES
                    ))
                }
            }

            override fun visitForStatement(statement: PsiForStatement?) {
                super.visitForStatement(statement)
                if (statement!= null) {
                    for (import in this.containsDbImports){
                        checkIfLoopContainsMethod(statement, import, DATABASE_PACKAGES, "Datenbank")
                    }
                    for (import in this.containsNetworkImports){
                        checkIfLoopContainsMethod(statement, import, NETWORK_PACKAGES, "Netzwerk")
                    }
                }
            }

            override fun visitForeachStatement(statement: PsiForeachStatement) {
                super.visitForeachStatement(statement)

                val collectionExpression = statement.iteratedValue
                val project = statement.project
                val collectionType = collectionExpression?.type

                // Überprüfen, ob die Expression eine Collection ist
                if (isCollectionType(collectionType, project)) {
                    // Überprüfen, ob im Schleifenkörper einzelne Operationen auf Elemente angewendet werden
                    val body = statement.body
                    if (body != null && containsElementOperation(body)) {
                        // Registrieren Sie ein Problem und schlagen Bulk Operations vor
                        holder.registerProblem(
                            statement,
                            "Consider using bulk operations like addAll, removeAll, or retainAll for improved performance",
                            ProblemHighlightType.WARNING
                        )
                    }
                }
            }

            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)

                val methodName = expression.methodExpression.referenceName
                val containingMethod = PsiTreeUtil.getParentOfType(expression, PsiMethod::class.java)

                if (methodName != null && containingMethod != null) {
                    val methodCalls = mutableListOf<String>()
                    containingMethod.accept(object : JavaRecursiveElementVisitor() {
                        override fun visitMethodCallExpression(callExpression: PsiMethodCallExpression) {
                            super.visitMethodCallExpression(callExpression)
                            val calledMethodName = callExpression.methodExpression.referenceName
                            if (calledMethodName != null) {
                                methodCalls.add(calledMethodName)
                            }
                        }
                    })

                    val duplicateCalls = methodCalls.filter { it == methodName }
                    for (import in containsDbImports){
                        for (duplicate in duplicateCalls){
                            if (DATABASE_PACKAGES[import]?.any { duplicate.startsWith(it) } == true){
                                holder.registerProblem(
                                    expression,
                                    "Duplicate method call: $methodName - Diese Methode führt Datenbank-Kommunikation und wird häufiger aufgerufen. Bulk Operations verwenden?",
                                    ProblemHighlightType.WARNING
                                )
                                return
                            }
                        }
                    }
                    for (import in containsNetworkImports){
                        for (duplicate in duplicateCalls){
                            if (NETWORK_PACKAGES[import]?.any { duplicate.startsWith(it) } == true){
                                holder.registerProblem(
                                    expression,
                                    "Duplicate method call: $methodName - Diese Methode führt Netzwerk-Kommunikation und wird häufiger aufgerufen. Bulk Operations verwenden?",
                                    ProblemHighlightType.WARNING
                                )
                                return
                            }
                        }
                    }
                }
            }

            private fun checkIfLoopContainsMethod(statement: PsiForStatement, import: String,  packages: MutableMap<String, Array<String>>, type: String) {
                    statement.body?.accept(object : JavaRecursiveElementVisitor() {
                        override fun visitMethodCallExpression(expression: PsiMethodCallExpression?) {
                            super.visitMethodCallExpression(expression)
                            val calledMethod = expression?.resolveMethod()
                            val calledMethodName = calledMethod?.name
                            val invokingMethods: Array<String>? = packages[import]
                            if (invokingMethods != null) {
                                if (calledMethodName != null && invokingMethods.any { calledMethodName.startsWith(it) }) {

                                    holder.registerProblem(
                                        expression, "Class calls $calledMethodName - $type-Kommunikation in For-Schleife erkannt. Bulk Operations nutzen?",
                                        ProblemHighlightType.WARNING
                                    )
                                    return
                                }
                            }
                        }
                    })
            }



            private fun isCollectionType(type: PsiType?, project: Project): Boolean {
                // Überprüfen, ob der Typ eine Collection ist
                val collectionType = type?.resolveScope?.let {
                    PsiType.getTypeByName("java.util.Collection", project, it)
                }
                return collectionType?.isAssignableFrom(type) == true
            }





            private fun containsElementOperation(element: PsiElement): Boolean {
                // Überprüfen, ob im Schleifenkörper einzelne Operationen auf Elemente angewendet werden
                return PsiTreeUtil.findChildOfType(element, PsiExpression::class.java) != null
            }
        }
    }

    companion object {
        private val DATABASE_PACKAGES: MutableMap<String, Array<String>> = HashMap()
        private val NETWORK_PACKAGES: MutableMap<String, Array<String>> = HashMap()

        init {
            DATABASE_PACKAGES["java.sql"] = arrayOf("test")
            DATABASE_PACKAGES["java.persistence"] = arrayOf("test")
            DATABASE_PACKAGES["org.hibernate"] = arrayOf("test")
            DATABASE_PACKAGES["org.springframework.data.jpa.repository.JpaRepository"] = arrayOf("test")
            DATABASE_PACKAGES["org.springframework.data.repository"] = arrayOf("test")
            DATABASE_PACKAGES["org.apache.commons.dbutils"] = arrayOf("test")
            DATABASE_PACKAGES["org.jooq"] = arrayOf("test")
            DATABASE_PACKAGES["org.springframework.beans.factory.annotation.Autowired"] = arrayOf("find", "save", "delete")
            DATABASE_PACKAGES["org.springframework.stereotype.Service"] = arrayOf("find", "save", "delete")
            NETWORK_PACKAGES["org.apache.http"] = arrayOf("test")
            NETWORK_PACKAGES["okhttp3"] = arrayOf("test")
            NETWORK_PACKAGES["java.net"] = arrayOf("test")
            NETWORK_PACKAGES["java.io.BufferedReader"] = arrayOf("test")
            NETWORK_PACKAGES["java.io.InputStreamReader"] = arrayOf("test")
            NETWORK_PACKAGES["kong.unirest"] = arrayOf("test")
            NETWORK_PACKAGES["org.springframework.http"] = arrayOf("execute","exchange")
            NETWORK_PACKAGES["org.springframework.web"] = arrayOf("execute","exchange")
            NETWORK_PACKAGES["retrofit2."] = arrayOf("abc")
            NETWORK_PACKAGES["javax.ws.rs"] = arrayOf("test")
            NETWORK_PACKAGES["io.vertex.ext.web"] = arrayOf("test")
            NETWORK_PACKAGES["com.google.api.client.http"] = arrayOf("test")
            NETWORK_PACKAGES["java.io"] = arrayOf("printStackTrace")
        }
    }
}

