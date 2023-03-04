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
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;

@Slf4j
public class AlarmTimeRangeServiceParallelAdd {

    private static final int THREADS = Math.max(4, Runtime.getRuntime().availableProcessors() - (Runtime.getRuntime().availableProcessors() >> 1));
    private static final int STEPS = 10000;
    private static final int BATCH_SIZE = 100;
    private static final int RANGE_SECONDS = 7;
    private static final long STEP_NANOS = Duration.ofSeconds(RANGE_SECONDS).toNanos() / BATCH_SIZE / STEPS / THREADS;

    private static final AtomicInteger nanoMove = new AtomicInteger();
    private static final ITimeRangeFactory<Date,String> timeRangeFactory = ITimeRangeFactory.create (
            Duration.ofSeconds(RANGE_SECONDS),
            Duration.ofMillis(125),
            Duration.ofSeconds(1),
            date -> Instant.ofEpochMilli(date.getTime()).plusNanos(nanoMove.incrementAndGet()),
            instant -> instant,
            Instant::toString,
            Instant::compareTo
    );

    public static void main(String[] args) throws InterruptedException {

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        Instant rangeInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant startInstant = rangeInstant.plusSeconds(1);
        ITimeRange<Date,String> timeRange = timeRangeFactory.timeRange(startInstant);
        IAlarmService<Date,String> service = new AlarmTimeRangeService<>(timeRange, null);

        CountDownLatch scdl = new CountDownLatch(THREADS);
        CountDownLatch rcdl = new CountDownLatch(1);
        CountDownLatch ecdl = new CountDownLatch(THREADS + 1);

        AtomicInteger rejected = new AtomicInteger(0);
        AtomicInteger added = new AtomicInteger(0);
        AtomicInteger generated = new AtomicInteger(0);
        AtomicInteger fired = new AtomicInteger(0);

        AtomicLong nanos = new AtomicLong(0);

        LongStream.range(0, THREADS).mapToObj(ii -> new Thread(() -> {
            scdl.countDown();
            try {
                logger.info("Start addElement thread: {}", Thread.currentThread().getName());
                try { rcdl.await(); } catch (InterruptedException e) { throw new RuntimeException(e); }
                logger.info("Process addElement on thread: {}", Thread.currentThread().getName());
                for (int i = 0; i <= STEPS; i++) {
                    Collection<Date> list = new LinkedList<>();
                    for (int j = 0; j < BATCH_SIZE; j++) {
                        list.add(
                            new Date(
                                startInstant.plusNanos(
                                    nanos.getAndAdd(STEP_NANOS)
                                ).toEpochMilli()
                            )
                        );
                    }
                    generated.addAndGet(list.size());
                    service.addElements(list).thenAccept(reject -> {
                        rejected.addAndGet(reject.size());
                        added.addAndGet(list.size()- reject.size());
                    });
                }
                logger.info("Stop on thread: {}", Thread.currentThread().getName());
            } finally {
                ecdl.countDown();
            }
        })).peek(thread -> thread.setDaemon(true))
           .forEach(Thread::start);

        Thread serviceThread = new Thread(() -> {
            try {
                scdl.await();
                rcdl.countDown();
                service.process(fires -> {
                    int alarms = fired.addAndGet(fires.size());
                    if (alarms == THREADS * BATCH_SIZE * (STEPS+1) - rejected.get()) {
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                ecdl.countDown();
            }
        });
        serviceThread.setDaemon(true);
        serviceThread.start();
        ecdl.await();

        logger.info("Generated: {}, added: {}, fired: {}, rejected: {}", generated.get(), added.get(), fired.get(), rejected.get());
        logger.info("Completed...");
    }

}
