package com.github.pkohlmann1.eOptPlugin.inspections.generalRules

import com.github.pkohlmann1.eOptPlugin.detectors.ImportStatementMethodCallDetector
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.java.PsiLocalVariableImpl
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl
import com.intellij.psi.impl.source.tree.java.PsiNewExpressionImpl
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl
import java.util.regex.Pattern


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
                                    val bodyVar = exec_method.first?.let {
                                        exec_method.second?.let { it1 ->
                                            getHttpBodyVariable(method,
                                                it, it1, import, holder)
                                        }
                                    }
                                    if (bodyVar != null) {
                                        getVarOrigin(bodyVar, method, holder, file)
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
        private val REQUEST_OBJECTS: MutableMap<String, String> = HashMap()

        init {
            NETWORK_PACKAGES["org.springframework.http"] = arrayOf("execute","exchange")
            NETWORK_PACKAGES["org.springframework.web"] = arrayOf("execute","exchange", "getForEntity", "getForObject", "postForLocation", "postForObject", "postForEntity", "put", "patchForObject", "delete", "doExecute")
//            NETWORK_PACKAGES["java.io"] = arrayOf("printStackTrace")

            REQUEST_OBJECTS["org.springframework.http"] = "org.springframework.http.HttpEntity"
            REQUEST_OBJECTS["org.springframework.web"] = "org.springframework.http.HttpEntity"

//            NETWORK_PACKAGES["retrofit2."] = arrayOf("abc")
//            NETWORK_PACKAGES["javax.ws.rs"] = arrayOf("test")
//            NETWORK_PACKAGES["io.vertex.ext.web"] = arrayOf("test")
//            NETWORK_PACKAGES["com.google.api.client.http"] = arrayOf("test")
//            NETWORK_PACKAGES["org.apache.http"] = arrayOf("test")
//            NETWORK_PACKAGES["okhttp3"] = arrayOf("test")
//            NETWORK_PACKAGES["java.net"] = arrayOf("test")
//            NETWORK_PACKAGES["java.io.BufferedReader"] = arrayOf("test")
//            NETWORK_PACKAGES["java.io.InputStreamReader"] = arrayOf("test")
//            NETWORK_PACKAGES["kong.unirest"] = arrayOf("test")
        }
    }

    fun getVarOrigin(exp: PsiReferenceExpression, method: PsiMethod, holder: ProblemsHolder, file: PsiJavaFile): Boolean {
        // Check if method has exp as Parameter. If so find Containing Method and continue search there
        val parameterList = method.parameterList.parameters
        for (parameter in parameterList) {
            if (parameter.name.equals(exp.referenceName)){
                val originFound = findContainingMethodAndGetVarOrigin(method, holder, file)
                if (originFound) {
                    return true
                }
            }
        }
        // Check if exp references any uncompressed media
        val methodBody = method.body
        val bodyExpression = methodBody?.children?.filterIsInstance<PsiDeclarationStatement>()?.filter { it.text.contains(exp.lastChild.text) }?.get(0)
        val variable = bodyExpression?.declaredElements?.firstOrNull() as? PsiVariable
        val initializer = variable?.initializer
        val formats = listOf("zip", "gz", "tar", "rar", "7z", "bz2")
        var fileExtension: String? = initializer?.text?.removeSurrounding("\"")?.substringAfterLast(".", "")?.substringBeforeLast(")")?.trim()?.replace("\"", "")
        val expectedTypeText = "LinkedMultiValueMap"
        if (formats.contains(fileExtension)){
            return true;
        }
        if (fileExtension != null && fileExtension != "" && fileExtension.length < 5 && !formats.contains(fileExtension)) {
            if (bodyExpression != null) {
                holder.registerProblem(
                    bodyExpression, "Uncompressed Media" ,
                    ProblemHighlightType.WARNING
                )
                return true
            }
        } else if (initializer != null) {
            //Check if Exp ist eine MultiValueMap (Abkürzung für Spring basierte Programme, da immer mit .add auf MultivalueMap gearbeitet werden muss)
            if (initializer.type?.toString()?.contains(expectedTypeText) == true) {
                val bodyExpressions = methodBody?.children?.filterIsInstance<PsiExpressionStatement>()?.filter {it.text.contains(".add")}
                if (bodyExpressions != null) {
                    for (i in bodyExpressions.indices){
                        val data = (bodyExpressions[i].firstChild as PsiMethodCallExpressionImpl).argumentList.expressions.get(0)
                        if (data.text.contains("file")){
                            val dataVar = (bodyExpressions[i].firstChild as PsiMethodCallExpressionImpl).argumentList.expressions.get(1)
                            if (dataVar is PsiReferenceExpression){
                                return getVarOrigin(dataVar, method, holder, file)
                            } else if (dataVar is PsiNewExpressionImpl){
                                return getVarOrigin(dataVar.argumentList?.expressions?.get(0) as PsiReferenceExpression, method, holder, file)
                            }

                        }
                    }
                }
                //Exp wurde durch Aufruf einer new Expression weiterverarbeitet. Verfolge Call Tree weiter nach oben
            } else if (initializer is PsiNewExpressionImpl) {
                val argumentList = initializer.argumentList
                for (expr in argumentList?.expressions!!) {
                    return getVarOrigin(expr as PsiReferenceExpression, method, holder, file)
                }
                //Exp wurde durch Aufruf einers Methodenaufrufs weiterverarbeitet. Verfolge Call Tree weiter nach oben
            } else if (initializer is PsiMethodCallExpressionImpl) {
                val argumentList = initializer.argumentList
                for (expr in argumentList.expressions) {
                    if (expr is PsiMethodCallExpression) {
                        return getVarOrigin((expr.firstChild.firstChild as PsiNewExpressionImpl).argumentList?.expressions?.get(0) as PsiReferenceExpression, method, holder, file)
                    } else {
                        return getVarOrigin(expr.firstChild.firstChild as PsiReferenceExpression, method, holder, file)
                    }
                }
            }
        }
        return false
    }

    private fun findContainingMethodAndGetVarOrigin(
        method: PsiMethod,
        holder: ProblemsHolder,
        file: PsiJavaFile
    ): Boolean {
        var originFound = false;
        val methodCallVisitor = object : JavaRecursiveElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                val calledMethod: PsiMethod? = expression.resolveMethod()
                var containingMethod: PsiMethod? = null
                if (calledMethod?.name.equals(method.name)) {
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
                        originFound = getVarOrigin(argument, containingMethod, holder, file)
                    }
                }
            }
        }
        file.accept(methodCallVisitor)
        return originFound
    }

    fun getHttpBodyVariable(
        method: PsiMethod,
        exec_method: PsiMethod,
        exec_method_call: PsiMethodCallExpression,
        imp: String,
        holder: ProblemsHolder
    ): PsiReferenceExpression? {

        var bodyRef: PsiReferenceExpression? = null

        val import: String? = REQUEST_OBJECTS[imp]
        val parameters = exec_method.parameterList.parameters // Die Parameter der Methode erhalten

        var paramCount = 0
        for (parameter in parameters) {
            val parameterType = parameter.type // Den Typ des Parameters erhalten
            if (parameterType is PsiClassType) {
                val parameterClass = parameterType.resolve() // Die Klasse des Parameters auflösen
                if (parameterClass != null && import == parameterClass.qualifiedName) {
                    // Der Parameter hat den Typ HttpEntity
                    val methodBody = method.body
                    val bodyDeclarations = methodBody?.children?.filterIsInstance<PsiDeclarationStatement>()
                    val httpEntityVar = bodyDeclarations?.filter{ (it.firstChild as PsiLocalVariableImpl).type.canonicalText.contains(import.toString())}

                    if (httpEntityVar?.size != 0) {
                        bodyRef = ((httpEntityVar?.get(0)?.declaredElements?.get(0) as PsiLocalVariableImpl).initializer as PsiNewExpressionImpl).argumentList?.expressions?.get(0) as PsiReferenceExpression?
                        return bodyRef
                    }
                }
                else if (parameterClass != null && parameterClass.qualifiedName == "java.lang.Object") {
                    val bodyParam = exec_method_call.argumentList.expressions.get(1) as? PsiReferenceExpressionImpl
                    val methodBody = method.body
                    val bodyDeclarations = methodBody?.children?.filterIsInstance<PsiDeclarationStatement>()
                    val bodyVar = bodyDeclarations?.firstOrNull { (it.firstChild as? PsiLocalVariableImpl)?.name == bodyParam?.referenceName }

                    bodyVar?.let {
                        val initializer = (it.declaredElements.firstOrNull() as? PsiLocalVariableImpl)?.initializer
                        val pattern = Pattern.compile("\".*?\\.([a-zA-Z0-9]+)\"")

                        val matcher = pattern.matcher(initializer?.toString()?.trimIndent())
                        if (matcher?.find() == true) {
                            val formats = listOf("zip", "gz", "tar", "rar", "7z", "bz2")
                            val fileType = matcher.group(1)
                            if (!formats.contains(fileType)) {
                                holder.registerProblem(
                                    exec_method_call, "Uncompressed Media",
                                    ProblemHighlightType.WARNING
                                )
                            }
                        } else {
                            checkInitializerVariablesForUncompressedMedia(initializer.toString(), bodyDeclarations, exec_method_call, holder)
                        }
                    }
                }

            }
            paramCount += 1
        }
        return bodyRef
    }

    private fun extractVariables(expression: String): List<String> {
        val variablePattern = Pattern.compile("\\b([a-zA-Z]\\w*)\\b(?![\\s\\(])")
        val matcher = variablePattern.matcher(expression)

        val variables = mutableListOf<String>()
        while (matcher.find()) {
            val variable = matcher.group(1)
            variables.add(variable)
        }

        return variables
    }

    private fun checkInitializerVariablesForUncompressedMedia(initializerString: String, bodyDeclarations:  List<PsiDeclarationStatement>?, exec_method_call: PsiMethodCallExpression, holder: ProblemsHolder){
        val pattern = Pattern.compile("\".*?\\.([a-zA-Z0-9]+)\"")
        val variables = initializerString.trimIndent().let { it1 -> extractVariables(it1) }
        for (variable in variables) {
            val vars = bodyDeclarations?.filter { (it.firstChild as? PsiLocalVariableImpl)?.name == variable }
            if (vars != null) {
                if (vars.isNotEmpty()) {
                    val innerInitializer = (vars?.get(0)?.declaredElements?.get(0) as? PsiLocalVariableImpl)?.initializer
                    val innerMatcher = pattern.matcher(innerInitializer?.toString()?.trimIndent())
                    if (innerMatcher.find() == true) {
                        val formats = listOf("zip", "gz", "tar", "rar", "7z", "bz2")
                        val fileType = innerMatcher.group(1)
                        if (!formats.contains(fileType)) {
                            holder.registerProblem(
                                exec_method_call, "Uncompressed Media",
                                ProblemHighlightType.WARNING
                            )
                        }
                    } else {
                        checkInitializerVariablesForUncompressedMedia(innerInitializer.toString(), bodyDeclarations, exec_method_call, holder)
                    }
                }
            }
        }
    }




}