package com.github.pkohlmann1.eOptPlugin.listeners
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ExecutionListener
import com.intellij.execution.runners.ExecutionEnvironment
import java.lang.management.ManagementFactory


class CPUUsageListener : ExecutionListener {

    override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        println("Process started")

        val threadMXBean = ManagementFactory.getThreadMXBean()
        val mainThreadId = Thread.currentThread().id
        var lastThreadCpuTime: Long = 0
        var lastTimestamp = System.currentTimeMillis()

        var cum_usage: Double = 0.0
        var iterations: Int = 0


        val cpuMonitoringTask = Runnable {
            Thread.sleep(60000)
            println("GOGOGOGO")
            while (!handler.isProcessTerminated && !Thread.currentThread().isInterrupted) {
                val totalCpuTime = threadMXBean.getThreadCpuTime(mainThreadId)
                val elapsedTime = System.currentTimeMillis() - lastTimestamp
                val cpuUsage = (totalCpuTime - lastThreadCpuTime) / elapsedTime.toDouble()
                lastThreadCpuTime = totalCpuTime
                lastTimestamp = System.currentTimeMillis()
                println("CPU Usage: $cpuUsage")

                Thread.sleep(10) // Eine Sekunde warten
                if (cpuUsage < 1000000 && cpuUsage > 0 && cpuUsage.toString() !== "Infinity") {
                    cum_usage += cpuUsage
                    iterations++
                }
            }
            println("AVG CPU Usage: " + cum_usage / iterations)
        }

        val cpuMonitoringThread = Thread(cpuMonitoringTask)
        cpuMonitoringThread.start()

        // Weitere Verarbeitung des gestarteten Prozesses...
    }



    override fun processStarting(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler){
        println("Process starting")
    }

}
