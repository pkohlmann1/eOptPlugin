package com.github.pkohlmann1.testplugin.inspections.generalRules

import com.github.pkohlmann1.testplugin.detectors.ImportStatementMethodCallDetector
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl
import com.intellij.psi.impl.source.tree.java.PsiNewExpressionImpl

class NetworkCommunicationInspection : AbstractBaseJavaLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            var containsNetworkImports = ArrayList<String>();
            override fun visitImportStatement(statement: PsiImportStatement) {
                super.visitImportStatement(statement)
                val file = statement.containingFile as PsiJavaFile;
                    if (ImportStatementMethodCallDetector().detectImportStatements(statement,
                        NETWORK_PACKAGES
                    )) {
                    this.containsNetworkImports.add(
                        ImportStatementMethodCallDetector().getImportStatement(statement.importReference!!.qualifiedName,
                            NETWORK_PACKAGES
                    ))
                }
                if (containsNetworkImports.size != 0){
                    val methodCallVisitor = object : JavaRecursiveElementVisitor() {
                        override fun visitMethod(method: PsiMethod?) {
                            for (import in containsNetworkImports) {
                                super.visitMethod(method)
                                val className = method?.containingClass?.name
                                val exec_method = className?.let {
                                    ImportStatementMethodCallDetector().containsTestMethod(method,
                                        it, NETWORK_PACKAGES[import]!!, holder)
                                }
                                if (exec_method != null) {
                                    val bodyVar = getDeclaredVariable(method)
                                    if (bodyVar != null) {
                                        for (i in bodyVar.indices){
                                            val data = (bodyVar[i].firstChild as PsiMethodCallExpressionImpl).argumentList.expressions.get(0)
                                            if (data.text.contains("file")){
                                                val dataVar: PsiReferenceExpression = (bodyVar[i].firstChild as PsiMethodCallExpressionImpl).argumentList.expressions.get(1) as PsiReferenceExpression
                                                getVarOrigin(dataVar, method, holder, file)
                                            }
                                        }
                                    }

                                }
                            }
                        }
                    }
                    file.accept(methodCallVisitor)
                }

            }
        }
    }

    companion object {
        private val NETWORK_PACKAGES: MutableMap<String, Array<String>> = HashMap()

        init {
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

    fun getVarOrigin(exp: PsiReferenceExpression, method: PsiMethod, holder: ProblemsHolder, file: PsiJavaFile) {
        val parameterList = method.parameterList.parameters
        for (parameter in parameterList) {
            if (parameter.name.equals(exp.referenceName)){
                findContainingMethodAndGetVarOrigin(method, holder, file)
            }
        }
        val methodBody = method.body
        val bodyExpression = methodBody?.children?.filterIsInstance<PsiDeclarationStatement>()?.filter { it.text.contains(exp.lastChild.text) }?.get(0)
        val variable = bodyExpression?.declaredElements?.firstOrNull() as? PsiVariable
        val initializer = variable?.initializer
        val formats = listOf("zip", "gz", "tar", "rar", "7z", "bz2")
        val fileExtension = initializer?.text?.removeSurrounding("\"")?.substringAfterLast(".", "")?.trim()

        if (fileExtension != null && fileExtension != "" && !formats.contains(fileExtension)) {
            holder.registerProblem(
                    bodyExpression, "Uncompressed Media",
                    ProblemHighlightType.WARNING
                )
        } else {
            val constructorArguments: Array<PsiExpression>? = (initializer as PsiNewExpressionImpl).argumentList?.expressions
            val argument: PsiReferenceExpression = constructorArguments?.get(0) as PsiReferenceExpression
            getVarOrigin(argument, method, holder, file)
        }
    }

    private fun findContainingMethodAndGetVarOrigin(
        method: PsiMethod,
        holder: ProblemsHolder,
        file: PsiJavaFile
    ) {
        val methodCallVisitor = object : JavaRecursiveElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                val calledMethod: PsiMethod? = expression.resolveMethod()
                var containingMethod: PsiMethod? = null
                if (calledMethod == method) {
                    var parent: PsiElement? = expression.parent
                    while (parent != null) {
                        if (parent is PsiMethod) {
                            // Found the containing method
                            containingMethod = parent
                            // Process the containing method as needed
                            break
                        }
                        parent = parent.parent
                    }
                    if (containingMethod != null) {
                        val constructorArguments: Array<PsiExpression>? = expression.argumentList.expressions
                        val argument: PsiReferenceExpression = constructorArguments?.get(0) as PsiReferenceExpression
                        getVarOrigin(argument, containingMethod, holder, file)
                    }
                }
            }
        }
        file.accept(methodCallVisitor)
    }

    fun getDeclaredVariable(method: PsiMethod): List<PsiExpressionStatement>? {
        val methodBody = method.body
        val bodyExpressions = methodBody?.children?.filterIsInstance<PsiExpressionStatement>()
        return bodyExpressions?.filter {it.text.contains("body.add")}
    }

}