package com.github.pkohlmann1.testplugin;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CPUProfilerAgent {

    private static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private static long lastThreadCpuTime = 0;
    private static long lastTimestamp = 0;

    public static void premain(String agentArgs, Instrumentation inst) {
        // register a shutdown hook to report CPU usage on JVM exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            long totalCpuTime = 0;
            for (long threadId : threadMXBean.getAllThreadIds()) {
                totalCpuTime += threadMXBean.getThreadCpuTime(threadId);
            }
            double cpuUsage = (totalCpuTime - lastThreadCpuTime) / (double) (System.currentTimeMillis() - lastTimestamp);
            System.out.println("CPU Usage: " + cpuUsage);
        }));

        // start a background thread to periodically update the CPU usage
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            long totalCpuTime = 0;
            for (long threadId : threadMXBean.getAllThreadIds()) {
                totalCpuTime += threadMXBean.getThreadCpuTime(threadId);
            }
            lastTimestamp = System.currentTimeMillis();
            double cpuUsage = (totalCpuTime - lastThreadCpuTime) / (double) (lastTimestamp - lastThreadCpuTime);
            lastThreadCpuTime = totalCpuTime;
            System.out.println("CPU Usage: " + cpuUsage);
        }, 0, 1, TimeUnit.SECONDS);
    }
}
