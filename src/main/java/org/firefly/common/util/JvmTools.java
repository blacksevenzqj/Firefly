package org.firefly.common.util;

import com.sun.management.HotSpotDiagnosticMXBean;
import org.firefly.common.util.constant.JConstants;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class JvmTools {

    /**
     * Returns java stack traces of java threads for the current java process.
     */
    public static List<String> jStack() throws Exception {
        List<String> stackList = new LinkedList<>();
        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] stackTraces = entry.getValue();

            stackList.add(
                    String.format(
                            "\"%s\" tid=%s isDaemon=%s priority=%s" + JConstants.NEWLINE,
                            thread.getName(),
                            thread.getId(),
                            thread.isDaemon(),
                            thread.getPriority()
                    )
            );

            stackList.add("java.lang.Thread.State: " + thread.getState() + JConstants.NEWLINE);

            if (stackTraces != null) {
                for (StackTraceElement s : stackTraces) {
                    stackList.add("    " + s.toString() + JConstants.NEWLINE);
                }
            }
        }
        return stackList;
    }

    /**
     * Returns memory usage for the current java process.
     */
    public static List<String> memoryUsage() throws Exception {
        MemoryUsage heapMemoryUsage = MXBeanHolder.memoryMxBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = MXBeanHolder.memoryMxBean.getNonHeapMemoryUsage();

        List<String> memoryUsageList = new LinkedList<>();
        memoryUsageList.add("********************************** Memory Usage **********************************" + JConstants.NEWLINE);
        memoryUsageList.add("Heap Memory Usage: " + heapMemoryUsage.toString() + JConstants.NEWLINE);
        memoryUsageList.add("NonHeap Memory Usage: " + nonHeapMemoryUsage.toString() + JConstants.NEWLINE);

        return memoryUsageList;
    }

    /**
     * Returns the heap memory used for the current java process.
     */
    public static double memoryUsed() throws Exception {
        MemoryUsage heapMemoryUsage = MXBeanHolder.memoryMxBean.getHeapMemoryUsage();
        return (double) (heapMemoryUsage.getUsed()) / heapMemoryUsage.getMax();
    }

    /**
     * Dumps the heap to the outputFile file in the same format as the hprof heap dump.
     * @param outputFile    the system-dependent filename
     * @param live          if true dump only live objects i.e. objects that are reachable from others
     */
    @SuppressWarnings("all")
    public static void jMap(String outputFile, boolean live) throws Exception {
        File file = new File(outputFile);
        if (file.exists()) {
            file.delete();
        }
        MXBeanHolder.hotSpotDiagnosticMxBean.dumpHeap(outputFile, live);
    }

    private static class MXBeanHolder {
        static final MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();
        static final HotSpotDiagnosticMXBean hotSpotDiagnosticMxBean =
                ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
    }
}
