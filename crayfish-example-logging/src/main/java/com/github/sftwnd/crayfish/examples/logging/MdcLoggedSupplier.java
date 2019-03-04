package com.github.sftwnd.crayfish.examples.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.sftwnd.crayfish.utils.logging.Slf4jMdcUtility;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class MdcLoggedSupplier {

    private static Logger logger = LoggerFactory.getLogger(MdcLoggedSupplier.class);

    public static void main(String[] args) throws InterruptedException {
        getMeTheValue();
        Arrays.asList(new String[][]{{"PAY", "13456"}, {"MSG", "65431"}})
             .forEach((record) -> Slf4jMdcUtility.supplier(record[0], record[1], MdcLoggedSupplier::getMeTheValue).get());
        Arrays.asList(new Integer[]{111,222}).forEach(MdcLoggedSupplier::getMeTheN);
        Function<Integer, Object> s = MdcLoggedSupplier::getTheValS;
        System.out.println(s.apply(100));
        s = MdcLoggedSupplier::getTheValD;
        System.out.println(s.apply(200));

        Arrays.asList(new Integer[] {1, 2, 3}).forEach(System.out::println);


    }

    private static final AtomicLong atomicLong = new AtomicLong();

    public static int getMeTheValue() {
        logger.info("[{}] TimeMessage: {}", atomicLong.incrementAndGet(), Instant.now());
        return 0;
    }

    public static int getMeTheN(final int n) {
        return Slf4jMdcUtility.supplier(
                  "PAY"
                  ,String.valueOf(n)
                  ,  () -> { logger.info("[{}] TimeMessage: {}", atomicLong.incrementAndGet(), Instant.now());
                             return n;
                           }
               ).get();
    }

    static String getTheValS(Integer n) {
        return "String:"+String.valueOf(n);
    }

    static Double getTheValD(Integer n) {
        return new Double(0.01d+n);
    }

}
