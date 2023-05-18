package com.github.pkohlmann1.testplugin.inspections.generalRules

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiImportStatement

class DatabaseCommunicationInspection: AbstractBaseJavaLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitImportStatement(statement: PsiImportStatement) {
                super.visitImportStatement(statement)
//                val javaFile = statement.containingFile as PsiJavaFile
//                ImportStatementMethodCallDetector().detectImportStatementsWithMethodCall(holder,statement, javaFile,
//                    DATABASE_PACKAGES)
            }

        }
    }

    companion object {
        private val DATABASE_PACKAGES: MutableMap<String, Array<String>> = HashMap()

        init {
            DATABASE_PACKAGES["java.sql"] = arrayOf("test")
            DATABASE_PACKAGES["java.persistence"] = arrayOf("test")
            DATABASE_PACKAGES["org.hibernate"] = arrayOf("test")
            DATABASE_PACKAGES["org.springframework.data.jpa.repository.JpaRepository"] = arrayOf("test")
            DATABASE_PACKAGES["org.springframework.data.repository"] = arrayOf("test")
            DATABASE_PACKAGES["org.apache.commons.dbutils"] = arrayOf("test")
            DATABASE_PACKAGES["org.jooq"] = arrayOf("test")
            DATABASE_PACKAGES["org.springframework.beans.factory.annotation.Autowired"] = arrayOf("find", "save", "delete", "get")
            DATABASE_PACKAGES["org.springframework.stereotype.Service"] = arrayOf("find", "save", "delete", "get")
        }
    }
}