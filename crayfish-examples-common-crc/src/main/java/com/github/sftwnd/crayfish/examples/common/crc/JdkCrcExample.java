package com.github.sftwnd.crayfish.examples.common.crc;

import com.github.sftwnd.crayfish.common.crc.CRC;
import com.github.sftwnd.crayfish.common.crc.CrcDescription;
import com.github.sftwnd.crayfish.common.crc.CrcModel;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;

import static com.github.sftwnd.crayfish.examples.common.crc.Examples.check;

/**
 * CRC-32 and CRC-32C calculated by the lookup tables of the library or by the classes {@code java.util.zip.CRC32} and
 * {@code CRC32C} of the JDK, which use the CRC instructions of the CPU: the same CRCs, another speed.
 * <p>
 * The default way is given by {@code -Dcom.github.sftwnd.crayfish.common.crc.jdk=true}. The times are of a short
 * loop without a proper warm-up, an illustration rather than a benchmark.
 */
@Slf4j
public class JdkCrcExample {

    private static final int MIB = 1 << 20;

    public static void main(String[] args) {
        logger.info("The default way by the property {}: JDK = {}", CrcDescription.JDK_PROPERTY, CrcDescription.isJdk());
        byte[] data = new byte[MIB];
        new Random(1).nextBytes(data);

        for (String name : new String[] { "CRC-32", "CRC-32C" }) {
            CrcModel tables = CrcModel.of(name, false);
            CrcModel jdk = tables.withJdk(true);
            check(jdk == CrcModel.of(name, true), "one model per name and way");
            check(jdk.isJdk() && !tables.isJdk(), "the way of the models");
            CRC byTables = tables.crc(data);
            CRC byJdk = jdk.crc(data);
            check(byTables.equals(byJdk), "the CRCs of both ways are equal");
            // the CRCs of both ways can be combined
            CRC half = tables.crc(data, 0, MIB / 2).combine(jdk.crc(data, MIB / 2, MIB / 2));
            check(half.equals(byJdk), "the CRCs of both ways combined");
            logger.info("{}: 0x{} by the lookup tables in {} µs per MiB, by the JDK in {} µs per MiB",
                    name, byTables.hex(), micros(tables, data), micros(jdk, data));
        }

        // the way is a matter of the parameters: other algorithms are calculated by the lookup tables
        CrcModel arc = CrcModel.of("CRC-16/ARC", true);
        logger.info("CRC-16/ARC with the JDK: JDK = {}", arc.isJdk());
        check(!arc.isJdk(), "the JDK has no class of CRC-16/ARC");

        // the default of the models chosen without a choice, changed at run time
        boolean initial = CrcDescription.isJdk();
        CrcDescription.setJdk(true);
        try {
            CrcModel byDefault = CrcModel.of("CRC-32C");
            logger.info("CRC-32C with the default JDK: JDK = {}", byDefault.isJdk());
            check(byDefault.isJdk(), "the default way");
        } finally {
            CrcDescription.setJdk(initial);
        }
    }

    private static long micros(CrcModel model, byte[] data) {
        long result = 0;
        for (int i = 0; i < 300; i++) {
            result += model.value(data);
        }
        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            result += model.value(data);
        }
        long micros = (System.nanoTime() - start) / 100 / 1000;
        return result == 42 ? -1 : micros;          // the result is used, so the loop is not removed
    }

}
