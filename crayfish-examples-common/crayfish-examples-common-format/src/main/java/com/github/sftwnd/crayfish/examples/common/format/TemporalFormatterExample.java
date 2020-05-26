package com.github.sftwnd.crayfish.examples.common.format;

import com.github.sftwnd.crayfish.common.exception.Processor;
import com.github.sftwnd.crayfish.common.format.formatter.TemporalFormatter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;

import static com.github.sftwnd.crayfish.common.exception.ExceptionUtils.wrapUncheckedExceptions;

@Slf4j
public class TemporalFormatterExample {

    public static void main(String[] args) {

        TemporalFormatter temporalFormatter = new TemporalFormatter(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Instant instant = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        logger.info("Current time with timezone: {}", DateTimeFormatter.ISO_ZONED_DATE_TIME.format(ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())));
        logger.info("DateTime without zone info with TemporalFormatter.DEFAUT_ZONE_ID({}): {}", TemporalFormatter.DEFAUT_ZONE_ID, temporalFormatter.format(instant));
        ZoneId defaultZoneId = ZoneId.of("Europe/Kaliningrad");
        temporalFormatter.setDefaultZoneId(defaultZoneId);
        logger.info("temporalFormatter.setDefaultZoneId({}) - used for any thread", defaultZoneId);
        logger.info("DateTime with defaultZoneId({}): {}", defaultZoneId, temporalFormatter.format(instant));
        process(() -> logger.info("[Other thread] DateTime use defaultZoneId({}): {}", defaultZoneId, temporalFormatter.format(instant)));
        ZoneId currentZoneId = ZoneId.of("Asia/Novosibirsk");
        temporalFormatter.setCurrentZoneId(currentZoneId);
        logger.info("temporalFormatter.setCurrentZoneId({}) - used only for current thread", currentZoneId);
        logger.info("DateTime with setCurrentZoneId({})- applied: {}", currentZoneId, temporalFormatter.format(instant));
        process(() -> logger.info("[Other thread] DateTime with setCurrentZoneId({}) not applied - default used: {}", defaultZoneId, temporalFormatter.format(instant)));

        Object obj = Object.class;
        TemporalFormatter.register(obj, () -> new TemporalFormatter(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        ZoneId zoneId1 = ZoneId.of("Europe/Rome");
        TemporalFormatter.formatter(obj).setDefaultZoneId(zoneId1);
        logger.info("TemporalFormatter(obj) with zoneId({}) result is: {}", zoneId1, TemporalFormatter.formatter(obj).format(instant));

    }

    @SneakyThrows
    private static <E extends Exception> void process(Processor<E> processor) {
        CountDownLatch cdl = new CountDownLatch(1);
        new Thread(() -> wrapUncheckedExceptions(() -> { processor.process(); cdl.countDown(); })).start();
        cdl.await();
    }

}
