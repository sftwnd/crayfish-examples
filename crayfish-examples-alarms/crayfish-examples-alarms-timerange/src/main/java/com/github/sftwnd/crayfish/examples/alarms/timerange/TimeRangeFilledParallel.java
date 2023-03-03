package com.github.sftwnd.crayfish.examples.alarms.timerange;

import com.github.sftwnd.crayfish.alarms.timerange.ITimeRange;
import com.github.sftwnd.crayfish.alarms.timerange.ITimeRangeFactory;
import edu.umd.cs.findbugs.annotations.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class TimeRangeFilledParallel {

    private static final int THREAD_ALARMS_PER_SEQ = 750000;
    private static final int THREADS = Runtime.getRuntime().availableProcessors() >>> 1;
    private static final int SECONDS = 17;
    private static final AtomicInteger fires = new AtomicInteger(0);
    private static final AtomicInteger fired = new AtomicInteger(0);
    private static final AtomicInteger filled = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {

        ITimeRangeFactory<Instant, Instant> timeRangeFactory = ITimeRangeFactory.temporal(
                Duration.ofSeconds(SECONDS),
                Duration.ofMillis(125),
                Duration.ofSeconds(2),
                Instant::compareTo
        );
        Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS).plusSeconds(20);
        Collection<ITimeRange<Instant,Instant>> timeRanges = IntStream.range(0, THREADS)
                .mapToObj(i -> timeRangeFactory.timeRange(start))
                .collect(Collectors.toList());
        fill(timeRanges);
        process(timeRanges,Duration.ofMillis(165));
        logger.info("Filled: {}", filled.get());
        logger.info("Min delay: {}", minDelay.get());
        logger.info("Max delay: {}", maxDelay.get());
        logger.info("First alarm received at: {}", firstInstant.get());
        logger.info("Last alarm received at: {}", lastInstant.get());
        logger.info("Alarms: {}", fired.get());
        logger.info("Fires: {}", fires.get());
        logger.info("Avg fire duration: {} sec", 1.0D * Math.round(1.0D * setDuration.get().toNanos() / fires.get() / 100000.0D) / 10000.0D );
        logger.info("Avg delay: {} sec", 1.0D * Math.round(1.0D * completeDelay.get().toNanos() / fired.get() / 100000.0D) / 10000.0D);
        logger.info("Avg: {} alarm/sec",
                Math.round(100000.0D * fired.get() / Duration.between(firstInstant.get(), lastInstant.get()).toMillis()) / 100.0D
        );
    }

    private static final AtomicReference<Duration> minDelay = new AtomicReference<>(null);
    private static final AtomicReference<Duration> maxDelay = new AtomicReference<>(null);
    private static final AtomicReference<Instant> firstInstant = new AtomicReference<>(null);
    private static final AtomicReference<Instant> lastInstant = new AtomicReference<>(null);
    private static final AtomicReference<Duration> setDuration = new AtomicReference<>(Duration.ZERO);
    private static final AtomicReference<Duration> completeDelay = new AtomicReference<>(Duration.ZERO);


    private static void fireAlarms(@NonNull Collection<Instant> alarms) {
        Instant now = Instant.now();
        fires.incrementAndGet();
        fired.addAndGet(alarms.size());
        Instant firstAlarmInstant = Objects.requireNonNull(alarms.stream().min(Comparator.naturalOrder()).orElse(now));
        Instant lastAlarmInstant = Objects.requireNonNull(alarms.stream().max(Comparator.naturalOrder()).orElse(now));
        Duration firstAlarmDelay = Duration.between(firstAlarmInstant, now);
        Duration lastAlarmDelay = Duration.between(lastAlarmInstant, now);
        setDuration.updateAndGet(current -> current.plus(Duration.between(firstAlarmInstant, lastAlarmInstant)));
        firstInstant.compareAndSet(null, firstAlarmInstant);
        lastInstant.updateAndGet(current -> current == null || current.isBefore(lastAlarmInstant) ? lastAlarmInstant : current);
        minDelay.updateAndGet(current -> current == null || firstAlarmDelay.compareTo(current) < 0 ? firstAlarmDelay : current);
        maxDelay.updateAndGet(current -> current == null || lastAlarmDelay.compareTo(current) > 0 ? lastAlarmDelay : current);
        completeDelay.updateAndGet(current ->
                current.plus(alarms.stream()
                                .map(instant -> Duration.between(instant, now))
                                .reduce(Duration.ZERO, Duration::plus)));
    }

    private static void process(@NonNull Collection<ITimeRange<Instant, Instant>> timeRanges, @Nonnull Duration delay) throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(timeRanges.size());
        logger.info("Start to process: {}", Instant.now());
        timeRanges.stream().map(timeRange -> new Thread(() -> {
            process(timeRange, delay);
            cdl.countDown();
        })).forEach(Thread::start);
        cdl.await();
        logger.info("Stop processing: {}", Instant.now());
    }

    private static void process(@NonNull ITimeRange<Instant, Instant> timeRange, @Nonnull Duration delay) {
        Duration waitFor = timeRange.duration(delay);
        if (waitFor.compareTo(delay) > 0) {
            logger.info("Wait for: {} in thread: {}", waitFor, Thread.currentThread().getName());
            LockSupport.parkNanos(waitFor.toNanos());
        }
        logger.info("Start process at {}, thread: {}", Instant.now(), Thread.currentThread().getName());
        while (!timeRange.isComplete()) {
            Collection<Instant> alarms = timeRange.extractFiredElements(Optional.of(delay).map(Instant.now()::plus).orElseGet(Instant::now));
            if (!alarms.isEmpty()) {
                fireAlarms(alarms);
            }
            Duration duration = timeRange.duration(delay);
            if (duration.compareTo(delay) > 0) {
                LockSupport.parkNanos(duration.toNanos());
            }
        }
        logger.info("Stop process at {}, thread: {}", Instant.now(), Thread.currentThread().getName());
    }

    private static void fill(@NonNull Collection<ITimeRange<Instant, Instant>> timeRanges) throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(timeRanges.size());
        logger.info("Start to fill: {}", Instant.now());
        timeRanges.stream().map(timeRange -> new Thread(() -> {
            fill(timeRange);
            cdl.countDown();
        })).forEach(Thread::start);
        cdl.await();
        logger.info("Stop filling: {}", Instant.now());
    }


    private static void fill(@NonNull ITimeRange<Instant, Instant> timeRange) {
        logger.info("Start to fill: {}", Thread.currentThread().getName());
        List<Instant> elements = new LinkedList<>();
        Duration add = Duration.ofNanos(TimeUnit.SECONDS.toNanos(1)/THREAD_ALARMS_PER_SEQ);
        for (Instant instant = timeRange.getStartInstant();
             instant.isBefore(timeRange.getLastInstant());
             instant = instant.plus(add)) {
            elements.add(instant);
            filled.incrementAndGet();
            if (elements.size() >= 13) {
                timeRange.addElements(elements);
                elements = new LinkedList<>();
            }
        }
        if (!elements.isEmpty()) {
            timeRange.addElements(elements);
        }
        logger.info("Stop filling: {}", Thread.currentThread().getName());
    }

}
