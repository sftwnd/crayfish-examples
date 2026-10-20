package com.github.sftwnd.crayfish.examples.alarms.performance;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;

/**
 * The CPU time and the allocated bytes of a thread, the CPU time of the JVM and the time of its garbage collectors:
 * a snapshot, and the difference of two. A value that the JVM does not measure is negative.
 */
final class Usage {

    private static final ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();
    private static final OperatingSystemMXBean SYSTEM = ManagementFactory.getOperatingSystemMXBean();

    final long wallNanos;
    final long threadCpuNanos;
    final long allocatedBytes;
    final long processCpuNanos;
    final long gcMillis;

    private Usage(long wallNanos, long threadCpuNanos, long allocatedBytes, long processCpuNanos, long gcMillis) {
        this.wallNanos = wallNanos;
        this.threadCpuNanos = threadCpuNanos;
        this.allocatedBytes = allocatedBytes;
        this.processCpuNanos = processCpuNanos;
        this.gcMillis = gcMillis;
    }

    static Usage of(long threadId) {
        long allocated = THREADS instanceof com.sun.management.ThreadMXBean
                ? ((com.sun.management.ThreadMXBean) THREADS).getThreadAllocatedBytes(threadId)
                : -1L;
        long process = SYSTEM instanceof com.sun.management.OperatingSystemMXBean
                ? ((com.sun.management.OperatingSystemMXBean) SYSTEM).getProcessCpuTime()
                : -1L;
        long gc = 0L;
        for (GarbageCollectorMXBean collector : ManagementFactory.getGarbageCollectorMXBeans()) {
            gc += Math.max(0L, collector.getCollectionTime());
        }
        return new Usage(System.nanoTime(), THREADS.getThreadCpuTime(threadId), allocated, process, gc);
    }

    Usage since(Usage before) {
        return new Usage(
                this.wallNanos - before.wallNanos,
                difference(this.threadCpuNanos, before.threadCpuNanos),
                difference(this.allocatedBytes, before.allocatedBytes),
                difference(this.processCpuNanos, before.processCpuNanos),
                this.gcMillis - before.gcMillis);
    }

    /** The CPU time of the thread in percent of a core. */
    double threadCpuPercent() {
        return threadCpuNanos < 0 ? -1.0D : 100.0D * threadCpuNanos / wallNanos;
    }

    /** The CPU time of the JVM in cores. */
    double processCores() {
        return processCpuNanos < 0 ? -1.0D : (double) processCpuNanos / wallNanos;
    }

    private static long difference(long after, long before) {
        return after < 0 || before < 0 ? -1L : after - before;
    }

}
