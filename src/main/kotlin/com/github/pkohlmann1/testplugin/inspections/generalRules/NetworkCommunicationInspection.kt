package com.github.pkohlmann1.testplugin.inspections.generalRules

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*

class NetworkCommunicationInspection : AbstractBaseJavaLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitImportStatement(statement: PsiImportStatement) {
//                super.visitImportStatement(statement)
//                val javaFile = statement.containingFile as PsiJavaFile
//                ImportStatementMethodCallDetector().detectImportStatementsWithMethodCall(holder,statement, javaFile,
//                    NETWORK_PACKAGES)
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
}