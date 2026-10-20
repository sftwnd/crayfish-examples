package com.github.sftwnd.crayfish.examples.alarms.performance;

import com.github.sftwnd.crayfish.alarms.service.AlarmTimeRangeService;
import com.github.sftwnd.crayfish.alarms.timerange.ITimeRange;
import com.github.sftwnd.crayfish.alarms.timerange.ITimeRangeFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * Alarms registered at a constant rate, each one due a random lead after its registration, in calls of
 * {@code addElements} of 100 alarms. A step registers them for 4 s and waits until they are fired. The rates of a
 * scenario are doubled until a version does not keep up with them: its alarms per second fall below 95% of the rate,
 * the 99th percentile of the delays exceeds 100 ms, or an alarm is not fired 10 s after the last one is due.
 * The same seeds give the same alarms to every version.
 */
final class Load {

    private static final int[] TIME_RANGE_RATES = {12_500, 25_000, 50_000, 100_000, 200_000, 400_000, 800_000, 1_600_000};
    private static final int[] SERVICE_RATES = {6_250, 12_500, 25_000, 50_000, 100_000, 200_000, 400_000, 800_000, 1_600_000};
    private static final int SECONDS = 4;
    private static final long MIN_LEAD = 500_000L;
    private static final long MAX_LEAD = 1_500_000L;
    private static final int BATCH = 100;
    private static final int PRODUCERS = 4;
    private static final Duration INTERVAL = Duration.ofSeconds(1);
    private static final long DRAIN = 10_000_000L;

    // sparse: one alarm every 20 ms, due 10 to 100 ms after its registration
    private static final int SPARSE_RATE = 50;
    private static final int SPARSE_SECONDS = 20;
    private static final long SPARSE_MIN_LEAD = 10_000L;
    private static final long SPARSE_MAX_LEAD = 100_000L;

    private Load() {
    }

    /** A time range that one thread fills and extracts, as a process would, without a service. */
    static void timeRange() throws InterruptedException {
        ladder(TIME_RANGE_RATES, (rate, key) -> inOneThread(key, new Step(rate, SECONDS, MIN_LEAD, MAX_LEAD, BATCH, 1, null)));
    }

    /** An {@link AlarmTimeRangeService} with 4 threads that register the alarms, with the default minimalWait. */
    static void service() throws InterruptedException {
        ladder(SERVICE_RATES, (rate, key) -> byService("service", key, new Step(rate, SECONDS, MIN_LEAD, MAX_LEAD, BATCH, PRODUCERS, null)));
    }

    /**
     * 50 alarms per second, registered one by one, each due 10 to 100 ms later: the busy wait of minimalWait and a
     * registration that waits for the end of a wait, with the default minimalWait and with a minimalWait of zero.
     */
    static void sparse() throws InterruptedException {
        // the JIT compiles the code of the version before the measurement
        byService("sparse", null, new Step(SERVICE_RATES[0], 1, MIN_LEAD, MAX_LEAD, BATCH, PRODUCERS, null));
        byService("sparse", "default", new Step(SPARSE_RATE, SPARSE_SECONDS, SPARSE_MIN_LEAD, SPARSE_MAX_LEAD, 1, 1, null));
        byService("sparse", "zero", new Step(SPARSE_RATE, SPARSE_SECONDS, SPARSE_MIN_LEAD, SPARSE_MAX_LEAD, 1, 1, Duration.ZERO));
    }

    private static void ladder(int[] rates, Run run) throws InterruptedException {
        // a step that is not printed: the JIT compiles the code of the version before the measurement
        run.step(rates[0], null);
        for (int rate : rates) {
            if (!run.step(rate, Integer.toString(rate))) {
                break;
            }
        }
    }

    private static boolean inOneThread(String key, Step step) {
        Plan plan = new Plan(step);
        ITimeRange<Alarm, Alarm> range = plan.range();
        Firing firing = new Firing(plan);
        SplittableRandom random = new SplittableRandom(step.rate);
        long thread = Thread.currentThread().getId();
        int index = 0;
        long rejected = 0L;
        Alarm.parkUntil(plan.start);
        Usage before = Usage.of(thread);
        while (true) {
            long now = Alarm.now();
            for (; index < plan.batches && plan.batchAt(index) <= now; index++) {
                rejected += range.addElements(plan.alarms(index, random)).size();
            }
            Collection<Alarm> fired = range.extractFiredElements(Instant.now());
            if (!fired.isEmpty()) {
                firing.accept(fired);
            }
            now = Alarm.now();
            firing.sample(now);
            if (index == plan.batches && firing.fired() + rejected >= plan.total || now > plan.deadline) {
                break;
            }
            long next = index < plan.batches ? plan.batchAt(index) - now : Long.MAX_VALUE;
            long wait = Math.min(Math.min(next, range.duration(Instant.now()).toNanos() / 1_000L), 10_000L);
            if (wait > 0) {
                LockSupport.parkNanos(wait * 1_000L);
            }
        }
        Usage usage = Usage.of(thread).since(before);
        return firing.report("timerange", key, rejected, usage, null);
    }

    private static boolean byService(String scenario, String key, Step step) throws InterruptedException {
        Plan plan = new Plan(step);
        AlarmTimeRangeService<Alarm, Alarm> service = new AlarmTimeRangeService<>(plan.range(), step.minimalWait);
        Firing firing = new Firing(plan);
        Registrations registrations = new Registrations();
        // a process of 1.0.4 that has not caught up ignores its interrupt while it works: it must not keep the JVM
        Thread process = new Thread(() -> service.process(firing::accept), "alarms-process");
        process.setDaemon(true);
        process.start();
        Thread[] producers = new Thread[step.producers];
        for (int i = 0; i < producers.length; i++) {
            int first = i;
            producers[i] = new Thread(() -> register(service, plan, first, registrations), "alarms-producer-" + i);
            producers[i].setDaemon(true);
        }
        Alarm.parkUntil(plan.start);
        Usage before = Usage.of(process.getId());
        for (Thread producer : producers) {
            producer.start();
        }
        for (long now = Alarm.now(); now <= plan.deadline && !done(producers, firing, registrations, plan); now = Alarm.now()) {
            firing.sample(now);
            LockSupport.parkNanos(10_000_000L);
        }
        firing.sample(Alarm.now());
        Usage usage = Usage.of(process.getId()).since(before);
        process.interrupt();
        process.join(5_000L);
        boolean sustained = firing.report(scenario, key, registrations.rejected(), usage, registrations);
        return sustained && !process.isAlive();
    }

    private static void register(AlarmTimeRangeService<Alarm, Alarm> service, Plan plan, int first, Registrations registrations) {
        SplittableRandom random = new SplittableRandom(31L * plan.step.rate + first);
        for (int index = first; index < plan.batches; index += plan.step.producers) {
            Alarm.parkUntil(plan.batchAt(index));
            List<Alarm> alarms = plan.alarms(index, random);
            long called = System.nanoTime();
            service.addElements(alarms).whenComplete((rejected, error) -> registrations.completed(called, alarms.size(), rejected, error));
        }
    }

    private static boolean done(Thread[] producers, Firing firing, Registrations registrations, Plan plan) {
        for (Thread producer : producers) {
            if (producer.isAlive()) {
                return false;
            }
        }
        return firing.fired() + registrations.rejected() >= plan.total;
    }

    @FunctionalInterface
    private interface Run {
        /** Runs a step and prints its results unless the key is null; true if the version kept up. */
        boolean step(int rate, String key) throws InterruptedException;
    }

    /** The parameters of a step; the leads are in microseconds, a null minimalWait is the default of the service. */
    static final class Step {

        final int rate;
        final int seconds;
        final long minLead;
        final long maxLead;
        final int batch;
        final int producers;
        final Duration minimalWait;

        Step(int rate, int seconds, long minLead, long maxLead, int batch, int producers, Duration minimalWait) {
            this.rate = rate;
            this.seconds = seconds;
            this.minLead = minLead;
            this.maxLead = maxLead;
            this.batch = batch;
            this.producers = producers;
            this.minimalWait = minimalWait;
        }

    }

    /**
     * The schedule of a step in microseconds of the epoch: the calls of addElements from {@code start}, the window
     * in which the alarms are due at the full rate, the deadline, and the due moments of the alarms by their numbers.
     */
    static final class Plan {

        final Step step;
        final int batches;
        final int total;
        final long start;
        final long windowStart;
        final long windowEnd;
        final long deadline;
        final long[] dues;
        private final Instant rangeStart;
        private final Duration rangeDuration;

        Plan(Step step) {
            this.step = step;
            this.batches = (int) ((long) step.rate * step.seconds / step.batch);
            this.total = batches * step.batch;
            this.start = Alarm.now() + 200_000L;
            this.windowStart = start + step.maxLead;
            this.windowEnd = start + step.seconds * 1_000_000L + step.minLead;
            long last = start + step.seconds * 1_000_000L + step.maxLead;
            this.deadline = last + DRAIN;
            this.dues = new long[total];
            long second = Math.floorDiv(start, 1_000_000L);
            this.rangeStart = Instant.ofEpochSecond(second);
            this.rangeDuration = Duration.ofSeconds(Math.floorDiv(last, 1_000_000L) - second + 2);
        }

        /** A range from the second of the start to 2 s after the last alarm, in chunks of 1 s. */
        ITimeRange<Alarm, Alarm> range() {
            return ITimeRangeFactory.create(rangeDuration, INTERVAL, Duration.ofSeconds(1), Alarm::due, null)
                    .timeRange(rangeStart);
        }

        long batchAt(int index) {
            return start + (long) index * step.batch * 1_000_000L / step.rate;
        }

        List<Alarm> alarms(int index, SplittableRandom random) {
            long at = batchAt(index);
            List<Alarm> alarms = new ArrayList<>(step.batch);
            for (int i = 0, id = index * step.batch; i < step.batch; i++, id++) {
                long due = at + step.minLead + random.nextLong(step.maxLead - step.minLead + 1);
                dues[id] = due;
                alarms.add(new Alarm(id, due));
            }
            return alarms;
        }

    }

    /** The calls of addElements of the service: their times to the completion of the stage, and the rejected alarms. */
    static final class Registrations {

        private final Delays delays = new Delays();
        private final AtomicLong rejected = new AtomicLong();
        private final AtomicLong failed = new AtomicLong();

        // the stage completes in the thread of the process, or in the producer if it is complete before whenComplete
        void completed(long calledNanos, int size, Collection<Alarm> rejectedAlarms, Throwable error) {
            long micros = (System.nanoTime() - calledNanos) / 1_000L;
            synchronized (delays) {
                delays.record(micros);
            }
            if (error != null) {
                failed.addAndGet(size);
                rejected.addAndGet(size);
            } else {
                rejected.addAndGet(rejectedAlarms.size());
            }
        }

        long rejected() {
            return rejected.get();
        }

        long failed() {
            return failed.get();
        }

        long percentile(double percent) {
            synchronized (delays) {
                return delays.percentile(percent);
            }
        }

        long max() {
            synchronized (delays) {
                return delays.max();
            }
        }

    }

}
