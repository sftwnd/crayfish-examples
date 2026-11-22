package com.github.sftwnd.crayfish.examples.common.crc;

import com.github.sftwnd.crayfish.common.crc.CrcModel;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.github.sftwnd.crayfish.examples.common.crc.Examples.check;
import static com.github.sftwnd.crayfish.examples.common.crc.Examples.hex;

/**
 * The CRCs of bits that are not a whole number of bytes: a CAN frame, a USB token and a FlexRay header. The receiver
 * checks the bits followed by their CRC with one call.
 */
@Slf4j
public class BitsExample {

    public static void main(String[] args) {
        can();
        usb();
        flexRay();
        // whole bytes as bits: the same CRC as of the bytes
        byte[] data = "123456789".getBytes(StandardCharsets.US_ASCII);
        CrcModel crc15 = CrcModel.of("CRC-15/CAN");
        check(crc15.valueOfBits(data, 8L * data.length) == crc15.value(data), "the bits of whole bytes");
    }

    /** CRC-15/CAN of a CAN base frame: the bits from the start of the frame to the end of the data. */
    private static void can() {
        CrcModel model = CrcModel.of("CRC-15/CAN");
        byte[] payload = { 0x11, 0x22, 0x33 };
        Bits frame = new Bits()
                .append(0, 1)                       // start of frame
                .append(0x123, 11)                  // identifier
                .append(0, 1)                       // RTR: a data frame
                .append(0, 1)                       // IDE: an 11-bit identifier
                .append(0, 1)                       // r0
                .append(payload.length, 4);         // DLC
        for (byte b : payload) {
            frame.append(b, 8);
        }
        int count = frame.count();                  // 19 + 8 * DLC
        long crc = model.valueOfBits(frame.bytes(), count);
        logger.info("CAN frame of {} bits {}: CRC-15 0x{}", count, hex(frame.bytes()), Long.toHexString(crc));

        // the receiver: the bits followed by the 15 bits of their CRC, residue ^ xorout = 0 for CRC-15/CAN
        frame.append(crc, 15);
        long received = model.valueOfBits(frame.bytes(), frame.count());
        logger.info("CAN frame with the CRC: {}", received);
        check(received == (model.residue() ^ model.parameters().xorout()), "the CAN frame with its CRC");
    }

    /** CRC-5/USB of a token: 7 bits of the address and 4 bits of the endpoint, sent least significant bit first. */
    private static void usb() {
        CrcModel model = CrcModel.of("CRC-5/USB");
        // the SETUP token to the address 0, the endpoint 0 is sent as 2d 00 10: the CRC is 00010
        long setup = model.valueOfBits(0, 11);
        logger.info("USB token to address 0, endpoint 0: CRC-5 0x{}", Long.toHexString(setup));
        check(setup == 0x02, "the CRC of the SETUP token 2d 00 10");

        int address = 0x3A;
        int endpoint = 0x5;
        long token = address | endpoint << 7;
        long crc = model.valueOfBits(token, 11);
        logger.info("USB token to address 0x{}, endpoint {}: CRC-5 0x{}",
                Integer.toHexString(address), endpoint, Long.toHexString(crc));
        // the receiver: the 16 bits of the token with the CRC in the bits 11 to 15
        long received = model.valueOfBits(token | crc << 11, 16);
        logger.info("USB token with the CRC: 0x{}, residue ^ xorout = 0x{}", Long.toHexString(received),
                Long.toHexString(model.residue() ^ model.parameters().xorout()));
        check(received == (model.residue() ^ model.parameters().xorout()), "the USB token with its CRC");
    }

    /** CRC-11/FLEXRAY of a header: the sync and startup indicators, the frame ID and the payload length. */
    private static void flexRay() {
        CrcModel model = CrcModel.of("CRC-11/FLEXRAY");
        long header = new Bits()
                .append(0, 1)                       // sync frame indicator
                .append(0, 1)                       // startup frame indicator
                .append(0x05A, 11)                  // frame ID
                .append(8, 7)                       // payload length in words
                .value();
        long crc = model.valueOfBits(header, 20);
        logger.info("FlexRay header 0x{} of 20 bits: CRC-11 0x{}", Long.toHexString(header), Long.toHexString(crc));
        long received = model.valueOfBits(header << 11 | crc, 31);
        check(received == (model.residue() ^ model.parameters().xorout()), "the FlexRay header with its CRC");
    }

    /** Bits written most significant bit first. */
    private static final class Bits {

        private byte[] bytes = new byte[16];
        private int count;

        Bits append(long value, int bits) {
            for (int i = bits - 1; i >= 0; i--) {
                if (count == 8 * bytes.length) {
                    bytes = Arrays.copyOf(bytes, 2 * bytes.length);
                }
                if ((value >>> i & 1) != 0) {
                    bytes[count >> 3] |= (byte) (0x80 >>> (count & 7));
                }
                count++;
            }
            return this;
        }

        int count() {
            return count;
        }

        byte[] bytes() {
            return Arrays.copyOf(bytes, (count + 7) / 8);
        }

        /** The bits as a number, the first bit the most significant one. */
        long value() {
            long value = 0;
            for (int i = 0; i < count; i++) {
                value = value << 1 | (bytes[i >> 3] >>> (7 - (i & 7)) & 1);
            }
            return value;
        }

    }

}
