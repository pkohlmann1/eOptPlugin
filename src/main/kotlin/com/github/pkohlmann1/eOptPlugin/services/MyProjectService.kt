package com.github.pkohlmann1.eOptPlugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.github.pkohlmann1.eOptPlugin.MyBundle

@Service(Service.Level.PROJECT)
class MyProjectService(project: Project) {
    private val codeLocations: MutableList<CodeLocation> = mutableListOf()

    init {
        thisLogger().info(MyBundle.message("projectService", project.name))
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    fun getRandomNumber() = (1..100).random()

    fun addCodeLocation(file: String, method: String, variable: String) {
        val codeLocation = CodeLocation(file, method, variable)
        codeLocations.add(codeLocation)
    }

    fun getCodeLocations(): List<CodeLocation> {
        return codeLocations.toList()
    }

    fun hasCodeLocation(file: String, method: String, variable: String): Boolean {
        return codeLocations.any { it.file == file && it.method == method && it.variable == variable }
    }
}

data class CodeLocation(val file: String, val method: String, val variable: String)
