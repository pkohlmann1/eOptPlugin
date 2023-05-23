package com.github.pkohlmann1.testplugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import java.lang.instrument.Instrumentation
import java.lang.management.ManagementFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class CPUProfilerAgent : ApplicationComponent {

    override fun initComponent() {
        // Initialize and start the CPU profiler here
        premain(null, null)
    }

    override fun disposeComponent() {
        // Dispose or cleanup any resources here
    }

    override fun getComponentName(): String = "CPUProfilerAgent"

    companion object {

        fun getInstance(): CPUProfilerAgent =
            ApplicationManager.getApplication().getComponent(CPUProfilerAgent::class.java)

        fun premain(agentArgs: String?, inst: Instrumentation?) {
            val threadMXBean = ManagementFactory.getThreadMXBean()
            val mainThreadId = Thread.currentThread().id

            // Startzeitpunkt des Haupt-Threads erfassen
            val startTime = System.currentTimeMillis()

            for (i in 0 until 1000000000) {
                val x = i
                println(x)
            }

            // Endzeitpunkt des Haupt-Threads erfassen
            val endTime = System.currentTimeMillis()

            val threadCpuTime = threadMXBean.getThreadCpuTime(mainThreadId)
            val threadUserTime = threadMXBean.getThreadUserTime(mainThreadId)

            val cpuUsage = (threadCpuTime / 1000000).toDouble() // CPU-Nutzung in Millisekunden
            val userUsage = (threadUserTime / 1000000).toDouble() // Benutzerzeit in Millisekunden
            val elapsedTime = (endTime - startTime).toDouble() // Vergangene Zeit in Millisekunden

            val cpuUsagePercentage = (cpuUsage / elapsedTime) * 100

            println("CPU-Nutzung des Haupt-Threads: $cpuUsagePercentage%")
        }
    }
}
