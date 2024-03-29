package com.github.pkohlmann1.eOptPlugin.listeners.deprecated

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent


internal class MyVfsListener() : BulkFileListener {

    val code_old : String = ""
    val code_new : String = ""

    override fun before(events: MutableList<out VFileEvent>) {
        thisLogger().warn(events[0].toString())
        thisLogger().warn(events[0].hashCode().toString())
    }

    override fun after(events: MutableList<out VFileEvent>) {
        thisLogger().warn(events.toString())
        thisLogger().warn(events[0].hashCode().toString())
}
}
