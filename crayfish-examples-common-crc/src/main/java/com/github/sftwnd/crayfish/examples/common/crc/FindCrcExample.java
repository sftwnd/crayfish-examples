package com.github.sftwnd.crayfish.examples.common.crc;

import com.github.sftwnd.crayfish.common.crc.CrcModel;
import com.github.sftwnd.crayfish.common.crc.StandardCrc;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumSet;
import java.util.Set;

import static com.github.sftwnd.crayfish.examples.common.crc.Examples.bytes;
import static com.github.sftwnd.crayfish.examples.common.crc.Examples.check;
import static com.github.sftwnd.crayfish.examples.common.crc.Examples.hex;

/**
 * The CRC algorithm of a device found from samples of its data. The samples are the ROM codes of 1-Wire temperature
 * sensors: the family code 0x28 and 6 bytes of the serial number, followed by 1 byte of CRC.
 */
@Slf4j
public class FindCrcExample {

    private static final String[] SAMPLES = {
            "28 49 8b bc ae 5a 00 57",
            "28 27 1b c3 d5 d3 00 b1",
            "28 ed 76 b6 1b 7b 00 0a"
    };

    public static void main(String[] args) {
        Set<StandardCrc> found = EnumSet.allOf(StandardCrc.class);
        for (String sample : SAMPLES) {
            byte[] rom = bytes(sample);
            int length = rom.length - 1;
            long crc = rom[length] & 0xFFL;
            Set<StandardCrc> matching = StandardCrc.matching(rom, 0, length, crc);
            found.retainAll(matching);
            logger.info("Sample {} with CRC 0x{}: {} algorithm(s) {}, together: {}",
                    hex(rom, 0, length), hex(rom, length, 1), matching.size(), matching, found);
        }
        // a sample of an 8-bit CRC is matched by other algorithms by chance, more samples leave one algorithm
        check(found.size() == 1, "one algorithm matches all the samples");

        CrcModel model = found.iterator().next().model();
        logger.info("The algorithm of the device: {}, aliases {}", model.name(), model.description().aliases());
        check(model == CrcModel.of("CRC-8/MAXIM-DOW"), "the CRC of 1-Wire");

        // now the CRC of the next ROM code read from the bus can be checked
        byte[] next = bytes("28 ff 64 1e 0f 16 03 90");
        logger.info("ROM code {}: valid = {}", hex(next), model.isValid(next));
        check(model.isValid(next), "the next ROM code is valid");
    }

}
