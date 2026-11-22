package com.github.sftwnd.crayfish.examples.common.crc;

import com.github.sftwnd.crayfish.common.crc.CrcModel;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.zip.Deflater;

import static com.github.sftwnd.crayfish.examples.common.crc.Examples.bytes;
import static com.github.sftwnd.crayfish.examples.common.crc.Examples.check;
import static com.github.sftwnd.crayfish.examples.common.crc.Examples.hex;

/**
 * The chunks of a PNG image: the length of the data in 4 bytes, the type in 4 bytes, the data and the CRC-32 of the type
 * and the data, most significant byte first as every number of PNG. CRC-32 is refout, so the byte order of the format
 * is given to {@code codeword} and {@code isValid}.
 * <p>
 * Without arguments the example builds an image of its own; the argument is the path of a PNG file to check instead.
 */
@Slf4j
public class PngChunksExample {

    private static final CrcModel CRC32 = CrcModel.of("CRC-32");
    private static final byte[] SIGNATURE = bytes("89 50 4e 47 0d 0a 1a 0a");

    public static void main(String[] args) throws IOException {
        // the IEND chunk has no data: its type followed by the CRC-32 of the type, high byte first
        byte[] iend = CRC32.codeword("IEND".getBytes(StandardCharsets.US_ASCII), ByteOrder.BIG_ENDIAN);
        logger.info("IEND: {}", hex(iend));
        check(Arrays.equals(iend, bytes("49 45 4e 44 ae 42 60 82")), "the IEND chunk of every PNG");

        if (args.length > 0) {
            check(chunks(Files.readAllBytes(Paths.get(args[0]))), "the chunks of " + args[0]);
            return;
        }
        byte[] png = image();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        logger.info("Image of {} bytes read by ImageIO: {}x{}, the pixel 0x{}", png.length, image.getWidth(),
                image.getHeight(), Integer.toHexString(image.getRGB(0, 0) & 0xFFFFFF));
        check(image.getRGB(0, 0) == 0xFFFF8000, "the image built is a PNG image");
        check(chunks(png), "the chunks of the image");

        // one damaged byte of the data of IDAT, the chunk before the IEND chunk and the CRC of IDAT
        byte[] damaged = png.clone();
        damaged[png.length - (4 + iend.length) - 4 - 1] ^= 0x10;
        logger.info("Damaged image:");
        check(!chunks(damaged), "the damaged chunk is found");
    }

    /** Checks every chunk; true if all of them are valid. */
    private static boolean chunks(byte[] png) {
        check(Arrays.equals(Arrays.copyOf(png, SIGNATURE.length), SIGNATURE), "the signature of PNG");
        boolean valid = true;
        ByteBuffer buffer = ByteBuffer.wrap(png);      // big endian: the length of the chunk
        for (int offset = SIGNATURE.length; offset < png.length; ) {
            int length = buffer.getInt(offset);
            String type = new String(png, offset + 4, 4, StandardCharsets.US_ASCII);
            // the type, the data and the CRC: the CRC most significant byte first
            boolean byFormat = CRC32.isValid(png, offset + 4, length + 8, ByteOrder.BIG_ENDIAN);
            // the byte order of the model, least significant byte first for CRC-32, does not match the format
            boolean byModel = CRC32.isValid(png, offset + 4, length + 8);
            logger.info("  chunk {} of {} bytes: CRC {}, valid = {}, valid in the order of the model = {}",
                    type, length, hex(png, offset + 8 + length, 4), byFormat, byModel);
            valid &= byFormat;
            offset += length + 12;
        }
        return valid;
    }

    /** A PNG image of 1x1 pixel, orange. */
    private static byte[] image() throws IOException {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        png.write(SIGNATURE);
        // IHDR: width 1, height 1, 8 bits per sample, RGB, deflate, filter 0, no interlace
        chunk(png, "IHDR", bytes("00 00 00 01 00 00 00 01 08 02 00 00 00"));
        // IDAT: the zlib stream of one row: the filter 0 and the red, green and blue samples
        chunk(png, "IDAT", deflate(bytes("00 ff 80 00")));
        chunk(png, "IEND", new byte[0]);
        return png.toByteArray();
    }

    private static void chunk(ByteArrayOutputStream png, String type, byte[] data) throws IOException {
        byte[] typeAndData = new byte[4 + data.length];
        System.arraycopy(type.getBytes(StandardCharsets.US_ASCII), 0, typeAndData, 0, 4);
        System.arraycopy(data, 0, typeAndData, 4, data.length);
        png.write(ByteBuffer.allocate(4).putInt(data.length).array());
        png.write(CRC32.codeword(typeAndData, ByteOrder.BIG_ENDIAN));
    }

    private static byte[] deflate(byte[] data) {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();
        byte[] buffer = new byte[64];
        int length = deflater.deflate(buffer);
        deflater.end();
        return Arrays.copyOf(buffer, length);
    }

}
