package com.github.pkohlmann1.testplugin.listeners

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener

internal class MyToolWindowListener() : ToolWindowManagerListener {

    override fun stateChanged(toolWindowManager: ToolWindowManager) {
        this.thisLogger().warn(toolWindowManager.toString())
    }
}