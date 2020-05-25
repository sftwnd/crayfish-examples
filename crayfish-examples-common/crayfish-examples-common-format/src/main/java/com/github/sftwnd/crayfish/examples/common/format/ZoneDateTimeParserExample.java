package com.github.sftwnd.crayfish.examples.common.format;

import com.github.sftwnd.crayfish.common.exception.Processor;
import com.github.sftwnd.crayfish.common.format.parser.ZonedDateTimeParser;
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
public class ZoneDateTimeParserExample {

    public static void main(String[] args) {

        ZonedDateTimeParser parser = new ZonedDateTimeParser(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Instant instant = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String parseString="2020-02-21T03:17:34";
        logger.info("Parsed string: {}",parseString);
        ZonedDateTime zdt = parser.parse(parseString);
        logger.info("ZoneDateTime for default zoneId({}): {}", ZonedDateTimeParser.DEFAUT_ZONE_ID, parser.parse(parseString));
        ZoneId defaultZoneId = ZoneId.systemDefault();
        parser.setDefaultZoneId(defaultZoneId);
        logger.info("parser.setDefaultZoneId({})", defaultZoneId);
        logger.info("ZoneDateTime with default zoneId({}): {}", parser.getZoneId(), parser.parse(parseString));
        ZoneId currentZoneId = ZoneId.of("Asia/Novosibirsk");
        parser.setCurrentZoneId(currentZoneId);
        logger.info("parser.setCurrentZoneId({})", currentZoneId);
        logger.info("ZoneDateTime with current zoneId({}): {}", currentZoneId, parser.parse(parseString));
        process(() -> logger.info("[other thread] ZoneDateTime don't use setted current zoneId({}) use default zoneId({}): {}", currentZoneId, parser.getZoneId(), parser.parse(parseString)));

        logger.info("ZonedDateTimeParser.register(...) works like TemporalFormatter - see TemporalFormatterExample");
    }

    @SneakyThrows
    private static <E extends Exception> void process(Processor<E> processor) {
        CountDownLatch cdl = new CountDownLatch(1);
        new Thread(() -> wrapUncheckedExceptions(() -> { processor.process(); cdl.countDown(); })).start();
        cdl.await();
    }

}
