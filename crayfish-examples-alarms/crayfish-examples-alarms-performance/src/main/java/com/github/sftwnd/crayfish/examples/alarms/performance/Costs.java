package com.github.sftwnd.crayfish.examples.alarms.performance;

import com.github.sftwnd.crayfish.alarms.timerange.ITimeRange;
import com.github.sftwnd.crayfish.alarms.timerange.ITimeRangeFactory;
import com.github.sftwnd.crayfish.common.expectation.TemporalExtractor;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The costs of a time range in one thread, without waits: the time of addElements per alarm, the time of an
 * extraction that fires one alarm while the others wait, the alarms that extractions every 10 ms fire per second of
 * work, and the calls of the expectation. The range is one of 2030 and the extractions get their moments, so the
 * clock plays no part. Every measure is the median of its rounds after the rounds of the warm-up.
 */
final class Costs {

    private static final Instant START = Instant.parse("2030-01-01T00:00:00Z");
    private static final long START_MICROS = Alarm.micros(START);
    private static final Duration SECOND = Duration.ofSeconds(1);
    private static final Duration MINUTE = Duration.ofSeconds(60);
    private static final int ADDED = 200_000;
    private static final int DRAINED = 100_000;
    private static final int BATCH = 100;
    private static final int EXTRACTIONS = 20;

    private Costs() {
    }

    static void run() {
        add(SECOND);
        add(MINUTE);
        for (Duration interval : Arrays.asList(SECOND, MINUTE)) {
            extract(interval, 10_000);
            extract(interval, 100_000);
        }
        drain();
        calls();
    }

    /** The time of addElements per alarm: 200,000 alarms at random moments of the minute of the range, 100 per call. */
    private static void add(Duration interval) {
        List<List<Alarm>> batches = batches(random(ADDED, MINUTE));
        long[] times = new long[7];
        for (int round = -5; round < times.length; round++) {
            ITimeRange<Alarm, Alarm> range = range(interval, Alarm::due);
            long start = System.nanoTime();
            for (List<Alarm> batch : batches) {
                range.addElements(batch);
            }
            long time = System.nanoTime() - start;
            if (round >= 0) {
                times[round] = time;
            }
        }
        Scenarios.result("cost", "add-" + name(interval), "nsPerAlarm", Firing.round(median(times) / ADDED, 1));
    }

    /**
     * The time of an extraction that fires one alarm while the others wait in the range: the alarms are spread evenly
     * over 50 s from 0.5 s after the start, and every extraction is at the moment of the next alarm.
     */
    private static void extract(Duration interval, int waiting) {
        List<Alarm> alarms = new ArrayList<>(waiting);
        long step = 50_000_000L / waiting;
        for (int i = 0; i < waiting; i++) {
            alarms.add(new Alarm(i, START_MICROS + 500_000L + i * step));
        }
        long[] times = new long[3 * EXTRACTIONS];
        long wrong = 0L;
        for (int round = -2; round < 3; round++) {
            ITimeRange<Alarm, Alarm> range = range(interval, Alarm::due);
            range.addElements(alarms);
            for (int i = 0; i < EXTRACTIONS; i++) {
                Instant moment = alarms.get(i).due;
                long start = System.nanoTime();
                int fired = range.extractFiredElements(moment).size();
                long time = System.nanoTime() - start;
                if (fired != 1) {
                    wrong++;
                }
                if (round >= 0) {
                    times[round * EXTRACTIONS + i] = time;
                }
            }
        }
        Scenarios.result("cost", "extract-" + name(interval) + "-" + waiting,
                "usPerExtraction", Firing.round(median(times) / 1_000.0D, 2), "wrong", wrong);
    }

    /**
     * The alarms per second of work that extractions every 10 ms fire: 100,000 alarms at random moments of 20 s in
     * chunks of 1 s, and the extractions 10 ms apart; the time of the additions is not counted.
     */
    private static void drain() {
        List<List<Alarm>> batches = batches(random(DRAINED, Duration.ofSeconds(20)));
        long[] times = new long[3];
        long fired = 0L;
        for (int round = -2; round < times.length; round++) {
            ITimeRange<Alarm, Alarm> range = range(SECOND, Alarm::due);
            for (List<Alarm> batch : batches) {
                range.addElements(batch);
            }
            fired = 0L;
            long start = System.nanoTime();
            for (long at = 10_000L; at <= 20_010_000L; at += 10_000L) {
                fired += range.extractFiredElements(Alarm.instant(START_MICROS + at)).size();
            }
            long time = System.nanoTime() - start;
            if (round >= 0) {
                times[round] = time;
            }
        }
        double nanos = median(times);
        Scenarios.result("cost", "drain-1s", "alarmsPerSecond", Math.round(DRAINED * 1e9D / nanos),
                "nsPerAlarm", Firing.round(nanos / DRAINED, 1), "fired", fired);
    }

    /** The calls of the expectation per alarm added and per extraction in the middle of a chunk of 1 s. */
    private static void calls() {
        AtomicLong calls = new AtomicLong();
        TemporalExtractor<Alarm, Instant> counting = alarm -> {
            calls.incrementAndGet();
            return alarm.due;
        };
        ITimeRange<Alarm, Alarm> range = range(SECOND, counting);
        for (List<Alarm> batch : batches(random(ADDED, MINUTE))) {
            range.addElements(batch);
        }
        double perAdded = (double) calls.get() / ADDED;
        calls.set(0L);
        for (int i = 0; i < EXTRACTIONS; i++) {
            range.extractFiredElements(Alarm.instant(START_MICROS + 500_000L + i * 10_000L));
        }
        Scenarios.result("cost", "calls", "perAdded", Firing.round(perAdded, 1),
                "perExtraction", Math.round((double) calls.get() / EXTRACTIONS));
    }

    private static ITimeRange<Alarm, Alarm> range(Duration interval, TemporalExtractor<Alarm, Instant> expectation) {
        return ITimeRangeFactory.create(MINUTE, interval, SECOND, expectation, null).timeRange(START);
    }

    private static List<Alarm> random(int count, Duration span) {
        SplittableRandom random = new SplittableRandom(count);
        long micros = span.toNanos() / 1_000L;
        List<Alarm> alarms = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            alarms.add(new Alarm(i, START_MICROS + random.nextLong(micros)));
        }
        return alarms;
    }

    private static List<List<Alarm>> batches(List<Alarm> alarms) {
        List<List<Alarm>> batches = new ArrayList<>();
        for (int from = 0; from < alarms.size(); from += BATCH) {
            batches.add(new ArrayList<>(alarms.subList(from, Math.min(alarms.size(), from + BATCH))));
        }
        return batches;
    }

    private static double median(long[] values) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        int middle = sorted.length / 2;
        return sorted.length % 2 == 1 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2.0D;
    }

    private static String name(Duration interval) {
        return interval.getSeconds() + "s";
    }

}
