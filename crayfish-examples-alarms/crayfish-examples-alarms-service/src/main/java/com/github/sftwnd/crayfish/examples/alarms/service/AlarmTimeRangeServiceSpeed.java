package com.github.sftwnd.crayfish.examples.alarms.service;

import com.github.sftwnd.crayfish.alarms.service.AlarmTimeRangeService;
import com.github.sftwnd.crayfish.alarms.service.IAlarmService;
import com.github.sftwnd.crayfish.alarms.timerange.ITimeRange;
import com.github.sftwnd.crayfish.alarms.timerange.ITimeRangeFactory;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;

@Slf4j
public class AlarmTimeRangeServiceSpeed {

    private static final int THREADS = Math.max(4, Runtime.getRuntime().availableProcessors() >> 1);
    private static final int THREAD_REQ_SEQ = 1000000;
    private static final int BATCH_SIZE = 100;
    private static final int SECONDS = 8;

    private static final ITimeRangeFactory<Instant,Instant> timeRangeFactory = ITimeRangeFactory.temporal (
            Duration.ofSeconds(SECONDS),
            Duration.ofMillis(333),
            Duration.ofSeconds(1),
            Instant::compareTo
    );

    private static final AtomicReference<Instant> firstFire = new AtomicReference<>();
    private static final AtomicReference<Instant> lastFire = new AtomicReference<>();
    private static final AtomicLong firedAlarms = new AtomicLong();
    private static final AtomicLong delay = new AtomicLong();


    public static void main(String[] args) throws InterruptedException {

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        Instant startInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS).plusSeconds(24);
        ITimeRange<Instant,Instant> timeRange = timeRangeFactory.timeRange(startInstant);
        IAlarmService<Instant,Instant> service = new AlarmTimeRangeService<>(timeRange, Duration.ZERO);

        CountDownLatch serviceCdl = new CountDownLatch(1);
        Thread serviceThread = new Thread(() -> {
            serviceCdl.countDown();
            service.process(fired -> {
                if (firstFire.compareAndSet(null, Instant.now())) {
                    lastFire.compareAndSet(null, firstFire.get());
                    logger.info("First fire: {}", firstFire.get());
                }
                active.addAndGet(-fired.size());
                firedAlarms.addAndGet(fired.size());
                Instant now = Instant.now();
                fired.forEach(instant -> delay.addAndGet(Duration.between(instant, now).toNanos()));
                lastFire.updateAndGet(current -> current.isBefore(now) ? now : current);
            });
        });
        serviceThread.start();
        serviceCdl.await();

        logger.info("Start to fill");
        Instant start = Instant.now();
        IntStream.range(0, SECONDS)
                .mapToObj(startInstant::plusSeconds)
                .forEach(instant -> fillSecond(service, instant));
        logger.info("Filled[{}] for {}", added.get(), Duration.between(start, Instant.now()));

        for (long curr = active.get(); curr > 0 || !timeRange.isComplete(); curr = active.get()) {
            if (curr == active.get()) {
                LockSupport.parkNanos(333333333);
            }
        }
        LockSupport.parkNanos(Duration.ofSeconds(1).toNanos());
        logger.info("Threads: {}, register: {}, reject: {}, for {}", THREADS, added.get()- rejected.get(), rejected.get(), Duration.between(start, Instant.now()));
        Duration duration = Duration.between(firstFire.get(), lastFire.get());
        logger.info("Fired[{}] for {}", firedAlarms.get(), duration);
        logger.info("delay: {}, avg: {} sec", Duration.ofNanos(delay.get()), 1.0D * Math.round(1.0D * delay.get() / firedAlarms.get() / 100000.0D) / 10000.0D);
        logger.info("Alarm/sec: {}", Math.round(1000000000.0D * firedAlarms.get() / duration.toNanos()) );
    }

    private static final AtomicLong active = new AtomicLong();
    private static final AtomicLong added = new AtomicLong();
    private static final AtomicLong rejected = new AtomicLong();
    private static void fillSecond(IAlarmService<Instant, Instant> service, Instant instant) {
        long stepNanos = 1000000000 / THREADS / THREAD_REQ_SEQ;
        Collection<Instant> elements = new LinkedList<>();
        for (long nanos = 0; nanos < 1000000000; nanos += stepNanos) {
            elements.add(instant.plusNanos(nanos));
            if (elements.size() >= BATCH_SIZE) {
                int size = elements.size();
                added.addAndGet(elements.size());
                service.addElements(elements).thenAccept(reject -> {
                    active.addAndGet(size - reject.size());
                    rejected.addAndGet(reject.size());
                });
                elements = new LinkedList<>();
            }
        }
        int size = elements.size();
        added.addAndGet(elements.size());
        service.addElements(elements).thenAccept(reject -> {
            active.addAndGet(size - reject.size());
            rejected.addAndGet(reject.size());
        });
    }

}
