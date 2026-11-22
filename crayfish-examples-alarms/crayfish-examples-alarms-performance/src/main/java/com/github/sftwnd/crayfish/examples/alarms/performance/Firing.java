package com.github.sftwnd.crayfish.examples.alarms.performance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The consumer of the alarms of a step and its results: the delays of the alarms, the alarms fired twice and those
 * fired after an alarm due later, the alarms fired in the window of the full rate, and the samples of the fired
 * alarms that give the backlog. One thread fires the alarms, another one may take the samples.
 */
final class Firing {

    private static final long SAMPLE = 10_000L;

    private final Load.Plan plan;
    private final Delays delays = new Delays();
    private final BitSet ids;
    private final AtomicLong fired = new AtomicLong();
    private long duplicates;
    private long outOfOrder;
    private long lastDue = Long.MIN_VALUE;
    private long inWindow;
    private long lastFired;
    private long[] sampleMoments = new long[1024];
    private long[] sampleFired = new long[1024];
    private int samples;

    Firing(Load.Plan plan) {
        this.plan = plan;
        this.ids = new BitSet(plan.total);
    }

    /** Takes the alarms fired: the delay of an alarm is the time from its due moment to this call. */
    void accept(Collection<Alarm> alarms) {
        long now = Alarm.now();
        long count = 0L;
        for (Alarm alarm : alarms) {
            if (ids.get(alarm.id)) {
                duplicates++;
            } else {
                ids.set(alarm.id);
                count++;
                delays.record(now - alarm.dueMicros);
                if (alarm.dueMicros < lastDue) {
                    outOfOrder++;
                } else {
                    lastDue = alarm.dueMicros;
                }
            }
        }
        if (now >= plan.windowStart && now < plan.windowEnd) {
            inWindow += count;
        }
        if (count > 0) {
            lastFired = now;
        }
        fired.addAndGet(count);
    }

    long fired() {
        return fired.get();
    }

    /** Takes a sample of the fired alarms, at most one in 10 ms. */
    void sample(long now) {
        if (samples > 0 && now - sampleMoments[samples - 1] < SAMPLE) {
            return;
        }
        if (samples == sampleMoments.length) {
            sampleMoments = Arrays.copyOf(sampleMoments, samples * 2);
            sampleFired = Arrays.copyOf(sampleFired, samples * 2);
        }
        sampleMoments[samples] = now;
        sampleFired[samples] = fired.get();
        samples++;
    }

    /**
     * Prints the results of the step, unless the key is null.
     * @return true if the version kept up with the rate of the step
     */
    boolean report(String scenario, String key, long rejected, Usage usage, Load.Registrations registrations) {
        long firedAlarms = fired.get();
        long notFired = plan.total - rejected - firedAlarms;
        double throughput = inWindow * 1_000_000.0D / (plan.windowEnd - plan.windowStart);
        long p99 = delays.percentile(99.0D);
        boolean sustained = notFired == 0 && duplicates == 0
                && throughput >= 0.95D * plan.step.rate && p99 <= 100_000L;
        if (key == null) {
            return sustained;
        }
        // the due moments of the alarms registered: an alarm not registered has none
        long[] dues = Arrays.stream(plan.dues).filter(due -> due != 0L).sorted().toArray();
        List<Object> values = new ArrayList<>(Arrays.asList(
                "rate", plan.step.rate,
                "alarms", plan.total,
                "rejected", rejected,
                "notFired", notFired,
                "duplicates", duplicates,
                "outOfOrder", round(100.0D * outOfOrder / Math.max(1L, firedAlarms), 2),
                "throughput", Math.round(throughput),
                "p50", ms(delays.percentile(50.0D)),
                "p90", ms(delays.percentile(90.0D)),
                "p99", ms(p99),
                "p999", ms(delays.percentile(99.9D)),
                "max", ms(delays.max()),
                "mean", round(delays.mean() / 1_000.0D, 3),
                "late50", delays.above(50_000L),
                "backlog", backlog(dues),
                "drain", notFired == 0 && dues.length > 0 ? ms(lastFired - dues[dues.length - 1]) : "-1",
                "cpu", round(usage.threadCpuPercent(), 1),
                "cores", round(usage.processCores(), 2),
                "alloc", usage.allocatedBytes < 0 ? -1L : usage.allocatedBytes / Math.max(1L, firedAlarms),
                "gc", usage.gcMillis,
                "sustained", sustained));
        if (registrations != null) {
            values.addAll(Arrays.asList(
                    "regP50", ms(registrations.percentile(50.0D)),
                    "regP99", ms(registrations.percentile(99.0D)),
                    "regMax", ms(registrations.max()),
                    "failed", registrations.failed()));
        }
        Scenarios.result(scenario, key, values.toArray());
        return sustained;
    }

    /** The largest number of alarms due but not fired at the moment of a sample. */
    private long backlog(long[] dues) {
        long result = 0L;
        for (int i = 0; i < samples; i++) {
            result = Math.max(result, due(dues, sampleMoments[i]) - sampleFired[i]);
        }
        return result;
    }

    // the number of the alarms due at the moment
    private static long due(long[] dues, long moment) {
        int low = 0;
        int high = dues.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (dues[middle] <= moment) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    static String ms(long micros) {
        return Double.toString(micros / 1_000.0D);
    }

    static double round(double value, int digits) {
        double scale = Math.pow(10.0D, digits);
        return Math.round(value * scale) / scale;
    }

}
