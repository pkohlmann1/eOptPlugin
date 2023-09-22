package com.github.pkohlmann1.eOptPlugin.inspections.generalRules

import com.github.pkohlmann1.eOptPlugin.detectors.ImportStatementMethodCallDetector
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*

class DatabaseCommunicationInspection: AbstractBaseJavaLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {

        return object : JavaElementVisitor() {
        var containsDbImports = ArrayList<String>();
        override fun visitImportStatement(statement: PsiImportStatement) {
            super.visitImportStatement(statement)
            val file = statement.containingFile as PsiJavaFile;
            if (ImportStatementMethodCallDetector().detectImportStatements(statement,
                    DATABASE_PACKAGES
                )) {
                containsDbImports.add(
                    ImportStatementMethodCallDetector().getImportStatement(statement.importReference!!.qualifiedName,
                        DATABASE_PACKAGES
                    ))
            }
            if (containsDbImports.size != 0){
                val methodCallVisitor = object : JavaRecursiveElementVisitor() {
                    override fun visitMethod(method: PsiMethod?) {
                        for (import in containsDbImports) {
                            super.visitMethod(method)
                            val className = method?.containingClass?.name
                            val exec_method = className?.let {
                                ImportStatementMethodCallDetector().containsTestMethod(method,
                                    it, DATABASE_PACKAGES[import]!!, holder)
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
        private val DATABASE_PACKAGES: MutableMap<String, Array<String>> = HashMap()

        init {
            DATABASE_PACKAGES["org.springframework.beans.factory.annotation.Autowired"] = arrayOf("find", "save", "delete", "get")
            DATABASE_PACKAGES["org.springframework.stereotype.Service"] = arrayOf("find", "save", "delete", "get")
            DATABASE_PACKAGES["org.springframework.jdbc"] = arrayOf("batchUpdate", "execute", "query", "queryForList", "queryForMap", "queryForObject", "queryForRowSet", "queryForStream", "update")

        }
    }
}